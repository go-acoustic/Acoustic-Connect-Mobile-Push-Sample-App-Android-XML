/*
 * Copyright (C) 2026 Acoustic, L.P. All rights reserved.
 *
 * NOTICE: This file contains material that is confidential and proprietary to
 * Acoustic, L.P. and/or other developers. No license is granted under any
 * intellectual or industrial property rights of Acoustic, L.P. except as may
 * be provided in an agreement with Acoustic, L.P. Any unauthorized copying or
 * distribution of content from this file is prohibited.
 */
package com.acoustic.connect.android.demo.connect.external

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.acoustic.connect.android.connectmod.Connect
import com.acoustic.connect.android.connectmod.push.ConnectPushConfig
import com.acoustic.connect.android.connectmod.push.constants.ConnectConstants
import com.acoustic.connect.android.connectmod.push.core.MobileServiceType
import com.acoustic.connect.android.demo.connect.external.notification.NotificationViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val viewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Connect.init(application)
        Connect.enable(
            appKey = AcousticCredentials.APP_KEY, // YOUR_APP_KEY
            postMessageUrl = AcousticCredentials.COLLECTOR_URL, // YOUR_COLLECTOR_URL
            pushConfig = ConnectPushConfig(
                application = application,
                iconRes = R.drawable.ic_notification,
                onFailure = { exception ->
                    Log.e(TAG, "ConnectPush initialization failed: ${exception.message}")
                },
                onTokenReady = {
                    token -> viewModel.setToken(token)
                    Log.d("Token", "Token: "+token.token)
                },
                onPermissionResult = { isGranted ->
                    viewModel.onNotificationPermissionResult(isGranted)
                },
            ),
        )

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
    }

    companion object {
        private const val TAG = "ConnectDemo"
    }

    override fun dispatchTouchEvent(e: MotionEvent?): Boolean {
        Connect.dispatchTouchEvent(this, e)
        return super.dispatchTouchEvent(e)
    }
}
