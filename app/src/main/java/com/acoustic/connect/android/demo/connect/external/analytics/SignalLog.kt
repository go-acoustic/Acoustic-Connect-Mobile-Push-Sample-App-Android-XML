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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val MAX_ENTRIES = 50

/**
 * In-memory record of the analytics calls this app makes, newest first.
 *
 * <p>It exists so a lifecycle transition can be observed on the device instead of only in a
 * collector payload — the app-state signals in CA-144239 (foreground, background, orientation)
 * are awkward to verify otherwise, because the transition that produces them also takes the app
 * off screen.
 *
 * <p>Scope note: this records what the *app* asked the SDK to log and whether the SDK accepted the
 * call. It says nothing about what the SDK auto-instruments or what actually reaches the collector,
 * so it narrows the payload comparison rather than replacing it.
 */
object SignalLog {

    /**
     * @param accepted the SDK's return value for the call, or null where the call was not made
     *   (for example the SDK was not enabled yet).
     */
    data class Entry(
        val timestampMillis: Long,
        val name: String,
        val detail: String,
        val accepted: Boolean?,
    )

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun record(name: String, detail: String = "", accepted: Boolean? = null) {
        val entry = Entry(System.currentTimeMillis(), name, detail, accepted)
        _entries.update { current -> (listOf(entry) + current).take(MAX_ENTRIES) }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
