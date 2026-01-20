package com.tradescan.bubble

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer
import kotlin.math.max

class ScreenCaptureController(
    private val ctx: Context,
    private val resultCode: Int,
    private val dataIntent: Intent
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val projection: MediaProjection by lazy {
        val mgr = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mgr.getMediaProjection(resultCode, dataIntent)
    }

    private var reader: ImageReader? = null
    private var vdisplay: VirtualDisplay? = null

    fun captureOnce(cb: (Bitmap) -> Unit) {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        vdisplay = projection.createVirtualDisplay(
            "TradeScanCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface,
            null,
            mainHandler
        )

        mainHandler.postDelayed({
            val image = reader?.acquireLatestImage()
            if (image == null) {
                cleanupDisplay()
                return@postDelayed
            }

            val plane = image.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val bmp = Bitmap.createBitmap(
                width + max(0, rowPadding / pixelStride),
                height,
                Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buffer)
            image.close()

            val fixed = Bitmap.createBitmap(bmp, 0, 0, width, height)

            cleanupDisplay()
            cb(fixed)
        }, 250)
    }

    private fun cleanupDisplay() {
        vdisplay?.release()
        vdisplay = null
        reader?.close()
        reader = null
    }

    fun release() {
        cleanupDisplay()
        projection.stop()
    }
}
