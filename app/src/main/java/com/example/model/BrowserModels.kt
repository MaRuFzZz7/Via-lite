package com.example.model

data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SpeedDialItem(
    val id: String,
    val title: String,
    val url: String,
    val iconLetter: String,
    val colorHex: Long
)

enum class SearchEngine(
    val displayName: String,
    val queryUrl: String,
    val homeUrl: String,
    val iconLetter: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=", "https://www.google.com", "G"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "https://duckduckgo.com", "D"),
    BING("Bing", "https://www.bing.com/search?q=", "https://www.bing.com", "B"),
    BRAVE("Brave", "https://search.brave.com/search?q=", "https://search.brave.com", "Br"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=", "https://www.ecosia.org", "E")
}
