package com.blockko.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockko.app.ui.strings.LocalStrings

@Composable
fun OnboardingScreen(onEnableProtection: () -> Unit) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.height(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = strings.onboardingTitle,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.onboardingBody,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        InfoRow(icon = Icons.Default.Lock, text = strings.onboardingLocalPoint)
        Spacer(modifier = Modifier.height(12.dp))
        InfoRow(icon = Icons.Default.Warning, text = strings.onboardingLimitsPoint)

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEnableProtection, modifier = Modifier.fillMaxWidth()) {
            Text(strings.onboardingCta)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
