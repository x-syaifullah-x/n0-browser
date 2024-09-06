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
import android.os.Message
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.monstertechno.adblocker.AdBlockerWebView
import com.monstertechno.adblocker.util.AdBlocker
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

    private val activityResultLauncherMultiplePermissionssasdf =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var isGranted = false
            for (permission in PERMISSIONS) {
                isGranted = permissions[permission] ?: false
            }
            if (isGranted) {
                val fileDownloadDir =
                    File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS)
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

//        activityMainBinding.etAddressBar.setOnKeyListener { v, keyCode, event ->
//            val isActionDone = event.getAction() == KeyEvent.ACTION_DOWN
//            val isActionEnter = keyCode == KeyEvent.KEYCODE_ENTER
//            if (isActionDone && isActionEnter) {
//                val addressBar = activityMainBinding.etAddressBar
//                activityMainBinding.webView.loadUrl(addressBar.text.toString())
//                addressBar.text.delete(0, addressBar.text.length)
//                return@setOnKeyListener true
//            }
//            return@setOnKeyListener false
//        }


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

        AdBlockerWebView.init(this).initializeWebView(webView)

        @SuppressLint("SetJavaScriptEnabled")
        webView.settings.javaScriptEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccess = true
        webView.settings.domStorageEnabled = true
        webView.settings.setSupportMultipleWindows(true)
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.getSettings().userAgentString = "${System.getProperty("http.agent")}N0Render"
        webView.settings.saveFormData = true;
        webView.settings.setEnableSmoothTransition(true);
        webView.setDownloadListener { url: String, _: String, _: String, _: String, _: Long ->
            if (url.contains(PATH_SELF_LOAD)) {
                _downloadUrl = url
                activityResultLauncherMultiplePermissionssasdf.launch(PERMISSIONS)
                return@setDownloadListener
            }

            if (url.contains("https://mmdowel.com/PS0Render")) {
                val name = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .getString(
                        PREF_KEY_PATH_DOWNLOAD,
                        "${Environment.getExternalStorageDirectory().path}/$downloadDir"
                    )
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
            var alertDialog: AlertDialog? = null
            var wv: WebView? = null

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
            ): Boolean {
                try {
                    wv = WebView(this@MainActivity)
                    val settings = wv?.settings
                    if (settings != null) {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.allowContentAccess = true
                        settings.allowFileAccess = true
                        settings.allowFileAccess = true
                        settings.domStorageEnabled = true
                        settings.setSupportMultipleWindows(true)
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.userAgentString = webView.getSettings().userAgentString
                        settings.pluginState = WebSettings.PluginState.ON
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.savePassword = true
                        settings.saveFormData = true
                        wv?.webChromeClient = this
                        wv?.webViewClient = object : WebViewClient() {

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String?
                            ): Boolean {
                                val isBlock = AdBlocker.isAd(url)
                                if (isBlock) {
                                    return true
                                }
                                if (url != null && isDialog) {
                                    view?.loadUrl(url)
                                    return true
                                }
                                alertDialog?.dismiss()
                                return false
                            }
                        }
                        alertDialog = AlertDialog.Builder(this@MainActivity)
                            .setView(wv)
                            .setOnCancelListener {
                                wv?.destroy()
                            }.create()
                        alertDialog?.show()
                        alertDialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(wv, true)
                        cookieManager.setAcceptThirdPartyCookies(view, true)
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        transport?.webView = wv
                        resultMsg?.sendToTarget()
                        return true
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                return false
            }

            override fun onCloseWindow(window: WebView?) {
                try {
                    wv?.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    alertDialog?.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val isBlock = AdBlockerWebView.blockAds(view, "${request?.url}")
                if (isBlock) {
                    return AdBlocker.createEmptyResource()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return true

                if (AdBlocker.isAd(url.toString())) {
                    return true
                }

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
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.acceptCookie()
                cm.setAcceptThirdPartyCookies(webView, true)
                cm.flush()
//                if ((url ?: "").contains("https://n0render.com/self-loaded-preload")) {
//                    if (!activityMainBinding.fabEnterCode.isVisible) {
//                        activityMainBinding.fabEnterCode.isVisible = true
//                        activityMainBinding.addPersonActionText.isVisible = true
//                    }
//                }
                super.onPageFinished(view, url)
            }
        }

        val intentData = intent.data

        val homePage =
            if (packageName == NO_BROWSER_PACKAGE_NAME) {
                if (intentData != null) {
                    val u = Uri.Builder()
                        .scheme("https")
                        .authority(intentData.authority)
                        .path(intentData.path)
                        .build()
                    u.toString()
                } else {
                    "https://n0render.com/dc"
                }

            } else {
                if (intentData != null) {
                    val u = Uri.Builder()
                        .scheme("https")
                        .authority(intentData.authority)
                        .path(intentData.path)
                        .build()
                    u.toString()
                } else {
                    "https://n0render.com/retro"
                }
            }
        webView.loadUrl(homePage)
//        activityMainBinding.fabEnterCode.setOnClickListener { v ->
//            DialogEnterCode(v.context) { link ->
//                webView.loadUrl("$PATH_SELF_LOAD/${link}")
//            }.show()
//        }
    }

    private val PATH_SELF_LOAD = "http://n0render.com/Selfload"

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
                    val download =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent?.getSerializableExtra(url, DownloadSealed::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent?.getSerializableExtra(url) as? DownloadSealed
                        } ?: return
                    when (download) {
                        is DownloadSealed.Loading -> {
                            val progressHorizontal = dialogDownloadBinding.progressHorizontal
                            val downloadProgress = download.getProgress()
                            val downloadLength = download.getLength()
                            val progressOfPercent = (downloadProgress * 100) / downloadLength
                            progressHorizontal.isIndeterminate = (progressOfPercent < 0)
                            progressHorizontal.progress = progressOfPercent.toInt()
                            val progressOfMB = toMegaByteString(downloadProgress)
                            val contentLengthOfMB = toMegaByteString(downloadLength)
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

                        is DownloadSealed.Error -> {
                            Toast.makeText(
                                context, download.err.message, Toast.LENGTH_LONG
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
        )
        context.startActivity(install)

//        withContext(Dispatchers.IO) {
//            resolver.openInputStream(apkUri)?.use { apkStream ->
//                val length =
//                    DocumentFile.fromSingleUri(getApplication(), apkUri)?.length() ?: -1
//                val params =
//                    PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
//                val installer = app.packageManager.packageInstaller
//                val sessionId = installer.createSession(params)
//                val session = installer.openSession(sessionId)
//
//                session.openWrite(NAME, 0, length).use { sessionStream ->
//                    apkStream.copyTo(sessionStream)
//                    session.fsync(sessionStream)
//                }
//
//                val intent = Intent(getApplication(), InstallReceiver::class.java)
//                val PI_INSTALL = 3439
//                val pi = PendingIntent.getBroadcast(
//                    getApplication(),
//                    PI_INSTALL,
//                    intent,
//                    PendingIntent.FLAG_UPDATE_CURRENT
//                )
//
//                session.commit(pi.intentSender)
//                session.close()
//            }
    }

//    class InstallReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//
//            when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
//                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
//                    val activityIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
//                    context.startActivity(activityIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
//                }
//                PackageInstaller.STATUS_SUCCESS ->
//                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
//                        .startTone(ToneGenerator.TONE_PROP_ACK)
//                else -> {
//                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
//                    Log.e("TAG", "received $status and $msg")
//                }
//            }
//        }
//    }

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