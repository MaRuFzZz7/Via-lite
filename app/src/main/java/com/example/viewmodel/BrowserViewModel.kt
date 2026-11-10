package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adblock.AdBlockEngine
import com.example.cookie.CookieHelper
import com.example.data.BrowserRepository
import com.example.model.AdBlockRule
import com.example.model.CookieItem
import com.example.model.SearchEngine
import com.example.model.WebTab
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    val repository = BrowserRepository(application)

    private val _tabs = MutableStateFlow<List<WebTab>>(emptyList())
    val tabs: StateFlow<List<WebTab>> = _tabs.asStateFlow()

    private val _currentTabId = MutableStateFlow<String>("")
    val currentTabId: StateFlow<String> = _currentTabId.asStateFlow()

    val currentTab: StateFlow<WebTab?> = combine(_tabs, _currentTabId) { tabsList, curId ->
        tabsList.find { it.id == curId } ?: tabsList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Current page blocked ads & tracking URLs
    private val _pageBlockedUrls = MutableStateFlow<List<String>>(emptyList())
    val pageBlockedUrls: StateFlow<List<String>> = _pageBlockedUrls.asStateFlow()

    // UI Sheets & Dialogs State
    val isTabManagerOpen = MutableStateFlow(false)
    val isMenuOpen = MutableStateFlow(false)
    val isCookieEditorOpen = MutableStateFlow(false)
    val isAdBlockerSheetOpen = MutableStateFlow(false)
    val isBookmarksHistoryOpen = MutableStateFlow(false)
    val isSettingsOpen = MutableStateFlow(false)
    val isFindInPageOpen = MutableStateFlow(false)
    val findQuery = MutableStateFlow("")

    // Cookie Editor State
    private val _currentCookies = MutableStateFlow<List<CookieItem>>(emptyList())
    val currentCookies: StateFlow<List<CookieItem>> = _currentCookies.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Navigation trigger from ViewModel to active WebView
    sealed class WebAction {
        data class LoadUrl(val tabId: String, val url: String) : WebAction()
        data class GoBack(val tabId: String) : WebAction()
        data class GoForward(val tabId: String) : WebAction()
        data class Reload(val tabId: String) : WebAction()
        data class FindInPage(val query: String) : WebAction()
        data class ClearFindMatches(val tabId: String) : WebAction()
    }

    private val _webAction = MutableSharedFlow<WebAction>()
    val webAction: SharedFlow<WebAction> = _webAction.asSharedFlow()

    init {
        // Init AdBlockEngine with stored rules
        viewModelScope.launch {
            repository.customRules.collect { rules ->
                AdBlockEngine.initCustomRules(rules)
            }
        }
        // Create initial tab
        createNewTab()
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    // ================= Tabs Management =================
    fun createNewTab(url: String = "", isIncognito: Boolean = false) {
        val newId = UUID.randomUUID().toString()
        val newTab = WebTab(
            id = newId,
            url = url,
            title = if (url.isBlank()) "Home" else url,
            isIncognito = isIncognito
        )
        val updated = _tabs.value + newTab
        _tabs.value = updated
        _currentTabId.value = newId
        _pageBlockedUrls.value = emptyList()

        if (url.isNotBlank()) {
            viewModelScope.launch {
                _webAction.emit(WebAction.LoadUrl(newId, url))
            }
        }
    }

    fun switchTab(tabId: String) {
        if (_currentTabId.value != tabId && _tabs.value.any { it.id == tabId }) {
            _currentTabId.value = tabId
            _pageBlockedUrls.value = emptyList()
            isTabManagerOpen.value = false
        }
    }

    fun closeTab(tabId: String) {
        val curList = _tabs.value
        if (curList.size <= 1) {
            // Reset last tab to blank home
            _tabs.value = listOf(WebTab(id = UUID.randomUUID().toString(), url = "", title = "Home"))
            _currentTabId.value = _tabs.value.first().id
            _pageBlockedUrls.value = emptyList()
            return
        }

        val idx = curList.indexOfFirst { it.id == tabId }
        val updated = curList.filter { it.id != tabId }
        _tabs.value = updated

        if (_currentTabId.value == tabId) {
            val newIndex = if (idx >= updated.size) updated.size - 1 else idx
            _currentTabId.value = updated[newIndex].id
            _pageBlockedUrls.value = emptyList()
        }
    }

    fun closeAllTabs() {
        val newTab = WebTab(id = UUID.randomUUID().toString(), url = "", title = "Home")
        _tabs.value = listOf(newTab)
        _currentTabId.value = newTab.id
        _pageBlockedUrls.value = emptyList()
        isTabManagerOpen.value = false
    }

    // ================= Web Navigation =================
    fun navigateTo(input: String) {
        val tab = currentTab.value ?: return
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val targetUrl = resolveUrlOrSearch(trimmed, repository.searchEngine.value)
        updateTabUrl(tab.id, targetUrl, targetUrl)
        viewModelScope.launch {
            _webAction.emit(WebAction.LoadUrl(tab.id, targetUrl))
        }
    }

    private fun resolveUrlOrSearch(input: String, engine: SearchEngine): String {
        val lower = input.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> input
            lower.startsWith("about:") || lower.startsWith("data:") || lower.startsWith("javascript:") -> input
            looksLikeDomain(input) -> "https://$input"
            else -> engine.queryUrl + URLEncoder.encode(input, "UTF-8")
        }
    }

    private fun looksLikeDomain(input: String): Boolean {
        if (input.contains(" ")) return false
        val slashIdx = input.indexOf('/')
        val hostPart = if (slashIdx != -1) input.substring(0, slashIdx) else input
        return hostPart.contains(".") && hostPart.length > 3
    }

    fun goBack() {
        currentTab.value?.let { tab ->
            viewModelScope.launch {
                _webAction.emit(WebAction.GoBack(tab.id))
            }
        }
    }

    fun goForward() {
        currentTab.value?.let { tab ->
            viewModelScope.launch {
                _webAction.emit(WebAction.GoForward(tab.id))
            }
        }
    }

    fun reload() {
        currentTab.value?.let { tab ->
            viewModelScope.launch {
                _webAction.emit(WebAction.Reload(tab.id))
            }
        }
    }

    fun goHome() {
        currentTab.value?.let { tab ->
            updateTabUrl(tab.id, "", "Home")
            _pageBlockedUrls.value = emptyList()
        }
    }

    // ================= Tab State Updates from WebView =================
    fun updateTabUrl(tabId: String, url: String, title: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) {
                it.copy(url = url, title = title.ifBlank { url })
            } else it
        }
        val tab = _tabs.value.find { it.id == tabId }
        if (tab != null && !tab.isIncognito && url.isNotBlank() && !url.startsWith("about:")) {
            repository.addHistory(title, url)
        }
    }

    fun updateTabProgress(tabId: String, isLoading: Boolean, progress: Int) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) {
                it.copy(isLoading = isLoading, progress = progress)
            } else it
        }
    }

    fun updateTabNavigation(tabId: String, canBack: Boolean, canForward: Boolean) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) {
                it.copy(canGoBack = canBack, canGoForward = canForward)
            } else it
        }
    }

    fun recordBlockedUrl(tabId: String, url: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) {
                it.copy(blockedCount = it.blockedCount + 1)
            } else it
        }
        if (tabId == _currentTabId.value) {
            _pageBlockedUrls.value = (_pageBlockedUrls.value + url).takeLast(60)
        }
    }

    fun resetTabBlockedCount(tabId: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) {
                it.copy(blockedCount = 0)
            } else it
        }
        if (tabId == _currentTabId.value) {
            _pageBlockedUrls.value = emptyList()
        }
    }

    fun toggleDesktopMode() {
        val tab = currentTab.value ?: return
        val newMode = !tab.isDesktopMode
        _tabs.value = _tabs.value.map {
            if (it.id == tab.id) it.copy(isDesktopMode = newMode) else it
        }
        reload()
        showToast(if (newMode) "Desktop Mode enabled" else "Mobile Mode enabled")
    }

    fun toggleNightMode() {
        val newMode = !repository.isNightModeEnabled.value
        repository.setNightModeEnabled(newMode)
        showToast(if (newMode) "Dark Mode active" else "Dark Mode disabled")
    }

    fun toggleAdBlocker() {
        val newMode = !repository.isAdBlockerEnabled.value
        repository.setAdBlockerEnabled(newMode)
        showToast(if (newMode) "Ad Blocker enabled" else "Ad Blocker disabled")
    }

    fun toggleTextOnly() {
        val newMode = !repository.isTextOnlyMode.value
        repository.setTextOnlyMode(newMode)
        reload()
        showToast(if (newMode) "Text-Only mode (Fast load) ON" else "Image loading enabled")
    }

    // ================= Cookie Editor =================
    fun loadCookiesForCurrentTab() {
        val tab = currentTab.value
        if (tab != null && tab.url.isNotBlank()) {
            val list = CookieHelper.getCookiesForUrl(tab.url)
            _currentCookies.value = list
        } else {
            _currentCookies.value = emptyList()
        }
    }

    fun getExportableJsonCookies(): String {
        return CookieHelper.exportCookiesAsJson(_currentCookies.value)
    }

    fun importCookies(jsonStr: String) {
        val tab = currentTab.value ?: return
        if (tab.url.isBlank()) {
            showToast("Open a website to import cookies")
            return
        }
        val result = CookieHelper.importCookiesFromJson(tab.url, jsonStr)
        result.onSuccess { count ->
            showToast("Successfully imported $count cookies!")
            loadCookiesForCurrentTab()
        }.onFailure { err ->
            showToast("Failed to import JSON: ${err.message}")
        }
    }

    fun addOrUpdateCookie(cookie: CookieItem) {
        val tab = currentTab.value ?: return
        CookieHelper.setCookie(tab.url, cookie)
        loadCookiesForCurrentTab()
        showToast("Cookie saved!")
    }

    fun deleteCookie(cookie: CookieItem) {
        val tab = currentTab.value ?: return
        CookieHelper.deleteCookie(tab.url, cookie.name, cookie.domain, cookie.path)
        loadCookiesForCurrentTab()
        showToast("Cookie deleted!")
    }

    fun clearAllCookiesForSite() {
        val tab = currentTab.value ?: return
        for (cookie in _currentCookies.value) {
            CookieHelper.deleteCookie(tab.url, cookie.name, cookie.domain, cookie.path)
        }
        loadCookiesForCurrentTab()
        showToast("Cookies cleared for this site")
    }

    // ================= Custom Ad Rules =================
    fun addCustomAdRule(pattern: String, isRegex: Boolean, description: String = "") {
        repository.addCustomRule(pattern, isRegex, description)
        showToast("Rule added: $pattern")
    }

    fun removeCustomAdRule(ruleId: String) {
        repository.removeCustomRule(ruleId)
        showToast("Rule removed")
    }

    fun toggleCustomAdRule(ruleId: String, enabled: Boolean) {
        repository.toggleCustomRule(ruleId, enabled)
    }

    // ================= Find in Page =================
    fun performFind(query: String) {
        findQuery.value = query
        viewModelScope.launch {
            _webAction.emit(WebAction.FindInPage(query))
        }
    }

    fun clearFind() {
        findQuery.value = ""
        isFindInPageOpen.value = false
        currentTab.value?.let { tab ->
            viewModelScope.launch {
                _webAction.emit(WebAction.ClearFindMatches(tab.id))
            }
        }
    }
}
