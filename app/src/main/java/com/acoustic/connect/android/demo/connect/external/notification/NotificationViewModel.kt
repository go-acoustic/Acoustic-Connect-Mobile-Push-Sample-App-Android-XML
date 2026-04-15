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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import com.acoustic.connect.android.connectmod.Connect
import com.acoustic.connect.android.connectmod.push.ConnectPushConfig
import com.acoustic.connect.android.connectmod.push.constants.ConnectConstants
import com.acoustic.connect.android.connectmod.push.model.Provider
import com.acoustic.connect.android.connectmod.push.model.Token
import com.acoustic.connect.android.demo.connect.external.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadSavedCredentials()
        // OPEN_APP push path: NotificationActionActivity calls Connect.enable(appKey, url)
        // WITHOUT pushConfig before MainActivity starts. ConnectWrapper sees isEnabled()=true
        // and skips its enable() block, so ConnectPush is never initialized and onTokenReady
        // never fires. Re-calling enable() here with a full ConnectPushConfig is safe —
        // the underlying SDK is idempotent — and initializes ConnectPush so that onTokenReady
        // fires with the token cached from the previous session, restoring UI state.
        // Normal autoLaunch/manualLaunch: SDK is not enabled yet at this point, so this
        // branch is a no-op — onTokenReady drives setToken() via ConnectPushConfig instead.
        if (Connect.isEnabled()) {
            val state = _uiState.value
            try {
                Connect.enable(
                    appKey = state.appKey,
                    postMessageUrl = state.collectorUrl,
                    pushConfig = ConnectPushConfig(
                        application = getApplication(),
                        iconRes = R.drawable.ic_notification,
                        strictProvider = null,
                        onTokenReady = { token -> setToken(token) },
                        onFailure = { e ->
                            // HMS reports an error when turnOnPush() is called but push is
                            // already enabled. Fetch the token directly as a fallback.
                            Connect.push.getToken()
                                .addOnSuccessListener { token -> setToken(token) }
                                .addOnFailureListener {
                                    Log.e(TAG, "Push re-init after OPEN_APP: ${e.message}")
                                    _uiState.update {
                                        it.copy(notificationStatusMessage = "Error: failed to enable push notifications")
                                    }
                                }
                        },
                        onPermissionResult = { isGranted -> onNotificationPermissionResult(isGranted) },
                    )
                )
                _uiState.update {
                    it.copy(
                        isSdkEnabled = true,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error re-initializing Connect push after OPEN_APP", e)
            }
        }
    }

    private fun loadSavedCredentials() {
        val prefs = getApplication<Application>()
            .getSharedPreferences(ConnectConstants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val savedAppKey = prefs.getString(ConnectConstants.CLIENT_APP_ID_KEY, null)
        val savedCollectorUrl = prefs.getString(ConnectConstants.COLLECTOR_URL_KEY, null)
        if (savedAppKey != null || savedCollectorUrl != null) {
            _uiState.update {
                it.copy(
                    appKey = savedAppKey ?: it.appKey,
                    collectorUrl = savedCollectorUrl ?: it.collectorUrl,
                )
            }
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

    fun onAppKeyChanged(value: String) {
        _uiState.update { it.copy(appKey = value) }
    }

    fun onCollectorUrlChanged(value: String) {
        _uiState.update { it.copy(collectorUrl = value) }
    }

    fun onEnableSdk() {
        val state = _uiState.value
        val clientId = state.appKey.trim()
        val collectorUrl = state.collectorUrl.trim()

        if (clientId.isEmpty() || collectorUrl.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Client ID and Collector URL cannot be empty") }
            return
        }

        val prefs = getApplication<Application>()
            .getSharedPreferences(ConnectConstants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString(ConnectConstants.CLIENT_APP_ID_KEY, clientId)
            .putString(ConnectConstants.COLLECTOR_URL_KEY, collectorUrl)
            .apply()

        try {
            Connect.enable(
                appKey = clientId,
                postMessageUrl = collectorUrl,
                pushConfig = ConnectPushConfig(
                    application = getApplication(),
                    iconRes = R.drawable.ic_notification,
                    strictProvider = null,
                    onTokenReady = { fetchToken() },
                    onFailure = { exception ->
                        Log.e(TAG, "ConnectPush initialization failed: ${exception.message}")
                        _uiState.update { s -> s.copy(notificationStatusMessage = "Error: failed to enable push notifications") }
                    },
                    onPermissionResult = { isGranted -> onNotificationPermissionResult(isGranted) },
                )
            )
            _uiState.update {
                it.copy(
                    statusMessage = "Credentials are applied",
                    isSdkEnabled = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Connect parameters", e)
        }
    }

    fun onDisableSdk() {
        Connect.disable()
        _uiState.update {
            it.copy(
                isSdkEnabled = false,
                statusMessage = "Connect SDK is disabled",
                isNotificationAuthorized = false,
                notificationStatusMessage = "Push notifications are disabled",
                pushToken = "",
                pushProvider = null,
                tokenStatus = TokenStatus.Idle,
            )
        }
    }

    fun onCopyToken() {
        val token = _uiState.value.pushToken
        if (token.isBlank()) return
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("push_token", token))
    }

    fun onFlushMessages() {
        Connect.flushQueues()
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
                val tokenString = token?.token ?: ""
                val provider = token?.provider
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
                Log.e(TAG, "Failed to get token: ${e?.message}")
                _uiState.update {
                    it.copy(
                        tokenStatus = TokenStatus.Failure(
                            e?.message ?: "Unknown error"
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
    val appKey: String = "68664f44ca814272b363bcb8ccd50805",
    val collectorUrl: String = "https://collector-eaoc.qa.goacoustic.com/collector/collectorPost",
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
