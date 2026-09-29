package com.example.articles.utils

import com.example.articles.models.UILocation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class UtilsTest {

    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(defaultLocale)

    @Test
    fun `decimal parsing accepts a dot`() {
        assertEquals(1.5, "1.50".toDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `decimal parsing accepts a comma - regression for prices silently dropped`() {
        assertEquals(1.5, "1,50".toDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `decimal parsing trims whitespace`() {
        assertEquals(3.0, " 3 ".toDecimalOrNull()!!, 0.0)
    }

    @Test
    fun `decimal parsing rejects garbage and empty input`() {
        assertNull("abc".toDecimalOrNull())
        assertNull("".toDecimalOrNull())
        assertNull("1,5,0".toDecimalOrNull())
    }

    @Test
    fun `email normalization lowercases and trims`() {
        assertEquals("qa.tester@example.com", "  QA.Tester@Example.COM ".normalizeEmail())
    }

    @Test
    fun `email normalization is locale independent`() {
        // In a Turkish locale "I".lowercase() would give a dotless ı.
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        assertEquals("ivan@example.com", "IVAN@EXAMPLE.COM".normalizeEmail())
    }

    @Test
    fun `coordinates are formatted with a dot regardless of locale`() {
        Locale.setDefault(Locale.forLanguageTag("sr-RS"))
        assertEquals("44.81234, 20.45678", UILocation(44.812341, 20.456779).toCoordinatesString())
    }

    @Test
    fun `isInteger and tryToInt`() {
        assertTrue(2.0.isInteger())
        assertFalse(2.5.isInteger())
        assertEquals(2, 2.0.tryToInt())
        assertEquals(2.5, 2.5.tryToInt())
    }
}
