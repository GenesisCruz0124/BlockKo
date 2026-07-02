package com.blockko.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockko.app.BlockKoApplication
import com.blockko.app.data.datastore.BlockKoSettings
import com.blockko.app.data.db.CustomRuleType
import com.blockko.app.data.db.DayCount
import com.blockko.app.data.db.DomainCount
import com.blockko.app.vpn.VpnController
import com.blockko.app.vpn.VpnRunState
import com.blockko.app.vpn.VpnStatusBus
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val runState: VpnRunState = VpnRunState.STOPPED,
    val settings: BlockKoSettings = BlockKoSettings(),
    val blockedToday: Int = 0,
    val blockedAllTime: Int = 0,
    val queriesToday: Int = 0,
    val topBlocked: List<DomainCount> = emptyList(),
    val dailyCounts: List<DayCount> = emptyList()
)

private data class StatsBundle(
    val blockedToday: Int,
    val blockedAllTime: Int,
    val topBlocked: List<DomainCount>,
    val dailyCounts: List<DayCount>
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<BlockKoApplication>()

    private fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun sevenDaysAgoMillis(): Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

    private val statsFlow = combine(
        app.statsRepository.blockedSince(startOfTodayMillis()),
        app.statsRepository.blockedAllTime(),
        app.statsRepository.topBlockedDomains(sevenDaysAgoMillis()),
        app.statsRepository.dailyCounts(sevenDaysAgoMillis())
    ) { blockedToday, blockedAllTime, topBlocked, dailyCounts ->
        StatsBundle(blockedToday, blockedAllTime, topBlocked, dailyCounts)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        VpnStatusBus.state,
        app.settingsRepository.settings,
        statsFlow
    ) { vpnState, settings, stats ->
        HomeUiState(
            runState = vpnState.runState,
            settings = settings,
            blockedToday = stats.blockedToday,
            blockedAllTime = stats.blockedAllTime,
            queriesToday = vpnState.queriesToday,
            topBlocked = stats.topBlocked,
            dailyCounts = stats.dailyCounts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun allowDomain(domain: String) {
        viewModelScope.launch {
            app.blocklistRepository.addCustomRule(domain, CustomRuleType.ALLOW)
        }
    }

    fun pauseProtection() = VpnController.pause(app)

    fun resumeProtection() = VpnController.resume(app)

    fun stopProtection() {
        VpnController.stop(app)
        viewModelScope.launch { app.settingsRepository.setProtectionEnabled(false) }
    }
}
