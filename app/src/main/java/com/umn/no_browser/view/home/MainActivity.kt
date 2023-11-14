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

//    private val startActivityResult =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { ar ->
//            if (ar.resultCode == Activity.RESULT_OK) {
//                val data = ar.data ?: return@registerForActivityResult
//            }
//        }

    private val activityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var timeOnBackPressed = 0L

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

        fun moveCursor(deltaX: Float, deltaY: Float) {
            val x = cursor.x + deltaX
            val y = cursor.y + deltaY
            println("cursorX: $x || cursorY: $y")

            if (x < 0 - 20) {
                return
            }

            if (x > webView.width - 20) {
                return
            }

            if (y < 0 - 20) {
                return
            }
            if (y >= webView.height - 20) {
                return
            }
            cursor.x = x
            cursor.y = y
        }


        cursor.setOnKeyListener { v, keyCode, _ ->
            println(keyCode)

            fun enter() {
                val x = v.x
                val y = v.y
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
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    return@setOnKeyListener false
                }


                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    enter()
                    return@setOnKeyListener false
                }

                KeyEvent.KEYCODE_ENTER -> {
                    enter()
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
                    moveCursor(-18F, 0F)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    moveCursor(18F, 0F)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    moveCursor(0F, -18F)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveCursor(0F, 18F)
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener true
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
//                cursor.requestFocus()
            }
        }
//        activityMainBinding.textInputLayoutUrl.setEndIconOnClickListener { v ->
//            closeKeyboard(v)
//            val url = "${activityMainBinding.textInputEditTextUrl.text}"
//            webView.loadUrl(url)
//        }
//        activityMainBinding.textInputEditTextUrl.setOnKeyListener { v, keyCode, event ->
//            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
//                val url = "${activityMainBinding.textInputEditTextUrl.text}"
//                webView.loadUrl(url)
//                closeKeyboard(v)
//                return@setOnKeyListener true
//            }
//            return@setOnKeyListener false
//        }
//        activityMainBinding.iconSetting.setOnClickListener {
//            Toast.makeText(it.context, "Soon", Toast.LENGTH_LONG).show()
//        }
        val homePage = "https://n0render.com/dc"
//        val homePage = "https://apkpure.com"
//        val homePage = "https://google.com"
        webView.loadUrl(homePage)
//        activityMainBinding.btnHomePage.setOnClickListener {
//            webView.loadUrl(homePage)
//        }
    }

    /**
     * example return *0.0*
     */
    fun toMegaByte(value: Long): Double {
        return (value / (1024f * 1024f)).toDouble()
    }

    /**
     * example return *.00 MB
     */
    fun toMegaByteString(value: Long): String? {
        return String.format(Locale.getDefault(), "%.2f MB", toMegaByte(value))
    }

    private fun closeKeyboard(v: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
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