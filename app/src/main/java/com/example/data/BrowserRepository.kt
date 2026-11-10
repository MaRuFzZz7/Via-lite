package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AdBlockRule
import com.example.model.BookmarkItem
import com.example.model.HistoryItem
import com.example.model.SearchEngine
import com.example.model.SpeedDialItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class BrowserRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vialite_browser_prefs", Context.MODE_PRIVATE)

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _customRules = MutableStateFlow<List<AdBlockRule>>(emptyList())
    val customRules: StateFlow<List<AdBlockRule>> = _customRules.asStateFlow()

    private val _speedDials = MutableStateFlow<List<SpeedDialItem>>(emptyList())
    val speedDials: StateFlow<List<SpeedDialItem>> = _speedDials.asStateFlow()

    private val _searchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    private val _isAdBlockerEnabled = MutableStateFlow(true)
    val isAdBlockerEnabled: StateFlow<Boolean> = _isAdBlockerEnabled.asStateFlow()

    private val _isNightModeEnabled = MutableStateFlow(false)
    val isNightModeEnabled: StateFlow<Boolean> = _isNightModeEnabled.asStateFlow()

    private val _isAmoledTheme = MutableStateFlow(true)
    val isAmoledTheme: StateFlow<Boolean> = _isAmoledTheme.asStateFlow()

    private val _isTextOnlyMode = MutableStateFlow(false)
    val isTextOnlyMode: StateFlow<Boolean> = _isTextOnlyMode.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        // Search Engine
        val seName = prefs.getString("search_engine", SearchEngine.GOOGLE.name)
        _searchEngine.value = try {
            SearchEngine.valueOf(seName ?: SearchEngine.GOOGLE.name)
        } catch (_: Exception) {
            SearchEngine.GOOGLE
        }

        // Toggles
        _isAdBlockerEnabled.value = prefs.getBoolean("ad_blocker_enabled", true)
        _isNightModeEnabled.value = prefs.getBoolean("night_mode_enabled", false)
        _isAmoledTheme.value = prefs.getBoolean("amoled_theme", true)
        _isTextOnlyMode.value = prefs.getBoolean("text_only_mode", false)

        // Bookmarks
        loadBookmarks()

        // History
        loadHistory()

        // Custom Adblock rules
        loadCustomRules()

        // Speed Dials
        loadSpeedDials()
    }

    // ================= Bookmarks =================
    private fun loadBookmarks() {
        val jsonStr = prefs.getString("bookmarks_json", null)
        val list = mutableListOf<BookmarkItem>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BookmarkItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            url = obj.getString("url"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty()) {
            list.add(BookmarkItem(UUID.randomUUID().toString(), "Google", "https://www.google.com"))
            list.add(BookmarkItem(UUID.randomUUID().toString(), "Wikipedia", "https://www.wikipedia.org"))
            list.add(BookmarkItem(UUID.randomUUID().toString(), "GitHub", "https://github.com"))
        }
        _bookmarks.value = list
    }

    private fun saveBookmarks(list: List<BookmarkItem>) {
        _bookmarks.value = list
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("bookmarks_json", array.toString()).apply()
    }

    fun addBookmark(title: String, url: String) {
        val current = _bookmarks.value.toMutableList()
        // If already exists, update title
        val existingIndex = current.indexOfFirst { it.url.equals(url, ignoreCase = true) }
        if (existingIndex != -1) {
            current[existingIndex] = current[existingIndex].copy(title = title, timestamp = System.currentTimeMillis())
        } else {
            current.add(0, BookmarkItem(UUID.randomUUID().toString(), title.ifBlank { url }, url))
        }
        saveBookmarks(current)
    }

    fun removeBookmark(id: String) {
        val current = _bookmarks.value.filter { it.id != id }
        saveBookmarks(current)
    }

    fun isBookmarked(url: String): Boolean {
        return _bookmarks.value.any { it.url.equals(url, ignoreCase = true) }
    }

    // ================= History =================
    private fun loadHistory() {
        val jsonStr = prefs.getString("history_json", null)
        val list = mutableListOf<HistoryItem>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HistoryItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            url = obj.getString("url"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _history.value = list
    }

    private fun saveHistory(list: List<HistoryItem>) {
        _history.value = list
        val array = JSONArray()
        for (item in list.take(150)) { // Keep last 150 entries for lightweight storage
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("history_json", array.toString()).apply()
    }

    fun addHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val current = _history.value.toMutableList()
        current.removeAll { it.url.equals(url, ignoreCase = true) }
        current.add(0, HistoryItem(UUID.randomUUID().toString(), title.ifBlank { url }, url))
        saveHistory(current)
    }

    fun removeHistoryItem(id: String) {
        val current = _history.value.filter { it.id != id }
        saveHistory(current)
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }

    // ================= Custom AdBlock Rules =================
    private fun loadCustomRules() {
        val jsonStr = prefs.getString("custom_ad_rules_json", null)
        val list = mutableListOf<AdBlockRule>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AdBlockRule(
                            id = obj.getString("id"),
                            pattern = obj.getString("pattern"),
                            isRegex = obj.optBoolean("isRegex", false),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            description = obj.optString("description", "")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty()) {
            list.add(AdBlockRule(UUID.randomUUID().toString(), "*.popads.net", isRegex = false, isEnabled = true, "Pop-up Ad Network"))
            list.add(AdBlockRule(UUID.randomUUID().toString(), "adservice", isRegex = false, isEnabled = true, "AdService keywords"))
            list.add(AdBlockRule(UUID.randomUUID().toString(), "telemetry", isRegex = false, isEnabled = true, "Tracking telemetry"))
        }
        _customRules.value = list
    }

    private fun saveCustomRules(list: List<AdBlockRule>) {
        _customRules.value = list
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("pattern", item.pattern)
            obj.put("isRegex", item.isRegex)
            obj.put("isEnabled", item.isEnabled)
            obj.put("description", item.description)
            array.put(obj)
        }
        prefs.edit().putString("custom_ad_rules_json", array.toString()).apply()
    }

    fun addCustomRule(pattern: String, isRegex: Boolean, description: String = "") {
        val current = _customRules.value.toMutableList()
        current.add(0, AdBlockRule(UUID.randomUUID().toString(), pattern.trim(), isRegex, true, description))
        saveCustomRules(current)
    }

    fun removeCustomRule(id: String) {
        val current = _customRules.value.filter { it.id != id }
        saveCustomRules(current)
    }

    fun toggleCustomRule(id: String, enabled: Boolean) {
        val current = _customRules.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        saveCustomRules(current)
    }

    // ================= Speed Dials =================
    private fun loadSpeedDials() {
        val jsonStr = prefs.getString("speed_dials_json", null)
        val list = mutableListOf<SpeedDialItem>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SpeedDialItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            url = obj.getString("url"),
                            iconLetter = obj.optString("iconLetter", "W"),
                            colorHex = obj.optLong("colorHex", 0xFF0284C7)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty()) {
            list.add(SpeedDialItem("1", "Google", "https://www.google.com", "G", 0xFF4285F4))
            list.add(SpeedDialItem("2", "YouTube", "https://www.youtube.com", "Y", 0xFFFF0000))
            list.add(SpeedDialItem("3", "GitHub", "https://github.com", "GH", 0xFF24292E))
            list.add(SpeedDialItem("4", "Wikipedia", "https://www.wikipedia.org", "W", 0xFF636466))
            list.add(SpeedDialItem("5", "Reddit", "https://www.reddit.com", "R", 0xFFFF4500))
            list.add(SpeedDialItem("6", "DuckDuckGo", "https://duckduckgo.com", "D", 0xFFDE5833))
            list.add(SpeedDialItem("7", "Hacker News", "https://news.ycombinator.com", "HN", 0xFFFF6600))
            list.add(SpeedDialItem("8", "Bing", "https://www.bing.com", "B", 0xFF00809D))
        }
        _speedDials.value = list
    }

    private fun saveSpeedDials(list: List<SpeedDialItem>) {
        _speedDials.value = list
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("iconLetter", item.iconLetter)
            obj.put("colorHex", item.colorHex)
            array.put(obj)
        }
        prefs.edit().putString("speed_dials_json", array.toString()).apply()
    }

    fun addSpeedDial(title: String, url: String) {
        val current = _speedDials.value.toMutableList()
        val letter = title.take(2).uppercase().ifBlank { "W" }
        val colors = listOf(0xFF0284C7, 0xFF7C3AED, 0xFF059669, 0xFFDB2777, 0xFFD97706, 0xFF2563EB)
        val color = colors[current.size % colors.size]
        current.add(SpeedDialItem(UUID.randomUUID().toString(), title, url, letter, color))
        saveSpeedDials(current)
    }

    fun removeSpeedDial(id: String) {
        val current = _speedDials.value.filter { it.id != id }
        saveSpeedDials(current)
    }

    // ================= Preferences =================
    fun setSearchEngine(engine: SearchEngine) {
        _searchEngine.value = engine
        prefs.edit().putString("search_engine", engine.name).apply()
    }

    fun setAdBlockerEnabled(enabled: Boolean) {
        _isAdBlockerEnabled.value = enabled
        prefs.edit().putBoolean("ad_blocker_enabled", enabled).apply()
    }

    fun setNightModeEnabled(enabled: Boolean) {
        _isNightModeEnabled.value = enabled
        prefs.edit().putBoolean("night_mode_enabled", enabled).apply()
    }

    fun setAmoledTheme(enabled: Boolean) {
        _isAmoledTheme.value = enabled
        prefs.edit().putBoolean("amoled_theme", enabled).apply()
    }

    fun setTextOnlyMode(enabled: Boolean) {
        _isTextOnlyMode.value = enabled
        prefs.edit().putBoolean("text_only_mode", enabled).apply()
    }
}
