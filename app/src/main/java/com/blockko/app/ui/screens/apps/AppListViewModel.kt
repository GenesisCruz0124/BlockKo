package com.blockko.app.ui.screens.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockko.app.BlockKoApplication
import com.blockko.app.data.apps.InstalledApp
import com.blockko.app.vpn.VpnController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AppListUiState(
    val apps: List<InstalledApp> = emptyList(),
    val excludedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<BlockKoApplication>()

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val excluded = app.settingsRepository.settings.first().excludedPackages
            val installedApps = app.installedAppsRepository.listInstalledApps()
            _uiState.value = AppListUiState(
                apps = installedApps,
                excludedPackages = excluded,
                isLoading = false
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleExcluded(packageName: String) {
        val current = _uiState.value.excludedPackages
        val updated = if (packageName in current) current - packageName else current + packageName
        _uiState.value = _uiState.value.copy(excludedPackages = updated)
        viewModelScope.launch {
            app.settingsRepository.setExcludedPackages(updated)
            VpnController.restartIfRunning(app)
        }
    }

    fun filteredApps(): List<InstalledApp> {
        val query = _uiState.value.searchQuery.trim()
        val apps = _uiState.value.apps
        return if (query.isBlank()) apps else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }
}
