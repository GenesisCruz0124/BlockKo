package com.blockko.app.ui.screens.blocklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockko.app.data.blocklist.BlocklistLoadState
import com.blockko.app.data.db.CustomRuleType
import com.blockko.app.ui.strings.LocalStrings
import java.text.DateFormat
import java.util.Date

@Composable
fun BlocklistScreen(viewModel: BlocklistViewModel = viewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsState()
    var urlField by remember { mutableStateOf(state.sourceUrl) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = strings.blocklistTitle, style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = strings.blocklistDomainCount(state.domainCount), style = MaterialTheme.typography.bodyLarge)
                    val lastUpdatedText = if (state.lastUpdatedMillis > 0) {
                        DateFormat.getDateTimeInstance().format(Date(state.lastUpdatedMillis))
                    } else "—"
                    Text(
                        text = strings.blocklistLastUpdated(lastUpdatedText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (val loadState = state.loadState) {
                        is BlocklistLoadState.Loading -> {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(strings.blocklistUpdating, style = MaterialTheme.typography.bodySmall)
                                LinearProgressIndicator(
                                    progress = { loadState.progress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }
                        else -> Unit
                    }

                    state.updateResult?.let { result ->
                        val text = when (result) {
                            is UpdateResult.Success -> strings.blocklistUpdateSuccess(result.count)
                            is UpdateResult.Failure -> strings.blocklistUpdateFailed
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result is UpdateResult.Failure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = urlField,
                        onValueChange = { urlField = it },
                        label = { Text("Blocklist URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                    Button(
                        onClick = { viewModel.updateBlocklist(urlField) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = state.loadState !is BlocklistLoadState.Loading
                    ) {
                        if (state.loadState is BlocklistLoadState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(strings.blocklistUpdateAction)
                    }
                }
            }
        }

        item {
            Text(text = strings.customRulesTitle, style = MaterialTheme.typography.titleMedium)
        }

        item { AddRuleRow(strings = strings, onAdd = viewModel::addRule) }

        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = { Text(strings.searchHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(state.customRules, key = { it.domain }) { rule ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = rule.domain, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (rule.type == CustomRuleType.BLOCK) "Blocked" else "Allowed",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (rule.type == CustomRuleType.BLOCK) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(onClick = { viewModel.removeRule(rule.domain) }) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun AddRuleRow(
    strings: com.blockko.app.ui.strings.BlockKoStrings,
    onAdd: (String, CustomRuleType) -> Unit
) {
    var domain by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = { Text(strings.customRuleHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onAdd(domain, CustomRuleType.BLOCK); domain = "" },
                modifier = Modifier.weight(1f)
            ) { Text(strings.addBlockRule) }
            OutlinedButton(
                onClick = { onAdd(domain, CustomRuleType.ALLOW); domain = "" },
                modifier = Modifier.weight(1f)
            ) { Text(strings.addAllowRule) }
        }
    }
}
