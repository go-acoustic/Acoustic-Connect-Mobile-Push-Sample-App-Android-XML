/*
 * Copyright (C) 2026 Acoustic, L.P. All rights reserved.
 *
 * NOTICE: This file contains material that is confidential and proprietary to
 * Acoustic, L.P. and/or other developers. No license is granted under any
 * intellectual or industrial property rights of Acoustic, L.P. except as may
 * be provided in an agreement with Acoustic, L.P. Any unauthorized copying or
 * distribution of content from this file is prohibited.
 */
package com.acoustic.connect.android.demo.connect.external.identity

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.acoustic.connect.android.connectmod.Connect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val PREFS_NAME = "identity_prefs"
private const val KEY_HISTORY = "identity_history"
private const val HISTORY_SEPARATOR = "|||"
private const val MAX_HISTORY = 5

data class IdentityHistoryEntry(val name: String, val value: String)

data class IdentityUiState(
    val identifierName: String = "",
    val identifierValue: String = "",
    val statusMessage: String = "",
    val isSuccess: Boolean = false,
    val isSdkEnabled: Boolean = false,
    val history: List<IdentityHistoryEntry> = emptyList(),
)

class IdentityViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        IdentityUiState(
            isSdkEnabled = Connect.isEnabled(),
            history = loadHistory(),
        )
    )
    val uiState: StateFlow<IdentityUiState> = _uiState.asStateFlow()

    fun onIdentifierNameChanged(value: String) {
        _uiState.update { it.copy(identifierName = value, statusMessage = "", isSuccess = false) }
    }

    fun onIdentifierValueChanged(value: String) {
        _uiState.update { it.copy(identifierValue = value, statusMessage = "", isSuccess = false) }
    }

    fun onLogIdentity() {
        val name = _uiState.value.identifierName.trim()
        val value = _uiState.value.identifierValue.trim()

        if (name.isEmpty() || value.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Identifier name and value cannot be empty", isSuccess = false) }
            return
        }

        val isEnabled = Connect.isEnabled()
        _uiState.update { it.copy(isSdkEnabled = isEnabled) }
        if (!isEnabled) {
            _uiState.update { it.copy(statusMessage = "Connect SDK is not ready yet — please try again", isSuccess = false) }
            return
        }

        val success = Connect.logIdentificationEvent(name, value)
        if (success) {
            val updated = buildUpdatedHistory(name, value)
            saveHistory(updated)
            _uiState.update {
                it.copy(
                    statusMessage = "Identity signal was sent",
                    isSuccess = true,
                    history = updated,
                )
            }
        } else {
            _uiState.update { it.copy(statusMessage = "Failed to send identity signal", isSuccess = false) }
        }
    }

    fun onHistoryEntrySelected(entry: IdentityHistoryEntry) {
        _uiState.update {
            it.copy(
                identifierName = entry.name,
                identifierValue = entry.value,
                statusMessage = "",
                isSuccess = false,
            )
        }
    }

    fun refreshSdkEnabled() {
        _uiState.update { it.copy(isSdkEnabled = Connect.isEnabled()) }
    }

    private fun buildUpdatedHistory(name: String, value: String): List<IdentityHistoryEntry> {
        val entry = IdentityHistoryEntry(name, value)
        return (listOf(entry) + _uiState.value.history).take(MAX_HISTORY)
    }

    private fun saveHistory(history: List<IdentityHistoryEntry>) {
        val serialised = history.joinToString("\n") { "${it.name}$HISTORY_SEPARATOR${it.value}" }
        prefs.edit().putString(KEY_HISTORY, serialised).apply()
    }

    private fun loadHistory(): List<IdentityHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return raw.lines()
            .filter { it.contains(HISTORY_SEPARATOR) }
            .map { line ->
                val parts = line.split(HISTORY_SEPARATOR, limit = 2)
                IdentityHistoryEntry(parts[0], parts[1])
            }
    }
}