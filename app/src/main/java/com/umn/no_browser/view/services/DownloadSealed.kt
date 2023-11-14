package com.umn.no_browser.view.services

import java.io.Serializable

sealed interface DownloadSealed : Serializable {

    data class OnDownload(
        val progress: Int,
        val contentLength: Int,
    ) : DownloadSealed

    data class OnError(val err: Throwable) : DownloadSealed
}