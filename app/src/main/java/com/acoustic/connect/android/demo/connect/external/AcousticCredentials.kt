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

import android.content.Context
import java.util.Properties

object AcousticCredentials {
    private const val CONFIG_ASSET = "ConnectBasicConfig.properties"
    private const val KEY_APP_KEY = "AppKey"
    private const val KEY_POST_URL = "PostMessageUrl"

    var appKey: String = ""
        private set
    var collectorUrl: String = ""
        private set

    fun load(context: Context) {
        val props = Properties().apply {
            context.assets.open(CONFIG_ASSET).use { load(it) }
        }
        appKey = props.getProperty(KEY_APP_KEY).orEmpty()
        collectorUrl = props.getProperty(KEY_POST_URL).orEmpty()
    }
}
