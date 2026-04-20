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

import android.app.Application
import android.util.Log
import com.acoustic.connect.android.connectmod.Connect

class ConnectApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            initConnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application: ${e.message}", e)
        }
    }

    private fun initConnect() {
        Connect.init(this)
    }

    companion object {
        private const val TAG = "ConnectApplication"
    }
}
