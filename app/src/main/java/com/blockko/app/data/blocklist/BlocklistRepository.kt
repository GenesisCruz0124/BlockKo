package com.blockko.app.data.blocklist

import android.content.Context
import com.blockko.app.data.db.BlockKoDatabase
import com.blockko.app.data.db.BlocklistDomainEntity
import com.blockko.app.data.db.CustomRuleEntity
import com.blockko.app.data.db.CustomRuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

sealed class BlocklistLoadState {
    data object Idle : BlocklistLoadState()
    data class Loading(val progress: Float) : BlocklistLoadState()
    data class Done(val domainCount: Int) : BlocklistLoadState()
    data class Error(val message: String) : BlocklistLoadState()
}

private const val ASSET_STARTER_BLOCKLIST = "blocklist_starter.txt"
private const val INSERT_BATCH_SIZE = 2000

/**
 * Owns the in-memory HashSet caches used by the VPN's hot DNS-filtering path.
 * Room is the source of truth on disk; [refreshCache] rebuilds the O(1) lookup
 * sets from it. Custom ALLOW rules always take precedence over blocked domains.
 */
class BlocklistRepository(
    private val context: Context,
    private val db: BlockKoDatabase
) {
    private val blockSetRef = AtomicReference(emptySet<String>())
    private val allowSetRef = AtomicReference(emptySet<String>())

    private val _loadState = MutableStateFlow<BlocklistLoadState>(BlocklistLoadState.Idle)
    val loadState: StateFlow<BlocklistLoadState> = _loadState

    suspend fun ensureInitialLoadFromBundledAsset() {
        if (db.blocklistDomainDao().count() > 0) {
            refreshCache()
            _loadState.value = BlocklistLoadState.Done(db.blocklistDomainDao().count())
            return
        }
        _loadState.value = BlocklistLoadState.Loading(0f)
        try {
            val domains = context.assets.open(ASSET_STARTER_BLOCKLIST).bufferedReader().use {
                HostsFileParser.parse(it)
            }
            insertDomainsInBatches(domains) { progress -> _loadState.value = BlocklistLoadState.Loading(progress) }
            refreshCache()
            _loadState.value = BlocklistLoadState.Done(domains.size)
        } catch (e: Exception) {
            _loadState.value = BlocklistLoadState.Error(e.message ?: "Failed to load starter blocklist")
        }
    }

    suspend fun updateFromUrl(url: String): Result<Int> = try {
        val domains = downloadHostsFile(url)
        if (domains.isEmpty()) {
            Result.failure(IllegalStateException("No domains found at that URL"))
        } else {
            db.blocklistDomainDao().clearAll()
            insertDomainsInBatches(domains) {}
            refreshCache()
            Result.success(domains.size)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun downloadHostsFile(url: String): Set<String> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
        }
        return try {
            connection.inputStream.bufferedReader().use { reader: BufferedReader ->
                HostsFileParser.parse(reader)
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun insertDomainsInBatches(
        domains: Set<String>,
        onProgress: (Float) -> Unit
    ) {
        val dao = db.blocklistDomainDao()
        val list = domains.toList()
        var inserted = 0
        list.chunked(INSERT_BATCH_SIZE).forEach { batch ->
            dao.insertAll(batch.map { BlocklistDomainEntity(it) })
            inserted += batch.size
            onProgress(inserted.toFloat() / list.size)
        }
    }

    suspend fun refreshCache() {
        val blocklistDomains = db.blocklistDomainDao().getAllDomains()
        val customRules = db.customRuleDao().getAllOnce()

        val blocked = HashSet<String>(blocklistDomains.size + 64)
        blocked.addAll(blocklistDomains)

        val allowed = HashSet<String>(64)
        customRules.forEach { rule ->
            when (rule.type) {
                CustomRuleType.BLOCK -> blocked.add(rule.domain)
                CustomRuleType.ALLOW -> allowed.add(rule.domain)
            }
        }
        blockSetRef.set(blocked)
        allowSetRef.set(allowed)
    }

    /** O(1)-ish match: exact domain, then walk parent domains down to the registrable (2-label) domain. */
    fun isBlocked(domain: String): Boolean {
        val blocked = blockSetRef.get()
        val allowed = allowSetRef.get()
        var current = domain.lowercase().removeSuffix(".")
        while (true) {
            if (current in allowed) return false
            if (current in blocked) return true
            val dotIndex = current.indexOf('.')
            if (dotIndex < 0) return false
            val parent = current.substring(dotIndex + 1)
            if (!parent.contains('.')) return false
            current = parent
        }
    }

    suspend fun addCustomRule(domain: String, type: CustomRuleType) {
        db.customRuleDao().upsert(CustomRuleEntity(domain.trim().lowercase(), type))
        refreshCache()
    }

    suspend fun removeCustomRule(domain: String) {
        db.customRuleDao().deleteByDomain(domain.trim().lowercase())
        refreshCache()
    }

    fun observeCustomRules(): Flow<List<CustomRuleEntity>> = db.customRuleDao().observeAll()

    fun searchCustomRules(query: String): Flow<List<CustomRuleEntity>> = db.customRuleDao().search(query)

    suspend fun blockedDomainCount(): Int = db.blocklistDomainDao().count()
}
