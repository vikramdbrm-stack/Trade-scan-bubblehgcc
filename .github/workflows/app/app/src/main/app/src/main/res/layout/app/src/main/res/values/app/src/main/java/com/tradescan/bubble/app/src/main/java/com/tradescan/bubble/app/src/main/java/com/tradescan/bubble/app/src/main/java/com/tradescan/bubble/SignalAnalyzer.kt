package com.tradescan.bubble

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SignalResult(
    val signal: String,       // UP / DOWN / WAIT
    val confidence: Int,      // %
    val note: String
)

object SignalAnalyzer {

    // Crop zone (relative) for your OlympTrade screenshot
    private fun cropChartArea(src: Bitmap): Bitmap {
        val W = src.width
        val H = src.height

        val x1 = (0.05f * W).toInt()
        val x2 = (0.92f * W).toInt()
        val y1 = (0.15f * H).toInt()
        val y2 = (0.73f * H).toInt()

        val w = max(10, x2 - x1)
        val h = max(10, y2 - y1)

        return Bitmap.createBitmap(src, x1, y1, w, h)
    }

    fun analyze(screen: Bitmap): SignalResult {
        val chart = cropChartArea(screen)

        // sample last-candle region (right side)
        val W = chart.width
        val H = chart.height

        val sampleXStart = (0.78f * W).toInt()
        val sampleXEnd = (0.93f * W).toInt()
        val sampleYStart = (0.15f * H).toInt()
        val sampleYEnd = (0.85f * H).toInt()

        var green = 0
        var red = 0
        var total = 0

        for (y in sampleYStart until sampleYEnd step 6) {
            for (x in sampleXStart until sampleXEnd step 6) {
                val px = chart.getPixel(x, y)
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)

                // ignore dark background pixels
                if (r + g + b < 60) continue

                if (g > r + 25 && g > b + 25) green++
                if (r > g + 25 && r > b + 25) red++

                total++
            }
        }

        if (total < 30) {
            return SignalResult("WAIT", 50, "Low data (zoom chart)")
        }

        val diff = green - red
        val strength = abs(diff) * 100 / max(1, total)
        val conf = min(90, max(55, 55 + strength))

        return when {
            diff > 8 -> SignalResult("UP", conf, "Bullish pressure")
            diff < -8 -> SignalResult("DOWN", conf, "Bearish pressure")
            else -> SignalResult("WAIT", 60, "Sideways / mixed candles")
        }
    }
}
