/*
 * Copyright (C) 2026 Acoustic, L.P. All rights reserved.
 *
 * NOTICE: This file contains material that is confidential and proprietary to
 * Acoustic, L.P. and/or other developers. No license is granted under any
 * intellectual or industrial property rights of Acoustic, L.P. except as may
 * be provided in an agreement with Acoustic, L.P. Any unauthorized copying or
 * distribution of content from this file is prohibited.
 */
package com.acoustic.connect.android.demo.connect.external.gestures

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.acoustic.connect.android.connectmod.Connect
import com.acoustic.connect.android.demo.connect.external.R
import com.tl.uic.model.ScreenviewType

/**
 * The targets are plain views with no analytics calls of their own — the SDK's window-wide
 * [Connect.dispatchTouchEvent] hook in `MainActivity` is the only thing that can report them, which is
 * what makes a missing signal attributable to the SDK rather than to the app.
 */
class GesturesFragment : Fragment() {

    private lateinit var lastGestureLabel: TextView
    private var zoom = 1f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_gestures, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Connect.logScreenLayout(requireActivity(), SCREEN_NAME)
        Connect.logScreenview(requireActivity(), SCREEN_NAME, ScreenviewType.LOAD)

        lastGestureLabel = view.findViewById(R.id.tv_gestures_last)
        populateScrollableRows(view.findViewById(R.id.gesture_row_container))
        wireLongPress(view.findViewById(R.id.gesture_long_press))
        wireDoubleTap(view.findViewById(R.id.gesture_double_tap))
        wirePinchZoom(view.findViewById(R.id.gesture_pinch_zoom))
    }

    override fun onDestroyView() {
        // Pairs the LOAD above. The Compose app emits UNLOAD from onDispose; same signal pair.
        Connect.logScreenview(requireActivity(), SCREEN_NAME, ScreenviewType.UNLOAD)
        super.onDestroyView()
    }

    /** Enough rows that a swipe produces real scrolling rather than a bounce. */
    private fun populateScrollableRows(container: LinearLayout) {
        for (index in 0 until LIST_ROWS) {
            val row = TextView(requireContext()).apply {
                text = getString(R.string.gestures_row, index + 1)
                setPadding(0, ROW_PADDING_PX, 0, ROW_PADDING_PX)
            }
            container.addView(row)
        }
    }

    private fun wireLongPress(target: TextView) {
        target.setOnClickListener { showGesture("tap") }
        target.setOnLongClickListener {
            showGesture("longPress")
            true
        }
    }

    private fun wireDoubleTap(target: TextView) {
        val detector = GestureDetector(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    showGesture("doubleTap")
                    return true
                }
            })
        target.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            // Returns false so the event continues to the activity's dispatchTouchEvent hook — the
            // SDK must still see it, otherwise this listener would hide the gesture from capture.
            false
        }
    }

    private fun wirePinchZoom(target: ImageView) {
        val detector = ScaleGestureDetector(requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    zoom = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    target.scaleX = zoom
                    target.scaleY = zoom
                    showGesture(if (detector.scaleFactor > 1f) "zoomIn" else "zoomOut")
                    return true
                }
            })
        target.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
    }

    private fun showGesture(name: String) {
        lastGestureLabel.text = getString(R.string.gestures_last, name)
    }

    companion object {
        /** Logical page name — identical to the Compose sample app's route. */
        private const val SCREEN_NAME = "gestures_screen"
        private const val LIST_ROWS = 40
        private const val ROW_PADDING_PX = 24
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 4f
    }
}
