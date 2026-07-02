package com.blockko.app.ui.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockko.app.BuildConfig
import com.blockko.app.ui.strings.LocalStrings

@Composable
fun AboutScreen() {
    val strings = LocalStrings.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = strings.aboutTitle, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "BlockKo v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text(
                text = strings.limitationsTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        val limitations = listOf(
            strings.limitationInAppAds,
            strings.limitationSingleVpn,
            strings.limitationPrivateDns,
            strings.limitationIpv6
        )
        items(limitations) { limitation ->
            Card {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(text = limitation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Text(
                text = "No analytics, no crash reporters, no third-party tracking SDKs are bundled in BlockKo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
