package com.tradescan.bubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object Notify {
    private const val CH = "trade_scan_ch"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CH, "TradeScan Signals", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }
    }

    fun foreground(ctx: Context): Notification {
        return NotificationCompat.Builder(ctx, CH)
            .setContentTitle("TradeScan Bubble Running")
            .setContentText("Tap bubble to scan USD/JPY (5m)")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    fun signal(ctx: Context, r: SignalResult) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = "SIGNAL: ${r.signal} (${r.confidence}%)"
        val text = "${r.note} | Expiry: 3 min | TF: 5m"

        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSound(tone)
            .setAutoCancel(true)
            .build()

        nm.notify(999, n)
    }
}
