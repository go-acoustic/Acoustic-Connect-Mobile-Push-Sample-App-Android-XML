/*
 * Copyright (C) 2026 Acoustic, L.P. All rights reserved.
 *
 * NOTICE: This file contains material that is confidential and proprietary to
 * Acoustic, L.P. and/or other developers. No license is granted under any
 * intellectual or industrial property rights of Acoustic, L.P. except as may
 * be provided in an agreement with Acoustic, L.P. Any unauthorized copying or
 * distribution of content from this file is prohibited.
 */
package com.acoustic.connect.android.demo.connect.external.notification

import android.app.Application
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import com.acoustic.connect.android.connectmod.Connect
import com.acoustic.connect.android.connectmod.push.model.Provider
import com.acoustic.connect.android.connectmod.push.model.Token
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isNotificationAuthorized = areNotificationsPermitted()) }
        if (Connect.isEnabled()) {
            fetchToken()
        }
    }

    fun onNotificationPermissionResult(isGranted: Boolean) {
        Log.d(TAG, "POST_NOTIFICATIONS permission granted: $isGranted")
        if (isGranted) {
            try {
                Connect.push.turnOnPush()
                fetchToken()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to turn on push: ${e.message}")
                _uiState.update { it.copy(notificationStatusMessage = "Error: failed to enable push notifications") }
            }
        } else {
            _uiState.update {
                it.copy(
                    isNotificationAuthorized = false,
                    notificationStatusMessage = "Error: push notifications permission denied",
                )
            }
        }
    }

    private fun areNotificationsPermitted(): Boolean =
        NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()

    fun setToken(token: Token) {
        val tokenString = token.token
        if (tokenString.isBlank()) return
        _uiState.update {
            it.copy(
                pushToken = tokenString,
                pushProvider = token.provider,
                isNotificationAuthorized = areNotificationsPermitted(),
                tokenStatus = TokenStatus.Success(tokenString),
                notificationStatusMessage = "",
            )
        }
    }

    private fun fetchToken() {
        _uiState.update { it.copy(tokenStatus = TokenStatus.Loading) }
        Connect.push.getToken()
            .addOnSuccessListener { token ->
                val tokenString = token.token
                val provider = token.provider
                Log.d(TAG, "Token received: $tokenString")
                _uiState.update {
                    it.copy(
                        isNotificationAuthorized = areNotificationsPermitted(),
                        tokenStatus = TokenStatus.Success(tokenString),
                        notificationStatusMessage = "",
                        pushToken = tokenString,
                        pushProvider = provider,
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get token: ${e.message}")
                _uiState.update {
                    it.copy(
                        tokenStatus = TokenStatus.Failure(
                            e.message ?: "Unknown error"
                        )
                    )
                }
            }
    }

    companion object {
        private const val TAG = "ConnectDemo"
    }
}

data class NotificationUiState(
    val tokenStatus: TokenStatus = TokenStatus.Idle,
    val isNotificationAuthorized: Boolean = false,
    val notificationStatusMessage: String = "",
    val pushToken: String = "",
    val pushProvider: Provider? = null,
    val statusMessage: String = "Connect SDK is enabled automatically",
    val isSdkEnabled: Boolean = true,
)

sealed class TokenStatus {
    object Idle : TokenStatus()
    object Loading : TokenStatus()
    data class Success(val token: String) : TokenStatus()
    data class Failure(val message: String) : TokenStatus()
}
