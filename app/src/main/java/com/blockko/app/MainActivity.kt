package com.blockko.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.blockko.app.ui.nav.BlockKoNavHost
import com.blockko.app.ui.strings.LocalStrings
import com.blockko.app.ui.strings.stringsFor
import com.blockko.app.ui.theme.BlockKoTheme
import com.blockko.app.vpn.VpnController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app get() = application as BlockKoApplication

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            onVpnConsentGranted()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: notification is just for the persistent status, VPN still works without it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            val settings by app.settingsRepository.settings.collectAsState(
                initial = com.blockko.app.data.datastore.BlockKoSettings()
            )
            val strings = stringsFor(settings.language)

            BlockKoTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalStrings provides strings) {
                    BlockKoNavHost(onRequestEnableVpn = ::requestEnableVpn)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestEnableVpn() {
        val consentIntent = VpnService.prepare(this)
        if (consentIntent != null) {
            vpnPermissionLauncher.launch(consentIntent)
        } else {
            onVpnConsentGranted()
        }
    }

    private fun onVpnConsentGranted() {
        VpnController.start(this)
        lifecycleScope.launch {
            app.settingsRepository.setProtectionEnabled(true)
            app.settingsRepository.setOnboardingCompleted(true)
        }
    }
}
