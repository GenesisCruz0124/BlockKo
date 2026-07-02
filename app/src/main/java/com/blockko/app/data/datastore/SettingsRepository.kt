package com.blockko.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "blockko_settings")

enum class UpstreamDnsProvider(val label: String, val ip: String) {
    CLOUDFLARE("Cloudflare", "1.1.1.1"),
    GOOGLE("Google", "8.8.8.8"),
    QUAD9("Quad9", "9.9.9.9"),
    CUSTOM("Custom", "")
}

enum class AppLanguage { EN, TL }

const val DEFAULT_BLOCKLIST_URL =
    "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"

data class BlockKoSettings(
    val protectionEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val upstreamDnsProvider: UpstreamDnsProvider = UpstreamDnsProvider.CLOUDFLARE,
    val customUpstreamDns: String = "",
    val language: AppLanguage = AppLanguage.EN,
    val autoStartOnBoot: Boolean = false,
    val pausedUntilMillis: Long = 0L,
    val excludedPackages: Set<String> = emptySet(),
    val blocklistSourceUrl: String = DEFAULT_BLOCKLIST_URL,
    val blocklistLastUpdatedMillis: Long = 0L,
    val totalQueriesToday: Int = 0,
    val totalQueriesDayEpoch: Long = 0L
) {
    val effectiveUpstreamIp: String
        get() = if (upstreamDnsProvider == UpstreamDnsProvider.CUSTOM) customUpstreamDns
        else upstreamDnsProvider.ip
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val UPSTREAM_PROVIDER = stringPreferencesKey("upstream_provider")
        val CUSTOM_UPSTREAM_DNS = stringPreferencesKey("custom_upstream_dns")
        val LANGUAGE = stringPreferencesKey("language")
        val AUTO_START = booleanPreferencesKey("auto_start_on_boot")
        val PAUSED_UNTIL = longPreferencesKey("paused_until_millis")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
        val BLOCKLIST_URL = stringPreferencesKey("blocklist_source_url")
        val BLOCKLIST_UPDATED = longPreferencesKey("blocklist_last_updated")
        val QUERIES_TODAY = intPreferencesKey("total_queries_today")
        val QUERIES_DAY_EPOCH = longPreferencesKey("total_queries_day_epoch")
    }

    val settings: Flow<BlockKoSettings> = context.dataStore.data.map { prefs ->
        BlockKoSettings(
            protectionEnabled = prefs[Keys.PROTECTION_ENABLED] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            upstreamDnsProvider = prefs[Keys.UPSTREAM_PROVIDER]?.let {
                runCatching { UpstreamDnsProvider.valueOf(it) }.getOrNull()
            } ?: UpstreamDnsProvider.CLOUDFLARE,
            customUpstreamDns = prefs[Keys.CUSTOM_UPSTREAM_DNS] ?: "",
            language = prefs[Keys.LANGUAGE]?.let {
                runCatching { AppLanguage.valueOf(it) }.getOrNull()
            } ?: AppLanguage.EN,
            autoStartOnBoot = prefs[Keys.AUTO_START] ?: false,
            pausedUntilMillis = prefs[Keys.PAUSED_UNTIL] ?: 0L,
            excludedPackages = prefs[Keys.EXCLUDED_PACKAGES] ?: emptySet(),
            blocklistSourceUrl = prefs[Keys.BLOCKLIST_URL] ?: DEFAULT_BLOCKLIST_URL,
            blocklistLastUpdatedMillis = prefs[Keys.BLOCKLIST_UPDATED] ?: 0L,
            totalQueriesToday = prefs[Keys.QUERIES_TODAY] ?: 0,
            totalQueriesDayEpoch = prefs[Keys.QUERIES_DAY_EPOCH] ?: 0L
        )
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PROTECTION_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setUpstreamProvider(provider: UpstreamDnsProvider) {
        context.dataStore.edit { it[Keys.UPSTREAM_PROVIDER] = provider.name }
    }

    suspend fun setCustomUpstreamDns(ip: String) {
        context.dataStore.edit { it[Keys.CUSTOM_UPSTREAM_DNS] = ip }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START] = enabled }
    }

    suspend fun setPausedUntil(millis: Long) {
        context.dataStore.edit { it[Keys.PAUSED_UNTIL] = millis }
    }

    suspend fun setExcludedPackages(packages: Set<String>) {
        context.dataStore.edit { it[Keys.EXCLUDED_PACKAGES] = packages }
    }

    suspend fun setBlocklistSourceUrl(url: String) {
        context.dataStore.edit { it[Keys.BLOCKLIST_URL] = url }
    }

    suspend fun setBlocklistLastUpdated(millis: Long) {
        context.dataStore.edit { it[Keys.BLOCKLIST_UPDATED] = millis }
    }

    suspend fun setTotalQueriesToday(count: Int, dayEpoch: Long) {
        context.dataStore.edit {
            it[Keys.QUERIES_TODAY] = count
            it[Keys.QUERIES_DAY_EPOCH] = dayEpoch
        }
    }
}
