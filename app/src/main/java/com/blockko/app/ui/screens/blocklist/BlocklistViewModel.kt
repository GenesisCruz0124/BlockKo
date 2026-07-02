package com.blockko.app.ui.screens.blocklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockko.app.BlockKoApplication
import com.blockko.app.data.blocklist.BlocklistLoadState
import com.blockko.app.data.db.CustomRuleEntity
import com.blockko.app.data.db.CustomRuleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlocklistUiState(
    val domainCount: Int = 0,
    val lastUpdatedMillis: Long = 0L,
    val sourceUrl: String = "",
    val loadState: BlocklistLoadState = BlocklistLoadState.Idle,
    val updateResult: UpdateResult? = null,
    val customRules: List<CustomRuleEntity> = emptyList(),
    val searchQuery: String = ""
)

sealed class UpdateResult {
    data class Success(val count: Int) : UpdateResult()
    data class Failure(val message: String) : UpdateResult()
}

class BlocklistViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<BlockKoApplication>()

    private val _searchQuery = MutableStateFlow("")
    private val _updateResult = MutableStateFlow<UpdateResult?>(null)

    private val rulesFlow = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) app.blocklistRepository.observeCustomRules()
        else app.blocklistRepository.searchCustomRules(query)
    }

    val uiState: StateFlow<BlocklistUiState> = combine(
        app.settingsRepository.settings,
        app.blocklistRepository.loadState,
        rulesFlow,
        _searchQuery,
        _updateResult
    ) { settings, loadState, rules, query, updateResult ->
        BlocklistUiState(
            domainCount = countCache,
            lastUpdatedMillis = settings.blocklistLastUpdatedMillis,
            sourceUrl = settings.blocklistSourceUrl,
            loadState = loadState,
            updateResult = updateResult,
            customRules = rules,
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlocklistUiState())

    @Volatile private var countCache: Int = 0

    init {
        viewModelScope.launch {
            countCache = app.blocklistRepository.blockedDomainCount()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun updateBlocklist(url: String) {
        viewModelScope.launch {
            _updateResult.value = null
            val result = app.blocklistRepository.updateFromUrl(url)
            result.onSuccess { count ->
                countCache = count
                app.settingsRepository.setBlocklistSourceUrl(url)
                app.settingsRepository.setBlocklistLastUpdated(System.currentTimeMillis())
                _updateResult.value = UpdateResult.Success(count)
            }.onFailure { e ->
                _updateResult.value = UpdateResult.Failure(e.message ?: "Update failed")
            }
        }
    }

    fun addRule(domain: String, type: CustomRuleType) {
        if (domain.isBlank()) return
        viewModelScope.launch {
            app.blocklistRepository.addCustomRule(domain.trim(), type)
        }
    }

    fun removeRule(domain: String) {
        viewModelScope.launch {
            app.blocklistRepository.removeCustomRule(domain)
        }
    }
}
