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
import com.acoustic.connect.android.demo.connect.external.analytics.AppStateSignals

/**
 * Registers the process-level analytics observers.
 *
 * <p>The SDK itself is still initialised in [MainActivity.onCreate] — that is the integration path
 * this sample demonstrates. Only the app-state observers live here, because foreground/background
 * and orientation are process facts that no single activity or fragment can see.
 */
class ConnectDemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppStateSignals.install(this)
    }
}
