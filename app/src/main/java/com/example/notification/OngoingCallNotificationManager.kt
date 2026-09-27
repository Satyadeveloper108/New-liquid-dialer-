package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.CallSession
import com.example.model.CallState

object OngoingCallNotificationManager {

  const val CHANNEL_ID = "ongoing_call_channel"
  private const val CHANNEL_NAME = "Ongoing Calls"
  private const val NOTIFICATION_ID = 9001

  const val ACTION_HANG_UP = "com.example.action.HANG_UP"
  const val ACTION_TOGGLE_SPEAKER = "com.example.action.TOGGLE_SPEAKER"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = "Shows active ongoing call status and controls"
        setShowBadge(false)
        enableVibration(false)
        setSound(null, null)
      }
      notificationManager?.createNotificationChannel(channel)
    }
  }

  fun showOrUpdateNotification(context: Context, session: CallSession) {
    // Only show for active or held calls (from answered until ended)
    if (session.state != CallState.ACTIVE && session.state != CallState.HOLDING) {
      cancelNotification(context)
      return
    }

    // Check POST_NOTIFICATIONS permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!hasPermission) {
        return
      }
    }

    createNotificationChannel(context)

    // Intent to open MainActivity when tapping the notification body
    val contentIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      0,
      contentIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    // Intent for "Hang up" action
    val hangUpIntent = Intent(context, OngoingCallActionReceiver::class.java).apply {
      action = ACTION_HANG_UP
    }
    val hangUpPendingIntent = PendingIntent.getBroadcast(
      context,
      1,
      hangUpIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    // Intent for "Turn speaker on" / "Turn speaker off" action
    val speakerIntent = Intent(context, OngoingCallActionReceiver::class.java).apply {
      action = ACTION_TOGGLE_SPEAKER
    }
    val speakerPendingIntent = PendingIntent.getBroadcast(
      context,
      2,
      speakerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val callerTitle = session.callerName.ifBlank {
      session.phoneNumber.ifBlank { "Unknown Caller" }
    }

    val speakerLabel = if (session.isSpeaker) "Turn speaker off" else "Turn speaker on"

    val hangUpAction = NotificationCompat.Action.Builder(
      android.R.drawable.ic_menu_close_clear_cancel,
      "Hang up",
      hangUpPendingIntent,
    ).build()

    val speakerAction = NotificationCompat.Action.Builder(
      android.R.drawable.stat_notify_chat,
      speakerLabel,
      speakerPendingIntent,
    ).build()

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_menu_call)
      .setContentTitle(callerTitle)
      .setContentText(session.formattedDuration)
      .setSubText("On-going call")
      .setColor(0xFF007AFF.toInt())
      .setColorized(true)
      .setOngoing(true)
      .setAutoCancel(false)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setContentIntent(contentPendingIntent)
      .addAction(hangUpAction)
      .addAction(speakerAction)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.notify(NOTIFICATION_ID, builder.build())
  }

  fun cancelNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.cancel(NOTIFICATION_ID)
  }
}
