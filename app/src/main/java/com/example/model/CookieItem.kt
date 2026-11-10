package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class CookieItem(
    val name: String,
    val value: String,
    val domain: String = "",
    val path: String = "/",
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val sameSite: String = "lax",
    val session: Boolean = true,
    val expirationDate: Long? = null
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("value", value)
        json.put("domain", domain)
        json.put("path", path)
        json.put("secure", secure)
        json.put("httpOnly", httpOnly)
        json.put("sameSite", sameSite)
        json.put("session", session)
        if (expirationDate != null) {
            json.put("expirationDate", expirationDate)
        }
        return json
    }

    fun toCookieHeader(): String {
        val sb = StringBuilder("$name=$value; Path=$path")
        if (domain.isNotBlank()) {
            sb.append("; Domain=$domain")
        }
        if (secure) {
            sb.append("; Secure")
        }
        if (httpOnly) {
            sb.append("; HttpOnly")
        }
        if (sameSite.isNotBlank()) {
            sb.append("; SameSite=$sameSite")
        }
        return sb.toString()
    }

    companion object {
        fun fromJsonObject(json: JSONObject): CookieItem {
            return CookieItem(
                name = json.optString("name", ""),
                value = json.optString("value", ""),
                domain = json.optString("domain", ""),
                path = json.optString("path", "/"),
                secure = json.optBoolean("secure", false),
                httpOnly = json.optBoolean("httpOnly", false),
                sameSite = json.optString("sameSite", "lax"),
                session = json.optBoolean("session", true),
                expirationDate = if (json.has("expirationDate")) json.optLong("expirationDate") else null
            )
        }

        fun listToJsonString(cookies: List<CookieItem>, indentSpaces: Int = 2): String {
            val jsonArray = JSONArray()
            for (cookie in cookies) {
                jsonArray.put(cookie.toJsonObject())
            }
            return jsonArray.toString(indentSpaces)
        }

        fun parseJsonString(jsonStr: String): List<CookieItem> {
            val list = mutableListOf<CookieItem>()
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        list.add(fromJsonObject(obj))
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                list.add(fromJsonObject(obj))
            }
            return list
        }
    }
}
