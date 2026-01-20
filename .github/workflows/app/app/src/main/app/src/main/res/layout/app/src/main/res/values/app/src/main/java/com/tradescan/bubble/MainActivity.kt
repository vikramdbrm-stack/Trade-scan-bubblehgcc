package com.tradescan.bubble

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQ_MEDIA_PROJ = 1001
    private val REQ_OVERLAY = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            requestOverlayPermissionThenProjection()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }
    }

    private fun requestOverlayPermissionThenProjection() {
        if (!Settings.canDrawOverlays(this)) {
            val i = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(i, REQ_OVERLAY)
            return
        }
        requestMediaProjection()
    }

    private fun requestMediaProjection() {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), REQ_MEDIA_PROJ)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_OVERLAY) {
            requestMediaProjection()
            return
        }

        if (requestCode == REQ_MEDIA_PROJ && resultCode == Activity.RESULT_OK && data != null) {
            val i = Intent(this, OverlayService::class.java)
            i.putExtra(OverlayService.EXTRA_RESULT_CODE, resultCode)
            i.putExtra(OverlayService.EXTRA_DATA_INTENT, data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
        }
    }
}
