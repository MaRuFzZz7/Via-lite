package com.example.adblock

import android.net.Uri
import com.example.model.AdBlockRule
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

object AdBlockEngine {
    // Curated high-performance default ad & tracking host domains
    private val DEFAULT_BLOCKED_HOSTS = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "partner.googleadservices.com",
        "analytics.google.com",
        "google-analytics.com",
        "adnxs.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "criteo.net",
        "scorecardresearch.com",
        "quantserve.com",
        "popads.net",
        "adroll.com",
        "amazon-adsystem.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "advertising.com",
        "adcolony.com",
        "applovin.com",
        "unityads.unity3d.com",
        "ironsrc.com",
        "vungle.com",
        "chartboost.com",
        "admob.com",
        "bidswitch.net",
        "smartadserver.com",
        "exponential.com",
        "inmobi.com",
        "moatads.com",
        "adsafeprotected.com",
        "hotjar.com",
        "clarity.ms",
        "segment.io",
        "mixpanel.com",
        "pixel.facebook.com"
    )

    private val DEFAULT_BLOCKED_PATHS = arrayOf(
        "/pagead/",
        "/ads/show_ads.js",
        "/google-analytics.com/analytics.js",
        "/gtag/js?id=",
        "facebook.com/tr/",
        "/ads?client=",
        "/wp-content/plugins/ad-inserter/"
    )

    val customRules: MutableList<AdBlockRule> = CopyOnWriteArrayList()
    private val compiledRegexCache = Collections.synchronizedMap(mutableMapOf<String, Pattern>())

    fun initCustomRules(rules: List<AdBlockRule>) {
        customRules.clear()
        customRules.addAll(rules)
        compiledRegexCache.clear()
        for (rule in rules) {
            if (rule.isRegex && rule.isEnabled) {
                try {
                    compiledRegexCache[rule.id] = Pattern.compile(rule.pattern, Pattern.CASE_INSENSITIVE)
                } catch (_: Exception) {}
            }
        }
    }

    fun addCustomRule(rule: AdBlockRule) {
        customRules.add(rule)
        if (rule.isRegex && rule.isEnabled) {
            try {
                compiledRegexCache[rule.id] = Pattern.compile(rule.pattern, Pattern.CASE_INSENSITIVE)
            } catch (_: Exception) {}
        }
    }

    fun removeCustomRule(ruleId: String) {
        customRules.removeAll { it.id == ruleId }
        compiledRegexCache.remove(ruleId)
    }

    fun toggleCustomRule(ruleId: String, enabled: Boolean) {
        val index = customRules.indexOfFirst { it.id == ruleId }
        if (index != -1) {
            val rule = customRules[index].copy(isEnabled = enabled)
            customRules[index] = rule
            if (rule.isRegex) {
                if (enabled) {
                    try {
                        compiledRegexCache[rule.id] = Pattern.compile(rule.pattern, Pattern.CASE_INSENSITIVE)
                    } catch (_: Exception) {}
                } else {
                    compiledRegexCache.remove(rule.id)
                }
            }
        }
    }

    fun shouldBlock(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }

        val host = uri.host?.lowercase() ?: ""
        if (host.isBlank()) return false

        // 1. Check default blocked hosts (exact or subdomains)
        if (DEFAULT_BLOCKED_HOSTS.contains(host)) return true
        for (blockedHost in DEFAULT_BLOCKED_HOSTS) {
            if (host.endsWith(".$blockedHost")) return true
        }

        // 2. Check path patterns
        val fullUrl = url.lowercase()
        for (path in DEFAULT_BLOCKED_PATHS) {
            if (fullUrl.contains(path)) return true
        }

        // 3. Check custom user rules
        for (rule in customRules) {
            if (!rule.isEnabled) continue
            if (rule.isRegex) {
                val pattern = compiledRegexCache[rule.id] ?: try {
                    val p = Pattern.compile(rule.pattern, Pattern.CASE_INSENSITIVE)
                    compiledRegexCache[rule.id] = p
                    p
                } catch (_: Exception) {
                    null
                }
                if (pattern != null && pattern.matcher(url).find()) {
                    return true
                }
            } else {
                val patternLower = rule.pattern.lowercase().trim()
                if (patternLower.isNotBlank()) {
                    if (patternLower.startsWith("*.")) {
                        val base = patternLower.removePrefix("*.")
                        if (host == base || host.endsWith(".$base")) return true
                    } else if (host.contains(patternLower) || fullUrl.contains(patternLower)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    const val COSMETIC_FILTER_JS: String = """
(function() {
    if (window._vialite_adblock_injected) return;
    window._vialite_adblock_injected = true;
    function injectAdStyle() {
        var style = document.createElement('style');
        style.id = 'vialite-adblock-style';
        style.type = 'text/css';
        style.innerHTML = 'ins.adsbygoogle, .adsbygoogle, [class*="google-auto-placed"], [id*="google_ads"], div[class*="ad-container"], div[class*="ad-banner"], div[id*="ad-banner"], iframe[src*="doubleclick"], iframe[src*="adservice"], iframe[src*="googleadservices"], div[class*="sponsored-post"] { display: none !important; height: 0 !important; visibility: hidden !important; pointer-events: none !important; }';
        (document.head || document.documentElement).appendChild(style);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', injectAdStyle);
    } else {
        injectAdStyle();
    }
})();
"""

    const val DARK_MODE_JS: String = """
(function() {
    var existing = document.getElementById('vialite-dark-style');
    if (!existing) {
        var style = document.createElement('style');
        style.id = 'vialite-dark-style';
        style.type = 'text/css';
        style.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; background-color: #121212 !important; } img, video, canvas, svg, [style*="background-image"], picture { filter: invert(100%) hue-rotate(180deg) !important; }';
        (document.head || document.documentElement).appendChild(style);
    }
})();
"""

    const val REMOVE_DARK_MODE_JS: String = """
(function() {
    var existing = document.getElementById('vialite-dark-style');
    if (existing && existing.parentNode) {
        existing.parentNode.removeChild(existing);
    }
})();
"""
}
