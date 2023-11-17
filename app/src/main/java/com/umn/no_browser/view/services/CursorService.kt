package com.umn.no_browser.view.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.umn.no_browser.R


class CursorService : Service() {

    inner class CursorBinder : Binder() {
        fun getServices() = this@CursorService
    }

    private var windowManager: WindowManager? = null
    private var _rootView: View? = null

    val overlay = WindowManager.LayoutParams(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE;
        },

        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    override fun onBind(intent: Intent?) = CursorBinder()

    override fun onCreate() {
        super.onCreate()

        windowManager = ContextCompat.getSystemService(this, WindowManager::class.java)
        _rootView = LayoutInflater.from(this)
            .inflate(R.layout.cursor, null)
        overlay.gravity = Gravity.CENTER
//        windowManager?.addView(_rootView, overlay)
    }

    override fun onDestroy() {
        val view = _rootView
        if (view != null) {
            val x = view.x
            val y = view.y
            println("x: $x || y: $y")
            windowManager?.removeView(_rootView)
            _rootView = null
            windowManager = null
        }
        super.onDestroy()
    }

    fun move(deltaX: Float, deltaY: Float) {
        val cursorView = _rootView?.findViewById<View>(R.id.cursor)
        if (cursorView != null) {
            val cursorX = cursorView.x
            val cursorY = cursorView.y
            val x = cursorX + deltaX
            val y = cursorY + deltaY
            println("cursorX=$cursorX,cursorY=$cursorY")
            val isLeft = x <= -18
            if (isLeft) return
            val rootWidth = _rootView?.width ?: return
            val isRight = x >= rootWidth - 18
            if (isRight) return
            val isTop = y <= -18
            if (isTop) return
            val rooHeight = _rootView?.height ?: return
            val isBottom = y >= rooHeight - 18
            if (isBottom) return
            cursorView.x = x
            cursorView.y = y
        }
    }

    fun getX() = _rootView?.findViewById<View>(R.id.cursor)?.x ?: 0F

    fun getY() = _rootView?.findViewById<View>(R.id.cursor)?.y ?: 0F

    fun requestFocus() {
        windowManager?.addView(_rootView, overlay)
    }

    fun isFocused() = _rootView?.findViewById<View>(R.id.cursor)?.isFocused ?: false

    fun clearFocus() = _rootView?.findViewById<View>(R.id.cursor)?.clearFocus()

}