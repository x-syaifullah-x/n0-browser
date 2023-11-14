package com.umn.no_browser.view.constant

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.umn.no_browser.BuildConfig
import java.io.File

object AppBuild {

    const val IS_DEBUG = BuildConfig.BUILD_TYPE == "debug"

    const val APPLICATION_ID = BuildConfig.APPLICATION_ID

    object Provider {

        const val AUTHORITIES = "$APPLICATION_ID.FILE_PROVIDER"

        fun getUriForFile(context: Context, file: File): Uri {
            return FileProvider.getUriForFile(
                context, AUTHORITIES, file
            )
        }
    }
}