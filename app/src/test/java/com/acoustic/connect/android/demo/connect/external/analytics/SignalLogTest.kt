/*
 * Copyright (C) 2026 Acoustic, L.P. All rights reserved.
 *
 * NOTICE: This file contains material that is confidential and proprietary to
 * Acoustic, L.P. and/or other developers. No license is granted under any
 * intellectual or industrial property rights of Acoustic, L.P. except as may
 * be provided in an agreement with Acoustic, L.P. Any unauthorized copying or
 * distribution of content from this file is prohibited.
 */
package com.acoustic.connect.android.demo.connect.external.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignalLogTest {

    /** [SignalLog] is a singleton, so each test starts from a known state. */
    @Before
    fun resetLog() {
        SignalLog.clear()
    }

    @Test
    fun `starts empty`() {
        assertTrue(SignalLog.entries.value.isEmpty())
    }

    @Test
    fun `orders entries newest first`() {
        SignalLog.record("first")
        SignalLog.record("second")
        SignalLog.record("third")

        assertEquals(
            listOf("third", "second", "first"),
            SignalLog.entries.value.map { it.name },
        )
    }

    @Test
    fun `keeps detail and outcome alongside the name`() {
        SignalLog.record("appBackground", "sessionId=abc", accepted = true)

        val entry = SignalLog.entries.value.single()
        assertEquals("appBackground", entry.name)
        assertEquals("sessionId=abc", entry.detail)
        assertEquals(true, entry.accepted)
    }

    @Test
    fun `records a not-sent call as null rather than as a failure`() {
        SignalLog.record("appForeground", "not sent — SDK not enabled yet", accepted = null)

        assertNull(SignalLog.entries.value.single().accepted)
    }

    @Test
    fun `caps the log and drops the oldest entries`() {
        repeat(60) { index -> SignalLog.record("event$index") }

        val entries = SignalLog.entries.value
        assertEquals(50, entries.size)
        assertEquals("event59", entries.first().name)
        assertEquals("event10", entries.last().name)
    }

    @Test
    fun `clear empties the log`() {
        SignalLog.record("appForeground")
        SignalLog.clear()

        assertTrue(SignalLog.entries.value.isEmpty())
    }
}
