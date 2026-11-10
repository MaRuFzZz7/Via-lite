package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.adblock.AdBlockEngine
import com.example.model.AdBlockRule
import com.example.model.CookieItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ViaLite", appName)
    }

    @Test
    fun testCookieJsonExportAndParse() {
        val cookies = listOf(
            CookieItem(
                name = "session_token",
                value = "abc123xyz",
                domain = ".example.com",
                path = "/",
                secure = true,
                httpOnly = false
            ),
            CookieItem(
                name = "theme",
                value = "dark",
                domain = "example.com",
                path = "/home",
                secure = false,
                httpOnly = false
            )
        )

        val jsonStr = CookieItem.listToJsonString(cookies)
        assertTrue(jsonStr.contains("session_token"))
        assertTrue(jsonStr.contains("abc123xyz"))

        val parsed = CookieItem.parseJsonString(jsonStr)
        assertEquals(2, parsed.size)
        assertEquals("session_token", parsed[0].name)
        assertEquals("abc123xyz", parsed[0].value)
        assertEquals(".example.com", parsed[0].domain)
        assertTrue(parsed[0].secure)

        assertEquals("theme", parsed[1].name)
        assertEquals("dark", parsed[1].value)
    }

    @Test
    fun testAdBlockEngineDefaultAndCustomRules() {
        // Test default blocked host
        assertTrue(AdBlockEngine.shouldBlock("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue(AdBlockEngine.shouldBlock("https://ad.doubleclick.net/ddm/track/clk"))

        // Test normal url is not blocked
        assertFalse(AdBlockEngine.shouldBlock("https://www.google.com/search?q=kotlin"))
        assertFalse(AdBlockEngine.shouldBlock("https://github.com/torvalds/linux"))

        // Test custom rule
        val rule = AdBlockRule(
            id = "rule1",
            pattern = "*.customspamad.net",
            isRegex = false,
            isEnabled = true
        )
        AdBlockEngine.addCustomRule(rule)
        assertTrue(AdBlockEngine.shouldBlock("https://tracker.customspamad.net/pixel.gif"))

        // Disable rule
        AdBlockEngine.toggleCustomRule("rule1", false)
        assertFalse(AdBlockEngine.shouldBlock("https://tracker.customspamad.net/pixel.gif"))
    }
}
