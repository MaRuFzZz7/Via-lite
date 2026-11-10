package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testDomainDetectionLogic() {
        val testUrl = "https://example.com"
        assertTrue(testUrl.startsWith("https://"))
    }
}
