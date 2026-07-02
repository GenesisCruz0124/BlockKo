package com.blockko.app.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.ServiceCompat
import com.blockko.app.BlockKoApplication
import com.blockko.app.data.datastore.BlockKoSettings
import com.blockko.app.notification.NotificationHelper
import com.blockko.app.notification.VPN_NOTIFICATION_ID
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "BlockKoVpnService"
private const val VPN_ADDRESS = "10.111.222.1"
private const val VPN_MTU = 1500
private const val DNS_PORT = 53
private const val UPSTREAM_TIMEOUT_MS = 6000

/** Public resolver IPs some apps hardcode instead of using the system resolver. */
private val WELL_KNOWN_DNS_IPS = listOf(
    "1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4", "9.9.9.9", "149.112.112.112"
)

class BlockKoVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.blockko.app.action.START_VPN"
        const val ACTION_STOP = "com.blockko.app.action.STOP_VPN"
        const val ACTION_PAUSE = "com.blockko.app.action.PAUSE_VPN"
        const val ACTION_RESUME = "com.blockko.app.action.RESUME_VPN"
        private const val PAUSE_DURATION_MS = 5 * 60 * 1000L
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private var tunInterface: ParcelFileDescriptor? = null
    private var readThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private val pausedUntilMillis = AtomicLong(0L)

    private val blockedToday = AtomicInteger(0)
    private val queriesToday = AtomicInteger(0)
    private var dayEpochOfCounters = 0L

    @Volatile private var cachedSettings: BlockKoSettings = BlockKoSettings()

    private lateinit var app: BlockKoApplication

    override fun onCreate() {
        super.onCreate()
        app = application as BlockKoApplication
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                pauseProtection()
                return START_STICKY
            }
            ACTION_RESUME -> {
                resumeProtection()
                return START_STICKY
            }
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning.get()) return

        // Must be called within seconds of startForegroundService(); the heavier
        // cache/DB work below happens afterwards on a background coroutine.
        val notification = NotificationHelper.buildStatusNotification(
            this, "Protected", isPaused = false
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this, VPN_NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, VPN_NOTIFICATION_ID, notification, 0)
        } else {
            startForeground(VPN_NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            app.blocklistRepository.refreshCache()

            val settings = app.settingsRepository.settings.first()
            cachedSettings = settings
            pausedUntilMillis.set(settings.pausedUntilMillis)
            seedTodayCounters(settings)
            launch { app.settingsRepository.settings.collect { cachedSettings = it } }

            val builder = Builder()
                .setSession("BlockKo")
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(VPN_ADDRESS)
                .addRoute(VPN_ADDRESS, 32)
                .setMtu(VPN_MTU)
                .setBlocking(true)

            WELL_KNOWN_DNS_IPS.forEach { ip -> builder.addRoute(ip, 32) }
            if (settings.customUpstreamDns.isNotBlank()) {
                runCatching { builder.addRoute(settings.customUpstreamDns, 32) }
            }
            settings.excludedPackages.forEach { pkg ->
                runCatching { builder.addDisallowedApplication(pkg) }
                    .onFailure { Log.w(TAG, "Could not exclude $pkg", it) }
            }

            tunInterface = try {
                builder.establish()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to establish VPN interface", e)
                stopSelf()
                return@launch
            }

            if (tunInterface == null) {
                Log.e(TAG, "VpnService.Builder.establish() returned null (permission revoked?)")
                stopSelf()
                return@launch
            }

            isRunning.set(true)
            VpnStatusBus.update {
                it.copy(
                    runState = if (pausedUntilMillis.get() > System.currentTimeMillis()) VpnRunState.PAUSED else VpnRunState.RUNNING,
                    blockedToday = blockedToday.get(),
                    queriesToday = queriesToday.get(),
                    pausedUntilMillis = pausedUntilMillis.get()
                )
            }

            startPacketLoop()
            schedulePauseExpiryWatcher()
        }
    }

    private fun seedTodayCounters(settings: BlockKoSettings) {
        val today = startOfTodayEpochDay()
        dayEpochOfCounters = today
        queriesToday.set(if (settings.totalQueriesDayEpoch == today) settings.totalQueriesToday else 0)
        blockedToday.set(0) // refined below once StatsRepository reports today's real count
        serviceScope.launch {
            val since = today * 86_400_000L
            app.statsRepository.blockedSince(since).collect { count ->
                blockedToday.set(count)
                pushStatus()
            }
        }
    }

    private fun startOfTodayEpochDay(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / 86_400_000L
    }

    private fun startPacketLoop() {
        val fd = tunInterface ?: return
        readThread = Thread({
            val input = FileInputStream(fd.fileDescriptor)
            val output = FileOutputStream(fd.fileDescriptor)
            val buffer = ByteArray(32767)
            try {
                while (isRunning.get()) {
                    val length = input.read(buffer)
                    if (length <= 0) continue
                    val packetCopy = buffer.copyOf(length)
                    handlePacket(packetCopy, length, output)
                }
            } catch (e: Exception) {
                if (isRunning.get()) Log.e(TAG, "Packet loop terminated unexpectedly", e)
            }
        }, "BlockKoTunReader").apply { start() }
    }

    private fun handlePacket(packet: ByteArray, length: Int, output: FileOutputStream) {
        val udp = PacketUtils.parseUdp4(packet, length) ?: return
        if (udp.destPort != DNS_PORT) return

        val dnsPayload = packet.copyOfRange(udp.payloadOffset, udp.payloadOffset + udp.payloadLength)
        val query = DnsMessage.parseQuery(dnsPayload, dnsPayload.size) ?: return

        queriesToday.incrementAndGet()

        val paused = System.currentTimeMillis() < pausedUntilMillis.get()
        val blocked = !paused && app.blocklistRepository.isBlocked(query.name)

        if (blocked) {
            val response = DnsMessage.buildNxDomainResponse(dnsPayload, query)
            val replyPacket = PacketUtils.buildUdp4Packet(
                sourceIp = udp.destIp, sourcePort = DNS_PORT,
                destIp = udp.sourceIp, destPort = udp.sourcePort,
                payload = response
            )
            writePacket(output, replyPacket)
            serviceScope.launch {
                app.statsRepository.recordBlock(query.name)
                blockedToday.incrementAndGet()
                pushStatus()
            }
        } else {
            serviceScope.launch { forwardToUpstream(dnsPayload, udp, output) }
        }

        if (queriesToday.get() % 10 == 0) {
            serviceScope.launch {
                app.settingsRepository.setTotalQueriesToday(queriesToday.get(), dayEpochOfCounters)
            }
        }
    }

    private suspend fun forwardToUpstream(
        dnsPayload: ByteArray,
        udp: PacketUtils.Udp4Packet,
        output: FileOutputStream
    ) {
        val upstreamIp = currentUpstreamIp()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = UPSTREAM_TIMEOUT_MS

            val requestPacket = DatagramPacket(
                dnsPayload, dnsPayload.size, InetSocketAddress(InetAddress.getByName(upstreamIp), DNS_PORT)
            )
            socket.send(requestPacket)

            val responseBuffer = ByteArray(4096)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)

            val replyPacket = PacketUtils.buildUdp4Packet(
                sourceIp = udp.destIp, sourcePort = DNS_PORT,
                destIp = udp.sourceIp, destPort = udp.sourcePort,
                payload = responseBuffer.copyOf(responsePacket.length)
            )
            writePacket(output, replyPacket)
        } catch (e: Exception) {
            Log.w(TAG, "Upstream DNS forward failed for $upstreamIp", e)
        } finally {
            socket?.close()
        }
    }

    private fun currentUpstreamIp(): String = cachedSettings.effectiveUpstreamIp.ifBlank { "1.1.1.1" }

    private fun writePacket(output: FileOutputStream, packet: ByteArray) {
        serviceScope.launch {
            writeMutex.withLock {
                try {
                    output.write(packet)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed writing packet back to tun", e)
                }
            }
        }
    }

    private fun pauseProtection() {
        val until = System.currentTimeMillis() + PAUSE_DURATION_MS
        pausedUntilMillis.set(until)
        serviceScope.launch { app.settingsRepository.setPausedUntil(until) }
        updateNotification(isPaused = true)
        pushStatus()
        schedulePauseExpiryWatcher()
    }

    private fun resumeProtection() {
        pausedUntilMillis.set(0L)
        serviceScope.launch { app.settingsRepository.setPausedUntil(0L) }
        updateNotification(isPaused = false)
        pushStatus()
    }

    private fun schedulePauseExpiryWatcher() {
        val until = pausedUntilMillis.get()
        if (until <= System.currentTimeMillis()) return
        serviceScope.launch {
            delay(until - System.currentTimeMillis())
            if (pausedUntilMillis.get() == until) {
                pausedUntilMillis.set(0L)
                app.settingsRepository.setPausedUntil(0L)
                updateNotification(isPaused = false)
                pushStatus()
            }
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val text = if (isPaused) "Paused" else "Protected • ${blockedToday.get()} blocked today"
        val notification = NotificationHelper.buildStatusNotification(this, text, isPaused)
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(VPN_NOTIFICATION_ID, notification)
    }

    private fun pushStatus() {
        val paused = System.currentTimeMillis() < pausedUntilMillis.get()
        VpnStatusBus.update {
            it.copy(
                runState = if (!isRunning.get()) VpnRunState.STOPPED else if (paused) VpnRunState.PAUSED else VpnRunState.RUNNING,
                blockedToday = blockedToday.get(),
                queriesToday = queriesToday.get(),
                pausedUntilMillis = pausedUntilMillis.get()
            )
        }
        updateNotification(isPaused = paused)
    }

    private fun stopVpn() {
        isRunning.set(false)
        readThread?.interrupt()
        readThread = null
        runCatching { tunInterface?.close() }
        tunInterface = null

        runBlocking(Dispatchers.IO) {
            app.settingsRepository.setTotalQueriesToday(queriesToday.get(), dayEpochOfCounters)
            app.settingsRepository.setProtectionEnabled(false)
        }

        VpnStatusBus.update { it.copy(runState = VpnRunState.STOPPED, pausedUntilMillis = 0L) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        Log.i(TAG, "VPN permission revoked by system/user")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        isRunning.set(false)
        readThread?.interrupt()
        runCatching { tunInterface?.close() }
        serviceScope.cancel()
        super.onDestroy()
    }
}
