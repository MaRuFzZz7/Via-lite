package com.example.cookie

import android.net.Uri
import android.webkit.CookieManager
import com.example.model.CookieItem

object CookieHelper {

    fun getCookiesForUrl(url: String): List<CookieItem> {
        val list = mutableListOf<CookieItem>()
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) {
            return list
        }

        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return list
        }

        val host = uri.host ?: ""
        val isSecure = uri.scheme?.equals("https", ignoreCase = true) == true
        val rawCookies = try {
            CookieManager.getInstance().getCookie(url)
        } catch (_: Exception) {
            null
        }

        if (!rawCookies.isNullOrBlank()) {
            val pairs = rawCookies.split(";")
            for (pair in pairs) {
                val trimmed = pair.trim()
                if (trimmed.isEmpty()) continue
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx != -1) {
                    val name = trimmed.substring(0, eqIdx).trim()
                    val value = trimmed.substring(eqIdx + 1).trim()
                    list.add(
                        CookieItem(
                            name = name,
                            value = value,
                            domain = host,
                            path = "/",
                            secure = isSecure,
                            httpOnly = false,
                            sameSite = "lax",
                            session = true
                        )
                    )
                } else {
                    list.add(
                        CookieItem(
                            name = trimmed,
                            value = "",
                            domain = host,
                            path = "/",
                            secure = isSecure
                        )
                    )
                }
            }
        }
        return list
    }

    fun exportCookiesAsJson(cookies: List<CookieItem>): String {
        return CookieItem.listToJsonString(cookies, indentSpaces = 2)
    }

    fun importCookiesFromJson(targetUrl: String, jsonString: String): Result<Int> {
        return try {
            val parsedCookies = CookieItem.parseJsonString(jsonString)
            if (parsedCookies.isEmpty()) {
                return Result.failure(IllegalArgumentException("No valid cookies found in JSON"))
            }

            val cookieManager = CookieManager.getInstance()
            val uri = Uri.parse(targetUrl)
            val defaultDomain = uri.host ?: ""
            val defaultPath = uri.path?.ifEmpty { "/" } ?: "/"
            val isSecure = uri.scheme?.equals("https", ignoreCase = true) == true

            var count = 0
            for (item in parsedCookies) {
                val domain = item.domain.ifBlank { defaultDomain }
                val path = item.path.ifBlank { defaultPath }
                val resolvedItem = item.copy(
                    domain = domain,
                    path = path,
                    secure = item.secure || isSecure
                )
                cookieManager.setCookie(targetUrl, resolvedItem.toCookieHeader())
                count++
            }
            cookieManager.flush()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setCookie(targetUrl: String, cookie: CookieItem) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie(targetUrl, cookie.toCookieHeader())
            cookieManager.flush()
        } catch (_: Exception) {}
    }

    fun deleteCookie(targetUrl: String, cookieName: String, domain: String, path: String = "/") {
        try {
            val cookieManager = CookieManager.getInstance()
            val expiredCookie = "$cookieName=; Domain=$domain; Path=$path; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
            cookieManager.setCookie(targetUrl, expiredCookie)
            cookieManager.flush()
        } catch (_: Exception) {}
    }

    fun clearAllCookies(onComplete: (() -> Unit)? = null) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.flush()
                onComplete?.invoke()
            }
        } catch (_: Exception) {
            onComplete?.invoke()
        }
    }
}
