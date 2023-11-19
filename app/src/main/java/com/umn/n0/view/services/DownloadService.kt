package com.umn.n0.view.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


class DownloadService : Service() {

    companion object {

        const val DATA_EXTRA_URL_STRING = "DownloadService_DATA_EXTRA_URL_STRING"
    }

    inner class LocalBinder : Binder() {
        fun getServices() = this@DownloadService
    }

    private val connections: MutableMap<String, HttpURLConnection> = mutableMapOf()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val urlString = intent?.getStringExtra(DATA_EXTRA_URL_STRING)
            ?: return super.onStartCommand(intent, flags, startId)
        downloadStarted(
            urlString,
            intent.data
                ?: return super.onStartCommand(intent, flags, startId)
        )
        return START_STICKY
    }

    private fun downloadStarted(urlString: String, data: Uri) {

        if (connections[urlString] != null) return

        scope.launch(Dispatchers.IO) {
            val i = Intent(urlString)
            try {
                val url = URL(urlString)
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection() as HttpURLConnection
                }
                if (connection.responseCode in 200..300) {
                    connections[urlString] = connection
//                    val name: String = urlString.toUri().lastPathSegment
//                        ?: throw IllegalArgumentException()
//                    val contentDisposition = connection.getHeaderField("Content-Disposition")
//                        if (contentDisposition != null && contentDisposition.indexOf("=") !== -1) {
//                            contentDisposition.split("=")[1] //getting value after '='
//                        } else {
//                            urlString.toUri().lastPathSegment ?: "aa.apk"
//                        }

                    val inputStream = connection.inputStream
                    val buffersSize = 1024
                    val buffers = ByteArray(buffersSize)
                    val contentLength = connection.contentLength
                    var downloadProgress = 0
                    val outputStream = contentResolver.openOutputStream(data)
                    i.putExtra(
                        urlString, DownloadSealed.OnDownload(
                            progress = 0,
                            contentLength = contentLength,
                        )
                    )
                    sendBroadcast(i)
                    while (true) {
                        val readCount = withContext(Dispatchers.IO) {
                            inputStream.read(buffers, 0, buffers.size)
                        }
                        if (readCount != -1) {
                            val bytes =
                                if (readCount == buffersSize) buffers else buffers.copyOf(readCount)
                            downloadProgress += bytes.size
                            i.putExtra(
                                urlString, DownloadSealed.OnDownload(
                                    progress = downloadProgress,
                                    contentLength = contentLength,
                                )
                            )
                            sendBroadcast(i)
                            outputStream?.write(bytes)
                            outputStream?.flush()
                        } else {
                            break
                        }
                    }
                    outputStream?.close()
                } else {
                    throw Throwable(String(connection.errorStream.readBytes()))
                }
                connection.disconnect()
            } catch (e: Throwable) {
                e.printStackTrace()
                i.putExtra(urlString, DownloadSealed.OnError(err = e))
                sendBroadcast(i)
            } finally {
                try {
                    connections[urlString]?.disconnect()
                } finally {
                    connections.remove(urlString)
                    if (connections.isEmpty())
                        stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connections.forEach {
            try {
                it.value.disconnect()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                connections.remove(it.key)
            }
        }
    }
}