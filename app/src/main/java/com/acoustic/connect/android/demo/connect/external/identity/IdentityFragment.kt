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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.acoustic.connect.android.connectmod.Connect
import com.acoustic.connect.android.demo.connect.external.R
import com.google.android.material.textfield.TextInputEditText
import com.tl.uic.model.ScreenviewType
import kotlinx.coroutines.launch

class IdentityFragment : Fragment() {

    private val viewModel: IdentityViewModel by viewModels()

    private lateinit var etIdentifierName: TextInputEditText
    private lateinit var etIdentifierValue: TextInputEditText
    private lateinit var btnSendIdentitySignal: Button
    private lateinit var tvStatusMessage: TextView
    private lateinit var cardHistory: CardView
    private lateinit var llHistoryContainer: LinearLayout

    private var isUpdatingFromViewModel = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_identity, container, false)

    override fun onResume() {
        super.onResume()
        Connect.logScreenLayout(requireActivity(), SCREEN_NAME)
        Connect.logScreenview(requireActivity(), SCREEN_NAME, ScreenviewType.LOAD)
        viewModel.refreshSdkEnabled()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etIdentifierName = view.findViewById(R.id.et_identifier_name)
        etIdentifierValue = view.findViewById(R.id.et_identifier_value)
        btnSendIdentitySignal = view.findViewById(R.id.btn_send_identity_signal)
        tvStatusMessage = view.findViewById(R.id.tv_identity_status_message)
        cardHistory = view.findViewById(R.id.card_history)
        llHistoryContainer = view.findViewById(R.id.ll_history_container)

        etIdentifierName.doAfterTextChanged { text ->
            if (!isUpdatingFromViewModel) {
                viewModel.onIdentifierNameChanged(text?.toString() ?: "")
            }
        }

        etIdentifierValue.doAfterTextChanged { text ->
            if (!isUpdatingFromViewModel) {
                viewModel.onIdentifierValueChanged(text?.toString() ?: "")
            }
        }

        btnSendIdentitySignal.setOnClickListener {
            val data = HashMap<String?, String?>().apply {
                put("identifierName", etIdentifierName.text?.toString().orEmpty())
            }
            Connect.logCustomEvent("IdentitySignalSent", data)
            viewModel.onLogIdentity()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: IdentityUiState) {
        isUpdatingFromViewModel = true

        if (etIdentifierName.text?.toString() != state.identifierName) {
            etIdentifierName.setText(state.identifierName)
            etIdentifierName.setSelection(state.identifierName.length)
        }
        if (etIdentifierValue.text?.toString() != state.identifierValue) {
            etIdentifierValue.setText(state.identifierValue)
            etIdentifierValue.setSelection(state.identifierValue.length)
        }

        isUpdatingFromViewModel = false

        btnSendIdentitySignal.isEnabled = true

        val message = state.statusMessage
        if (message.isEmpty()) {
            tvStatusMessage.visibility = View.GONE
        } else {
            tvStatusMessage.visibility = View.VISIBLE
            tvStatusMessage.text = message
            val color = if (state.isSuccess) {
                ContextCompat.getColor(requireContext(), R.color.acoustic_green)
            } else {
                ContextCompat.getColor(requireContext(), R.color.error_red)
            }
            tvStatusMessage.setTextColor(color)
        }

        if (state.history.isEmpty()) {
            cardHistory.visibility = View.GONE
        } else {
            cardHistory.visibility = View.VISIBLE
            rebuildHistory(state.history)
        }
    }

    override fun onDestroyView() {
        // Pairs the LOAD logged in onViewCreated, so each screen entry/exit is a matched signal pair.
        Connect.logScreenview(requireActivity(), SCREEN_NAME, ScreenviewType.UNLOAD)
        super.onDestroyView()
    }

    companion object {
        private const val SCREEN_NAME = "identity_screen"
    }

    private fun rebuildHistory(history: List<IdentityHistoryEntry>) {
        llHistoryContainer.removeAllViews()
        history.forEachIndexed { index, entry ->
            val itemView = layoutInflater.inflate(
                R.layout.item_history_entry,
                llHistoryContainer,
                false
            )
            val divider = itemView.findViewById<View>(R.id.divider)
            divider.visibility = if (index > 0) View.VISIBLE else View.GONE

            itemView.findViewById<TextView>(R.id.tv_history_name).text = entry.name
            itemView.findViewById<TextView>(R.id.tv_history_value).text = entry.value
            itemView.setOnClickListener {
                Connect.logEvent(it, "OnHistoryItemClick")
                viewModel.onHistoryEntrySelected(entry)
            }

            llHistoryContainer.addView(itemView)
        }
    }
}
