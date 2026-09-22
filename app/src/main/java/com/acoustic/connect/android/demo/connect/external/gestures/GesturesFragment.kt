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
import com.acoustic.connect.android.demo.connect.external.analytics.SignalLog
import com.acoustic.connect.android.connectmod.model.ConnectScreenviewType
import kotlin.math.abs

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
        // Return values recorded rather than dropped: the audit needs to know whether the SDK
        // accepted each screenview, not just that the call was made.
        SignalLog.record(
            "screenviewLoad",
            SCREEN_NAME,
            Connect.logScreenview(requireActivity(), SCREEN_NAME, ConnectScreenviewType.LOAD),
        )

        lastGestureLabel = view.findViewById(R.id.tv_gestures_last)
        populateScrollableRows(view.findViewById(R.id.gesture_row_container))
        wireLongPress(view.findViewById(R.id.gesture_long_press))
        wireDoubleTap(view.findViewById(R.id.gesture_double_tap))
        wireSwipe(view.findViewById(R.id.gesture_swipe))
        wirePinchZoom(view.findViewById(R.id.gesture_pinch_zoom))
    }

    override fun onDestroyView() {
        // Pairs the LOAD above. The Compose app emits UNLOAD from onDispose; same signal pair.
        SignalLog.record(
            "screenviewUnload",
            SCREEN_NAME,
            Connect.logScreenview(requireActivity(), SCREEN_NAME, ConnectScreenviewType.UNLOAD),
        )
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

    /**
     * Swipe from the travel between ACTION_DOWN and ACTION_UP.
     *
     * <p>A [GestureDetector] fling would only report a flick; the ticket's scope is a swipe in any
     * of the four directions, including a slow one, so the raw delta is used instead. Threshold and
     * direction naming match the Compose sample so the two apps' payloads line up.
     */
    private fun wireSwipe(target: TextView) {
        val thresholdPx = SWIPE_THRESHOLD_DP * resources.displayMetrics.density
        var downX = 0f
        var downY = 0f
        target.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    // The whole screen is a ScrollView, which intercepts vertical drags: without
                    // this the target is sent ACTION_CANCEL mid-swipe and only the horizontal
                    // directions are ever reported. Interception is view-tree only, so the
                    // activity's dispatchTouchEvent hook still sees every event either way.
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        swipeName(event.x - downX, event.y - downY, thresholdPx)?.let(::showGesture)
                    }
                }
            }
            // As with the other targets: false so the activity's dispatchTouchEvent hook still sees
            // the whole gesture. The view is clickable, so later events still reach this listener.
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
        /** Travel a drag must clear before it counts as a swipe rather than a sloppy tap. */
        private const val SWIPE_THRESHOLD_DP = 48f
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 4f
    }
}

/**
 * Direction label for a completed drag, or null when neither axis cleared the threshold — a short
 * drag that ends near where it began is not a swipe. The dominant axis wins, so a diagonal still
 * reports one direction. Mirrors `swipeName` in the Compose sample.
 */
internal fun swipeName(travelX: Float, travelY: Float, thresholdPx: Float): String? = when {
    abs(travelX) < thresholdPx && abs(travelY) < thresholdPx -> null
    abs(travelX) >= abs(travelY) -> if (travelX > 0) "swipeRight" else "swipeLeft"
    else -> if (travelY > 0) "swipeDown" else "swipeUp"
}
