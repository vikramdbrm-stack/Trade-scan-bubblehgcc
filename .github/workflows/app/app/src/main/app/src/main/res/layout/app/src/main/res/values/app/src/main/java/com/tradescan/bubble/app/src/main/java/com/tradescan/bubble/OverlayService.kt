package com.tradescan.bubble

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast

class OverlayService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA_INTENT = "data_intent"
        const val NOTIF_ID = 77
    }

    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private lateinit var capture: ScreenCaptureController

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        Notify.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, foregroundNotif())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val dataIntent = intent?.getParcelableExtra<Intent>(EXTRA_DATA_INTENT)

        if (dataIntent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        capture = ScreenCaptureController(this, resultCode, dataIntent)
        showBubble()

        return START_STICKY
    }

    private fun showBubble() {
        if (bubble != null) return

        val v = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null)
        v.minimumWidth = 140
        v.minimumHeight = 140
        v.setBackgroundColor(0xAA000000.toInt())

        v.setOnClickListener {
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show()
            capture.captureOnce { bmp ->
                val result = SignalAnalyzer.analyze(bmp)
                Notify.signal(this, result)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 40
        params.y = 240

        // Drag support
        v.setOnTouchListener(object : View.OnTouchListener {
            var initX = 0
            var initY = 0
            var touchX = 0f
            var touchY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = params.x
                        initY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initX + (touchX - event.rawX).toInt()
                        params.y = initY + (event.rawY - touchY).toInt()
                        wm.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        wm.addView(v, params)
        bubble = v
    }

    private fun foregroundNotif(): Notification {
        return Notify.foreground(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        bubble?.let { wm.removeView(it) }
        bubble = null
        if (::capture.isInitialized) capture.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
