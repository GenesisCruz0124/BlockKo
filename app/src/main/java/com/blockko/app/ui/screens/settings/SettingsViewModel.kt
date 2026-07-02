package com.blockko.app.ui.screens.settings

import android.app.Application
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockko.app.BlockKoApplication
import com.blockko.app.data.datastore.AppLanguage
import com.blockko.app.data.datastore.BlockKoSettings
import com.blockko.app.data.datastore.UpstreamDnsProvider
import com.blockko.app.vpn.VpnController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<BlockKoApplication>()

    val settings: StateFlow<BlockKoSettings> = app.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlockKoSettings())

    fun setUpstreamProvider(provider: UpstreamDnsProvider) {
        viewModelScope.launch {
            app.settingsRepository.setUpstreamProvider(provider)
            VpnController.restartIfRunning(app)
        }
    }

    fun setCustomUpstreamDns(ip: String) {
        viewModelScope.launch {
            app.settingsRepository.setCustomUpstreamDns(ip)
            VpnController.restartIfRunning(app)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { app.settingsRepository.setLanguage(language) }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { app.settingsRepository.setAutoStartOnBoot(enabled) }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = app.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(app.packageName) ?: true
    }
}
