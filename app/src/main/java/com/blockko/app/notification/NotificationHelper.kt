package com.blockko.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.blockko.app.MainActivity
import com.blockko.app.R
import com.blockko.app.vpn.BlockKoVpnService

const val VPN_NOTIFICATION_ID = 1001
const val VPN_NOTIFICATION_CHANNEL_ID = "blockko_protection"

object NotificationHelper {

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            VPN_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildStatusNotification(
        context: Context,
        contentText: String,
        isPaused: Boolean
    ): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleAction = if (isPaused) {
            NotificationCompat.Action(
                0, context.getString(R.string.notif_resume_action),
                servicePendingIntent(context, BlockKoVpnService.ACTION_RESUME, 2)
            )
        } else {
            NotificationCompat.Action(
                0, context.getString(R.string.notif_pause_action),
                servicePendingIntent(context, BlockKoVpnService.ACTION_PAUSE, 1)
            )
        }

        return NotificationCompat.Builder(context, VPN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .addAction(toggleAction)
            .build()
    }

    private fun servicePendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, BlockKoVpnService::class.java).setAction(action)
        return PendingIntent.getService(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
