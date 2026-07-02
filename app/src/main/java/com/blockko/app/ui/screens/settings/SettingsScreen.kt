package com.blockko.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockko.app.data.datastore.AppLanguage
import com.blockko.app.data.datastore.UpstreamDnsProvider
import com.blockko.app.ui.strings.LocalStrings

@Composable
fun SettingsScreen(
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val strings = LocalStrings.current
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = strings.settingsTitle, style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = strings.settingsUpstreamDns, style = MaterialTheme.typography.titleMedium)
                    UpstreamDnsProvider.entries.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.upstreamDnsProvider == provider,
                                onClick = { viewModel.setUpstreamProvider(provider) }
                            )
                            Text(
                                text = if (provider == UpstreamDnsProvider.CUSTOM) provider.label
                                else "${provider.label} (${provider.ip})"
                            )
                        }
                    }
                    if (settings.upstreamDnsProvider == UpstreamDnsProvider.CUSTOM) {
                        var customIp by remember(settings.customUpstreamDns) { mutableStateOf(settings.customUpstreamDns) }
                        OutlinedTextField(
                            value = customIp,
                            onValueChange = { customIp = it },
                            label = { Text("Custom DNS IP") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        Button(
                            onClick = { viewModel.setCustomUpstreamDns(customIp) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Save") }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = strings.settingsLanguage, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.language == AppLanguage.EN,
                            onClick = { viewModel.setLanguage(AppLanguage.EN) }
                        )
                        Text("English")
                        RadioButton(
                            selected = settings.language == AppLanguage.TL,
                            onClick = { viewModel.setLanguage(AppLanguage.TL) },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Text("Taglish")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = strings.settingsAutoStart, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = strings.settingsAutoStartHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoStartOnBoot,
                        onCheckedChange = { viewModel.setAutoStartOnBoot(it) }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = strings.settingsBatteryOptimization, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = strings.settingsBatteryOptimizationHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Open battery settings") }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = strings.settingsAbout,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = onOpenAbout, modifier = Modifier.padding(top = 8.dp)) {
                        Text(strings.settingsAbout)
                    }
                }
            }
        }
    }
}
