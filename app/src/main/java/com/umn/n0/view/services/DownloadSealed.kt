package com.umn.n0.view.services

import java.io.Serializable
import java.util.concurrent.atomic.AtomicLong

sealed interface DownloadSealed : Serializable {

    data class Loading(
        private val progress: AtomicLong,
        private val length: Long,
    ) : DownloadSealed {

        fun getProgress() = progress.get()

        fun getLength() = length
    }

    data class Error(
        val err: Throwable
    ) : DownloadSealed
}