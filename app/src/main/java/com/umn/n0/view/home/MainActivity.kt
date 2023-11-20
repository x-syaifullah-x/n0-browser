package com.umn.n0.view.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.umn.n0.BuildConfig
import com.umn.n0.R
import com.umn.n0.databinding.ActivityMainBinding
import com.umn.n0.databinding.DialogDownloadBinding
import com.umn.n0.view.constant.AppBuild
import com.umn.n0.view.services.DownloadSealed
import com.umn.n0.view.services.DownloadService
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {

        private val PERMISSIONS = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private const val NO_BROWSER_PACKAGE_NAME = "com.umn.n0.browser"

        private const val PREF_NAME = "N0_"
        private const val PREF_KEY_PATH_DOWNLOAD = "key_path_download"
    }

    private val downloadDir by lazy {
        when (packageName) {
            BuildConfig.PACKAGE_NAME_N0_BROWSER -> "N0Browser"
            BuildConfig.PACKAGE_NAME_N0_RENDER -> "PS0Render"
            else -> throw IllegalArgumentException("please check package name in gradle")
        }
    }

    private var _downloadUrl: String? = null

    private val a = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val path = it.data?.getStringExtra(SelectFolderActivity.DATA_EXTRA)
        if (path != null) {
            val downloadUrl = _downloadUrl ?: return@registerForActivityResult
            val fileName =
                downloadUrl.toUri().lastPathSegment ?: return@registerForActivityResult
            val uri = AppBuild.Provider.getUriForFile(
                this, File(File(path), fileName)
            )
            getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_PATH_DOWNLOAD, path)
                .commit()
            startDownload(downloadUrl, uri)
        }
    }

    private val activityResultLauncherMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var isGranted = false
            for (permission in PERMISSIONS) {
                isGranted = permissions[permission] ?: false
            }
            if (isGranted) {
                val i = Intent(this, SelectFolderActivity::class.java)
                a.launch(i)
            }
        }

    private val activityResultLauncherMultiplePermissionss =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var isGranted = false
            for (permission in PERMISSIONS) {
                isGranted = permissions[permission] ?: false
            }
            if (isGranted) {
                val fileDownloadDir = File(Environment.getExternalStorageDirectory(), downloadDir)
                if (!fileDownloadDir.exists()) fileDownloadDir.mkdir()
                val downloadUrl = _downloadUrl ?: return@registerForActivityResult
                val fileName =
                    downloadUrl.toUri().lastPathSegment ?: return@registerForActivityResult
                val uri = AppBuild.Provider.getUriForFile(
                    this, File(fileDownloadDir, fileName)
                )
                startDownload(downloadUrl, uri)
            }
        }

    private val activityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var onBackPressedTimeMillis = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activityMainBinding.root)

        val cursor = activityMainBinding.cursor
        cursor.bringToFront()
        cursor.setOnClickListener { _ -> }
        cursor.setOnFocusChangeListener { v, _ ->
            if (v.hasFocus()) {
                v.setBackgroundResource(R.drawable.ic_cursor)
            } else {
                v.background = null
            }
        }
        val webView = activityMainBinding.webView
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

                KeyEvent.KEYCODE_DPAD_UP -> {
                    moveCursor(keyCode)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    moveCursor(keyCode)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    moveCursor(keyCode)
                    return@setOnKeyListener true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveCursor(keyCode)
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }

        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.javaScriptEnabled = true
        webView.setDownloadListener { url: String, _: String, _: String, _: String, _: Long ->
            if (url.contains(".bin")) {
                val name = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .getString(PREF_KEY_PATH_DOWNLOAD, "${Environment.getExternalStorageDirectory().path}/$downloadDir")
                _downloadUrl = url
                AlertDialog.Builder(this)
                    .setTitle("Download location")
                    .setMessage("\nDownloads $name")
                    .setPositiveButton("Ok") { dialog, _ ->
                        activityResultLauncherMultiplePermissionss.launch(PERMISSIONS)
                        dialog.dismiss()
                    }.setNegativeButton("Change") { dialog, _ ->
                        activityResultLauncherMultiplePermissions.launch(PERMISSIONS)
                        dialog.cancel()
                    }.show()
            } else {
                val downloadDirectory = File(cacheDir, downloadDir)
                if (!downloadDirectory.exists()) {
                    downloadDirectory.mkdirs()
                }
                val file = File(downloadDirectory, url.toUri().lastPathSegment.toString())
                val des = AppBuild.Provider.getUriForFile(this@MainActivity, file)
                startDownload(url, des)
            }
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
        val homePage = if (packageName == NO_BROWSER_PACKAGE_NAME) {
            "https://n0render.com/dc"
        } else {
            "https://n0render.com/retro"
        }
        webView.loadUrl(homePage)
    }

    private fun startDownload(url: String, des: Uri) {
        val i = Intent(this, DownloadService::class.java)
        i.putExtra(DownloadService.DATA_EXTRA_URL_STRING, url)
        i.data = des
        registerReceiver(
            object : BroadcastReceiver() {
                @SuppressLint("InflateParams")
                val view =
                    LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_download, null)
                val dialogDownloadBinding = DialogDownloadBinding.bind(view)
                val dialog =
                    AlertDialog.Builder(this@MainActivity).setCancelable(false).setView(view)
                        .create()

                init {
                    dialogDownloadBinding.textViewName.text = url.toUri().lastPathSegment
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
                            val progressHorizontal = dialogDownloadBinding.progressHorizontal
                            val progressOfPercent =
                                (downloadSealed.progress.toLong() * 100) / downloadSealed.contentLength.toLong()
                            progressHorizontal.isIndeterminate = (progressOfPercent < 0)
                            progressHorizontal.progress = progressOfPercent.toInt()
                            val progressOfMB = toMegaByteString(downloadSealed.progress.toLong())
                            val contentLengthOfMB =
                                toMegaByteString(downloadSealed.contentLength.toLong())
                            val a = "$progressOfMB / $contentLengthOfMB"
                            dialogDownloadBinding.textProgress.text = a
                            if (progressOfMB == contentLengthOfMB) {
                                dialog.cancel()
                                unregisterReceiver(this)
                                val uri = i.data
                                val isApk = uri?.lastPathSegment?.contains(".apk") ?: false
                                if (isApk) {
                                    installApk(this@MainActivity, uri)
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

    private fun installApk(context: Context, apkFile: Uri?) {
        val install = Intent(Intent.ACTION_VIEW)
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        install.setDataAndType(
            apkFile, "application/vnd.android.package-archive"
        );
        context.startActivity(install)
    }

    private fun moveCursor(keyCode: Int) {
        val cursorSpeed = 30F
        val deltas = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> floatArrayOf(0F, -cursorSpeed)
            KeyEvent.KEYCODE_DPAD_RIGHT -> floatArrayOf(cursorSpeed, 0F)
            KeyEvent.KEYCODE_DPAD_DOWN -> floatArrayOf(0F, cursorSpeed)
            KeyEvent.KEYCODE_DPAD_LEFT -> floatArrayOf(-cursorSpeed, 0F)
            else -> throw IllegalAccessException("keyCode supported: [${KeyEvent.KEYCODE_DPAD_UP}, ${KeyEvent.KEYCODE_DPAD_RIGHT}, ${KeyEvent.KEYCODE_DPAD_DOWN}, ${KeyEvent.KEYCODE_DPAD_LEFT}]")
        }

        val cursor = activityMainBinding.cursor
        val x = cursor.x + deltas[0]
        val y = cursor.y + deltas[1]
        val isLeft = x < 0 - cursorSpeed
        if (isLeft) {
            return
        }
        val isRight = x > (activityMainBinding.root.width - cursorSpeed)
        if (isRight) {
            return
        }

        val isTop = y < 0 - cursorSpeed
        if (isTop) {
            val webView = activityMainBinding.webView
            webView.scrollTo(0, webView.scrollY - webView.height / 4)
            return
        }
        val isBottom = y >= (activityMainBinding.root.height - cursorSpeed)
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
        )
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
    private fun toMegaByteString(value: Long): String {
        return String.format(Locale.getDefault(), "%.2f MB", toMegaByte(value))
    }

    override fun onBackPressed() {
        val currentTimeMillis = System.currentTimeMillis()
        val delay = currentTimeMillis - onBackPressedTimeMillis
        val isTwoClick = delay < 1000
        if (isTwoClick) {
            if (activityMainBinding.webView.canGoBack()) {
                activityMainBinding.webView.goBack()
            } else {
                super.onBackPressed()
            }
        } else {
            onBackPressedTimeMillis = currentTimeMillis
            val cursor = activityMainBinding.cursor
            if (!cursor.isFocused) {
                cursor.requestFocusFromTouch()
            } else {
                cursor.clearFocus()
            }
        }
    }
}