package com.blockko.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockko.app.ui.components.BlockedPerDayChart
import com.blockko.app.ui.components.StatCard
import com.blockko.app.ui.strings.LocalStrings
import com.blockko.app.vpn.VpnRunState

@Composable
fun HomeScreen(
    onRequestEnableVpn: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProtectionToggleCard(
                runState = state.runState,
                onToggle = {
                    when (state.runState) {
                        VpnRunState.STOPPED -> onRequestEnableVpn()
                        else -> viewModel.stopProtection()
                    }
                },
                onPauseResume = {
                    if (state.runState == VpnRunState.PAUSED) viewModel.resumeProtection()
                    else viewModel.pauseProtection()
                }
            )
        }

        if (state.runState != VpnRunState.STOPPED && state.blockedToday > 0) {
            item {
                Text(
                    text = strings.protectedBanner(state.blockedToday),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = strings.blockedTodayLabel,
                    value = state.blockedToday.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = strings.blockedAllTimeLabel,
                    value = state.blockedAllTime.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            StatCard(label = strings.queriesTodayLabel, value = state.queriesToday.toString())
        }

        item {
            Text(text = strings.chartTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            BlockedPerDayChart(dailyCounts = state.dailyCounts)
        }

        item {
            Text(text = strings.topBlockedTitle, style = MaterialTheme.typography.titleMedium)
        }
        if (state.topBlocked.isEmpty()) {
            item {
                Text(
                    text = strings.topBlockedEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.topBlocked) { domainCount ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.allowDomain(domainCount.domain) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = domainCount.domain, style = MaterialTheme.typography.bodyMedium)
                        Text(text = domainCount.count.toString(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Text(
                    text = strings.tapToAllowlist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProtectionToggleCard(
    runState: VpnRunState,
    onToggle: () -> Unit,
    onPauseResume: () -> Unit
) {
    val strings = LocalStrings.current
    val (statusText, statusColor) = when (runState) {
        VpnRunState.RUNNING -> strings.statusProtected to MaterialTheme.colorScheme.tertiary
        VpnRunState.PAUSED -> strings.statusPaused to MaterialTheme.colorScheme.error
        VpnRunState.STOPPED -> strings.statusOff to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = statusText,
                    tint = statusColor,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = statusText, style = MaterialTheme.typography.headlineMedium, color = statusColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.toggleHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (runState != VpnRunState.STOPPED) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onPauseResume) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (runState == VpnRunState.PAUSED) strings.resumeAction else strings.pauseAction)
                }
            }
        }
    }
}
