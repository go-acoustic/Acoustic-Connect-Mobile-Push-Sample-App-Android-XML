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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.acoustic.connect.android.connectmod.Connect
import com.acoustic.connect.android.demo.connect.external.R
import com.acoustic.connect.android.demo.connect.external.analytics.SignalLog
import com.tl.uic.model.ScreenviewType
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private val viewModel: NotificationViewModel by activityViewModels()

    private lateinit var statusDot: View
    private lateinit var tvAuthStatus: TextView
    private lateinit var btnRequestAuthorization: Button
    private lateinit var tvStatusMessage: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_notification, container, false)

    override fun onResume() {
        super.onResume()
        Connect.logScreenLayout(requireActivity(), SCREEN_NAME)
        // Return values recorded rather than dropped: the audit needs to know whether the SDK
        // accepted each screenview, not just that the call was made.
        SignalLog.record(
            "screenviewLoad",
            SCREEN_NAME,
            Connect.logScreenview(requireActivity(), SCREEN_NAME, ScreenviewType.LOAD),
        )
        viewModel.refreshAuthorization()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusDot = view.findViewById(R.id.iv_notification_auth_status_dot)
        tvAuthStatus = view.findViewById(R.id.tv_notification_auth_status)
        btnRequestAuthorization = view.findViewById(R.id.btn_request_authorization)
        tvStatusMessage = view.findViewById(R.id.tv_notification_status_message)

        btnRequestAuthorization.setOnClickListener {
            Connect.logCustomEvent("RequestNotificationPermission")
            Connect.push.requestNotificationPermission(requireActivity())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onDestroyView() {
        // Pairs the LOAD logged in onViewCreated, so each screen entry/exit is a matched signal pair.
        SignalLog.record(
            "screenviewUnload",
            SCREEN_NAME,
            Connect.logScreenview(requireActivity(), SCREEN_NAME, ScreenviewType.UNLOAD),
        )
        super.onDestroyView()
    }

    companion object {
        private const val SCREEN_NAME = "notification_screen"
    }

    private fun render(state: NotificationUiState) {
        val authorized = state.isNotificationAuthorized

        val dotColor = if (authorized) {
            ContextCompat.getColor(requireContext(), R.color.acoustic_green)
        } else {
            ContextCompat.getColor(requireContext(), R.color.gray_dot)
        }
        statusDot.background.setTint(dotColor)

        tvAuthStatus.text = if (authorized) {
            getString(R.string.status_authorized)
        } else {
            getString(R.string.status_not_authorized)
        }

        btnRequestAuthorization.isEnabled = state.isSdkEnabled

        val message = state.notificationStatusMessage
        if (message.isEmpty()) {
            tvStatusMessage.visibility = View.GONE
        } else {
            tvStatusMessage.visibility = View.VISIBLE
            tvStatusMessage.text = message
            val color = when {
                message.startsWith("Error") || message.contains("disabled") ->
                    ContextCompat.getColor(requireContext(), R.color.error_red)
                else ->
                    ContextCompat.getColor(requireContext(), R.color.acoustic_green)
            }
            tvStatusMessage.setTextColor(color)
        }
    }
}