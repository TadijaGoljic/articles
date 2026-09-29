package com.example.articles.db

import com.example.articles.models.UILocation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `location round-trips through its string form`() {
        val location = UILocation(latitude = 44.8125, longitude = -20.4612)
        assertEquals(location, converters.fromString(converters.toString(location)))
    }

    @Test
    fun `location string is latitude comma longitude`() {
        assertEquals("1.5,2.25", converters.toString(UILocation(1.5, 2.25)))
    }

    @Test
    fun `uuid round-trips through its string form`() {
        val uuid = UUID.randomUUID()
        assertEquals(uuid, converters.toUUID(converters.fromUUID(uuid)))
    }
}
