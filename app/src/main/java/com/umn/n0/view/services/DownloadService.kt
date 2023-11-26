package com.umn.n0.view.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

class DownloadService : Service() {

    companion object {

        const val DATA_EXTRA_URL_STRING = "DownloadService_DATA_EXTRA_URL_STRING"
        const val DATA_EXTRA_DESTINATION_STRING = "DownloadService_DATA_EXTRA_DESTINATION_STRING"
    }

    inner class Binder : android.os.Binder() {

        fun getService() = this@DownloadService
    }

    private val connections: MutableMap<String, HttpURLConnection> = mutableMapOf()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val binder = Binder()

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

    private fun downloadStarted(urlString: String, destination: Uri) {

        if (connections[urlString] != null) return

        scope.launch(Dispatchers.IO) {
            val i = Intent(urlString)
            try {
                val url = URL(urlString)
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection() as HttpURLConnection
                }
                if (connection.responseCode in 200..299) {
                    connections[urlString] = connection
                    val inputStream = connection.inputStream
                    val buffersSize = 10 * (1024 * 1024) // 10MB
                    val buffers = ByteArray(buffersSize)
                    val contentLength = connection.contentLength.toLong()
                    val outputStream =
                        BufferedOutputStream(contentResolver.openOutputStream(destination))
                    val loadingProgress = AtomicLong(0)
                    val loading = DownloadSealed.Loading(
                        progress = loadingProgress, length = contentLength
                    )
                    i.putExtra(urlString, loading)
                    sendBroadcast(i)
                    while (true) {
                        val readCount = withContext(Dispatchers.IO) {
                            inputStream.read(buffers, 0, buffers.size)
                        }
                        if (readCount != -1) {
                            val bytes =
                                if (readCount == buffers.size)
                                    buffers
                                else
                                    buffers.copyOf(readCount)
                            outputStream.write(bytes)
                            outputStream.flush()
                            loadingProgress.set(loadingProgress.get() + bytes.size)
                            i.putExtra(urlString, loading)
                            sendBroadcast(i)
                        } else {
                            break
                        }
                    }
                    outputStream.close()
                } else {
                    throw Throwable(String(connection.errorStream.readBytes()))
                }
                connection.disconnect()
            } catch (e: Throwable) {
                e.printStackTrace()
                i.putExtra(urlString, DownloadSealed.Error(err = e))
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