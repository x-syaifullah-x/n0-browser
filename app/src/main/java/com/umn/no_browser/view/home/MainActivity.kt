package com.umn.no_browser.view.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.umn.no_browser.R
import com.umn.no_browser.databinding.ActivityMainBinding
import com.umn.no_browser.databinding.DialogDownloadBinding
import com.umn.no_browser.view.constant.AppBuild
import com.umn.no_browser.view.services.DownloadSealed
import com.umn.no_browser.view.services.DownloadService
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val activityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var timeOnBackPressed = 0L

    private val moveSpeed = 18F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activityMainBinding.root)

        val webView = activityMainBinding.webView

        val cursor = activityMainBinding.cursor
        cursor.bringToFront()
        cursor.requestFocus()
        cursor.setOnClickListener { _ -> }
        cursor.setOnFocusChangeListener { v, hasFocus ->
            if (v.hasFocus()) {
                v.setBackgroundResource(R.drawable.cursor)
            } else {
                v.background = null
            }
        }

        cursor.setOnKeyListener { v, keyCode, _ ->
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    return@setOnKeyListener false
                }


                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    webViewClicked(webView, v.x, v.y)
                    return@setOnKeyListener false
                }

                KeyEvent.KEYCODE_ENTER -> {
                    webViewClicked(webView, v.x, v.y)
                    return@setOnKeyListener false
                }

                KeyEvent.KEYCODE_PAGE_UP -> {
                    webView.requestFocus()
                    return@setOnKeyListener false
                }

                KeyEvent.KEYCODE_PAGE_DOWN -> {
                    webView.requestFocus()
                    return@setOnKeyListener false
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    moveCursor(-moveSpeed, 0F)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    moveCursor(moveSpeed, 0F)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    moveCursor(0F, -moveSpeed)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveCursor(0F, moveSpeed)
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }

        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.javaScriptEnabled = true
        webView.setDownloadListener { url: String, _: String, _: String, _: String, _: Long ->
            val i = Intent(this, DownloadService::class.java)
            i.putExtra(DownloadService.DATA_EXTRA_URL_STRING, url)
            val downloadDirectory = File(cacheDir, "NOBrowser")
            if (!downloadDirectory.exists()) {
                downloadDirectory.mkdirs()
            }
            val file = File(downloadDirectory, url.toUri().lastPathSegment.toString())
            i.data = AppBuild.Provider.getUriForFile(this@MainActivity, file)
            registerReceiver(
                object : BroadcastReceiver() {
                    val view = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.dialog_download, null)
                    val dialogDownloadBinding = DialogDownloadBinding.bind(view)
                    val dialog =
                        AlertDialog.Builder(this@MainActivity).setCancelable(false).setView(view)
                            .create()

                    init {
                        dialogDownloadBinding.textViewName.text = i.data?.lastPathSegment
                        dialog.show()
                        dialogDownloadBinding.buttonCancel.setOnClickListener {
                            stopService(Intent(it.context, DownloadService::class.java))
                            dialog.cancel()
                        }
                    }

                    override fun onReceive(context: Context?, intent: Intent?) {
                        val downloadSealed =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent?.getSerializableExtra(url, DownloadSealed::class.java)
                            } else {
                                @Suppress("DEPRECATION") intent?.getSerializableExtra(url) as? DownloadSealed
                            } ?: return
                        when (downloadSealed) {
                            is DownloadSealed.OnDownload -> {
                                val progressOfPercent =
                                    downloadSealed.progress * 100 / downloadSealed.contentLength
                                if (dialogDownloadBinding.progressHorizontal.isIndeterminate) {
                                    dialogDownloadBinding.progressHorizontal.isIndeterminate = false
                                }
                                dialogDownloadBinding.progressHorizontal.progress =
                                    progressOfPercent

                                val progressOfMB =
                                    toMegaByteString(downloadSealed.progress.toLong())
                                val contentLengthOfMB =
                                    toMegaByteString(downloadSealed.contentLength.toLong())
                                val a = "$progressOfMB / $contentLengthOfMB"
                                dialogDownloadBinding.textProgress.text = a
                                if (progressOfPercent == 100) {
                                    dialog.cancel()
                                    unregisterReceiver(this)
                                    val uri = i.data;
                                    val isApk = uri?.lastPathSegment?.contains(".apk") ?: false
                                    if (isApk) {
                                        val install = Intent(Intent.ACTION_VIEW)
                                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                        install.data = uri
                                        context?.startActivity(install)
                                    }
                                }
                            }

                            is DownloadSealed.OnError -> {
                                Toast.makeText(
                                    context, downloadSealed.err.message, Toast.LENGTH_LONG
                                ).show()
                                dialog.cancel()
                                unregisterReceiver(this)
                            }
                        }
                    }
                }, IntentFilter(url)
            )
            startService(i)
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                if (newProgress > 0) {
                    activityMainBinding.progressHorizontal.visibility = View.VISIBLE
                    activityMainBinding.progressHorizontal.progress = newProgress
                }
                if (newProgress == 100) {
                    activityMainBinding.progressHorizontal.visibility = View.GONE
                    if (activityMainBinding.progressBar.visibility == View.VISIBLE) {
                        activityMainBinding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return true
                if (url.scheme?.contains("http") == false) {
                    if (url.toString().contains("play.google.com")) {
                        val packageName = url.getQueryParameter("id")
                        if (!packageName.isNullOrBlank()) {
                            val i = Intent(Intent.ACTION_VIEW)
                            i.data = "market://details?id=$packageName".toUri()
                            startActivity(i)
                        }
                    }
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                activityMainBinding.progressHorizontal.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
        val homePage = "https://n0render.com/dc"
//        val homePage = "https://apkpure.com"
        webView.loadUrl(homePage)
    }

    private fun moveCursor(deltaX: Float, deltaY: Float) {
        val cursor = activityMainBinding.cursor
        val x = cursor.x + deltaX
        val y = cursor.y + deltaY
        val isLeft = x < 0 - moveSpeed
        if (isLeft) {
            return
        }
        val isRight = x > activityMainBinding.root.width - moveSpeed
        if (isRight) {
            return
        }

        val isTop = y < 0 - moveSpeed
        if (isTop) {
            val webView = activityMainBinding.webView
            webView.scrollTo(0, webView.scrollY - webView.height / 4)
            return
        }
        val isBottom = y >= activityMainBinding.root.height - moveSpeed
        if (isBottom) {
            val webView = activityMainBinding.webView
            webView.scrollTo(0, webView.scrollY + webView.height / 4)
            return
        }
        cursor.x = x
        cursor.y = y
    }

    private fun webViewClicked(webView: WebView, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        val properties = arrayOf(MotionEvent.PointerProperties())
        val pp1 = MotionEvent.PointerProperties()
        pp1.id = 0
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
        properties[0] = pp1
        val pointerCoords = arrayOf(MotionEvent.PointerCoords())
        val pc1 = MotionEvent.PointerCoords()
        pc1.x = x
        pc1.y = y
        pc1.pressure = 1F
        pc1.size = 1F
        pointerCoords[0] = pc1

        var motionEvent = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_DOWN,
            1,
            properties,
            pointerCoords,
            0,
            0,
            1F,
            1F,
            0,
            0,
            0,
            0,
        );
        webView.dispatchTouchEvent(motionEvent)
        motionEvent.recycle()

        motionEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_UP,
            1,
            properties,
            pointerCoords,
            0,
            0,
            1F,
            1F,
            0,
            0,
            0,
            0
        )
        webView.dispatchTouchEvent(motionEvent)
        motionEvent.recycle()
    }

    /**
     * example return *0.0*
     */
    private fun toMegaByte(value: Long): Double {
        return (value / (1024f * 1024f)).toDouble()
    }

    /**
     * example return *.00 MB
     */
    private fun toMegaByteString(value: Long): String? {
        return String.format(Locale.getDefault(), "%.2f MB", toMegaByte(value))
    }

    override fun onBackPressed() {
        val currentTimeMillis = System.currentTimeMillis()
        val delay = currentTimeMillis - timeOnBackPressed
        val isTwoClick = delay < 1000
        if (isTwoClick) {
            if (activityMainBinding.webView.canGoBack()) {
                activityMainBinding.webView.goBack()
            } else {
                super.onBackPressed()
            }
        } else {
            timeOnBackPressed = currentTimeMillis
            val cursor = activityMainBinding.cursor
            if (!cursor.isFocused) {
                cursor.requestFocus()
            } else {
                cursor.clearFocus()
            }
        }
    }
}