package com.example.model

data class WebTab(
    val id: String,
    val url: String = "",
    val title: String = "New Tab",
    val faviconUrl: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isIncognito: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val blockedCount: Int = 0,
    val isDesktopMode: Boolean = false
) {
    val isHome: Boolean
        get() = url.isBlank() || url == "about:blank"
}
