package com.shoop.app

import com.shoop.app.ble.HeartRateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateParserTest {

    @Test
    fun `uint8 heart rate without rr`() {
        // flags=0x00 -> uint8 HR, no extras
        val m = HeartRateParser.parse(byteArrayOf(0x00, 72))
        assertEquals(72, m?.heartRateBpm)
        assertEquals(0, m?.rrIntervalsMs?.size)
    }

    @Test
    fun `uint16 heart rate`() {
        // flags=0x01 -> uint16 HR LE
        val m = HeartRateParser.parse(byteArrayOf(0x01, 0x08, 0x01)) // 0x0108 = 264
        assertEquals(264, m?.heartRateBpm)
    }

    @Test
    fun `rr intervals converted from 1024ths to ms`() {
        // flags=0x10 -> uint8 HR + RR present; RR raw 1024 -> 1000 ms
        val m = HeartRateParser.parse(byteArrayOf(0x10, 60, 0x00, 0x04)) // 0x0400 = 1024
        assertEquals(60, m?.heartRateBpm)
        assertEquals(listOf(1000), m?.rrIntervalsMs)
    }

    @Test
    fun `multiple rr intervals`() {
        val m = HeartRateParser.parse(
            byteArrayOf(0x10, 60, 0x00, 0x04, 0x00, 0x02) // RR 1024 and 512
        )
        assertEquals(listOf(1000, 500), m?.rrIntervalsMs)
    }

    @Test
    fun `energy expended field skipped`() {
        // flags = uint8 HR + energy (0x08) + rr (0x10)
        val m = HeartRateParser.parse(byteArrayOf(0x18, 70, 0x34, 0x12, 0x00, 0x04))
        assertEquals(70, m?.heartRateBpm)
        assertEquals(listOf(1000), m?.rrIntervalsMs)
    }

    @Test
    fun `rejects empty and zero hr`() {
        assertNull(HeartRateParser.parse(byteArrayOf()))
        assertNull(HeartRateParser.parse(byteArrayOf(0x00, 0x00)))
    }

    @Test
    fun `rejects truncated uint16`() {
        assertNull(HeartRateParser.parse(byteArrayOf(0x01, 0x08)))
    }
}
