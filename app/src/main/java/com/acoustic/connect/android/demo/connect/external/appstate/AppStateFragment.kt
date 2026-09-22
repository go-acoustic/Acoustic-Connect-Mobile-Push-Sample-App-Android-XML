/*
 * Copyright (C) 2026 Acoustic, L.P. All rights reserved.
 *
 * NOTICE: This file contains material that is confidential and proprietary to
 * Acoustic, L.P. and/or other developers. No license is granted under any
 * intellectual or industrial property rights of Acoustic, L.P. except as may
 * be provided in an agreement with Acoustic, L.P. Any unauthorized copying or
 * distribution of content from this file is prohibited.
 */
package com.acoustic.connect.android.demo.connect.external.appstate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.acoustic.connect.android.connectmod.Connect
import com.acoustic.connect.android.demo.connect.external.R
import com.acoustic.connect.android.demo.connect.external.analytics.SignalLog
import com.acoustic.connect.android.demo.connect.external.analytics.currentLogicalPageName
import com.acoustic.connect.android.demo.connect.external.analytics.currentSessionId
import com.acoustic.connect.android.connectmod.model.ConnectScreenviewType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App-state readout for the analytics audit — the XML counterpart of the Compose sample's
 * `AppStateScreen`.
 *
 * <p>Shows the SDK state that the app-state signals are meant to carry — session id, logical page,
 * enabled flag — next to the running [SignalLog], so a foreground, background or rotation can be
 * checked on the device rather than only in a collector payload.
 *
 * <p>Session id and logical page are read on resume rather than observed: the SDK exposes no change
 * notification for either.
 */
class AppStateFragment : Fragment() {

    private lateinit var sdkEnabledLabel: TextView
    private lateinit var sessionIdLabel: TextView
    private lateinit var logicalPageLabel: TextView
    private lateinit var signalLogHeader: TextView
    private lateinit var signalLogContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_app_state, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Connect.logScreenLayout(requireActivity(), SCREEN_NAME)
        val accepted = Connect.logScreenview(requireActivity(), SCREEN_NAME, ConnectScreenviewType.LOAD)
        SignalLog.record("screenviewLoad", SCREEN_NAME, accepted)

        sdkEnabledLabel = view.findViewById(R.id.tv_sdk_enabled)
        sessionIdLabel = view.findViewById(R.id.tv_session_id)
        logicalPageLabel = view.findViewById(R.id.tv_logical_page)
        signalLogHeader = view.findViewById(R.id.tv_signal_log_header)
        signalLogContainer = view.findViewById(R.id.signal_log_container)

        view.findViewById<Button>(R.id.btn_start_session).setOnClickListener {
            val started = Connect.startSession()
            SignalLog.record(EVENT_SESSION_START, "Connect.startSession()", started)
            renderSdkState()
        }
        view.findViewById<Button>(R.id.btn_clear_signals).setOnClickListener { SignalLog.clear() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SignalLog.entries.collect { entries -> renderSignals(entries) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderSdkState()
    }

    override fun onDestroyView() {
        val accepted = Connect.logScreenview(requireActivity(), SCREEN_NAME, ConnectScreenviewType.UNLOAD)
        SignalLog.record("screenviewUnload", SCREEN_NAME, accepted)
        super.onDestroyView()
    }

    private fun renderSdkState() {
        sdkEnabledLabel.text = getString(R.string.app_state_sdk_enabled, Connect.isEnabled().toString())
        sessionIdLabel.text = getString(R.string.app_state_session_id, currentSessionId().ifEmpty { EMPTY })
        logicalPageLabel.text =
            getString(R.string.app_state_logical_page, currentLogicalPageName().ifEmpty { EMPTY })
    }

    private fun renderSignals(entries: List<SignalLog.Entry>) {
        signalLogHeader.text = getString(R.string.app_state_signal_log_header, entries.size)
        signalLogContainer.removeAllViews()

        if (entries.isEmpty()) {
            signalLogContainer.addView(signalRow(getString(R.string.app_state_signal_log_empty)))
            return
        }
        entries.forEach { entry ->
            val line = buildString {
                append(formatTime(entry.timestampMillis))
                append("  ")
                append(entry.name)
                append("  ")
                append(outcome(entry.accepted))
                if (entry.detail.isNotEmpty()) {
                    append('\n')
                    append(entry.detail)
                }
            }
            signalLogContainer.addView(signalRow(line))
        }
    }

    private fun signalRow(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        setPadding(0, ROW_PADDING_PX, 0, ROW_PADDING_PX)
    }

    private fun outcome(accepted: Boolean?): String = when (accepted) {
        true -> getString(R.string.app_state_accepted)
        false -> getString(R.string.app_state_rejected)
        null -> getString(R.string.app_state_not_sent)
    }

    private fun formatTime(timestampMillis: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestampMillis))

    companion object {
        /** Logical page name — identical to the Compose sample app's route. */
        private const val SCREEN_NAME = "app_state_screen"
        private const val EVENT_SESSION_START = "sessionStart"
        private const val ROW_PADDING_PX = 12
        private const val EMPTY = "—"
    }
}
