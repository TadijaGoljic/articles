package com.example.articles.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PositionResolverTest {

    @Test
    fun `empty list starts at the gap`() {
        assertEquals(1000.0, PositionResolver.resolve(null, null)!!, 0.0)
    }

    @Test
    fun `dropping at the end goes one gap after the previous item`() {
        assertEquals(3000.0, PositionResolver.resolve(2000.0, null)!!, 0.0)
    }

    @Test
    fun `dropping at the start goes one gap before the next item`() {
        assertEquals(0.0, PositionResolver.resolve(null, 1000.0)!!, 0.0)
    }

    @Test
    fun `dropping between two items takes the midpoint`() {
        assertEquals(1500.0, PositionResolver.resolve(1000.0, 2000.0)!!, 0.0)
    }

    @Test
    fun `neighbours closer than epsilon ask for a renumber`() {
        assertNull(PositionResolver.resolve(1000.0, 1000.0005))
    }

    @Test
    fun `repeated midpoint insertion eventually requests a renumber instead of colliding`() {
        var previous = 0.0
        val next = 1000.0
        var iterations = 0
        while (true) {
            val position = PositionResolver.resolve(previous, next) ?: break
            assert(position > previous && position < next)
            previous = position
            iterations++
        }
        assert(iterations in 10..40) { "unexpected iteration count $iterations" }
    }
}
