package com.blockko.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class VpnRunState { STOPPED, RUNNING, PAUSED }

data class VpnUiState(
    val runState: VpnRunState = VpnRunState.STOPPED,
    val blockedToday: Int = 0,
    val queriesToday: Int = 0,
    val pausedUntilMillis: Long = 0L
)

/**
 * Lightweight pub/sub so Compose screens can observe live VPN status without
 * binding the service. The service is the sole writer; the UI is read-only.
 */
object VpnStatusBus {
    private val _state = MutableStateFlow(VpnUiState())
    val state: StateFlow<VpnUiState> = _state

    fun update(transform: (VpnUiState) -> VpnUiState) {
        _state.update(transform)
    }
}
