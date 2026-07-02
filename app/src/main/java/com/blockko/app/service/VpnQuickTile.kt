package com.blockko.app.service

import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.blockko.app.BlockKoApplication
import com.blockko.app.vpn.VpnController
import com.blockko.app.vpn.VpnRunState
import com.blockko.app.vpn.VpnStatusBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VpnQuickTile : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        applyState(VpnStatusBus.state.value.runState)
        job = scope.launch {
            VpnStatusBus.state.collect { state -> applyState(state.runState) }
        }
    }

    private fun applyState(runState: VpnRunState) {
        val tile = qsTile ?: return
        tile.state = if (runState == VpnRunState.STOPPED) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.subtitle = when (runState) {
            VpnRunState.RUNNING -> "Protected"
            VpnRunState.PAUSED -> "Paused"
            VpnRunState.STOPPED -> "Off"
        }
        tile.updateTile()
    }

    override fun onStopListening() {
        job?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val app = application as BlockKoApplication
        when (VpnStatusBus.state.value.runState) {
            VpnRunState.STOPPED -> {
                if (VpnService.prepare(this) == null) {
                    VpnController.start(this)
                    scope.launch { app.settingsRepository.setProtectionEnabled(true) }
                }
                // If prepare() returns a non-null intent, consent must be granted from
                // inside the app; a QS tile cannot launch that activity for result.
            }
            VpnRunState.RUNNING, VpnRunState.PAUSED -> {
                VpnController.stop(this)
            }
        }
    }
}
