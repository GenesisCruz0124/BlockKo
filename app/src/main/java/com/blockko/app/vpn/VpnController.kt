package com.blockko.app.vpn

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** Thin wrapper around starting/stopping [BlockKoVpnService] so call sites stay declarative. */
object VpnController {

    fun start(context: Context) {
        val intent = Intent(context, BlockKoVpnService::class.java).setAction(BlockKoVpnService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(Intent(context, BlockKoVpnService::class.java).setAction(BlockKoVpnService.ACTION_STOP))
    }

    fun pause(context: Context) {
        context.startService(Intent(context, BlockKoVpnService::class.java).setAction(BlockKoVpnService.ACTION_PAUSE))
    }

    fun resume(context: Context) {
        context.startService(Intent(context, BlockKoVpnService::class.java).setAction(BlockKoVpnService.ACTION_RESUME))
    }

    /**
     * Per-app exclusions and other tunnel-level config are only read at
     * [android.net.VpnService.Builder.establish] time, so applying a change
     * means tearing the tunnel down and bringing it back up.
     */
    fun restartIfRunning(context: Context) {
        if (VpnStatusBus.state.value.runState == VpnRunState.STOPPED) return
        stop(context)
        Handler(Looper.getMainLooper()).postDelayed({ start(context) }, 400)
    }
}
