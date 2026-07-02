package com.blockko.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.blockko.app.BlockKoApplication
import com.blockko.app.vpn.BlockKoVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as BlockKoApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.settingsRepository.settings.first()
                // VpnService.prepare() returns null only if consent was already granted
                // in a previous session; a boot broadcast cannot show the consent UI.
                if (settings.autoStartOnBoot && settings.protectionEnabled &&
                    VpnService.prepare(context) == null
                ) {
                    val serviceIntent = Intent(context, BlockKoVpnService::class.java)
                        .setAction(BlockKoVpnService.ACTION_START)
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
