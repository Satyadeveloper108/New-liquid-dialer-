package com.example.notification

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.CallSession
import com.example.model.CallState

object OngoingCallNotificationManager {

  const val CHANNEL_ID = "ongoing_call_channel"
  private const val CHANNEL_NAME = "Ongoing Calls"
  private const val NOTIFICATION_ID = 9001

  const val ACTION_HANG_UP = "com.example.action.HANG_UP"
  const val ACTION_TOGGLE_SPEAKER = "com.example.action.TOGGLE_SPEAKER"

  // App theme tokens for Liquid Glass notification card and text
  private const val CARD_COLOR_DARK = 0xFF1C1C1E.toInt()
  private const val CARD_COLOR_LIGHT = 0xFFFFFFFF.toInt()

  private const val TEXT_PRIMARY_DARK = 0xFFFFFFFF.toInt()
  private const val TEXT_PRIMARY_LIGHT = 0xFF000000.toInt()

  private const val TEXT_SECONDARY_DARK = 0xFFA0A5B5.toInt()
  private const val TEXT_SECONDARY_LIGHT = 0xFF8E8E93.toInt()

  private var lastSession: CallSession? = null
  private var componentCallbacksRegistered = false

  fun onConfigurationChanged(context: Context) {
    lastSession?.let { session ->
      showOrUpdateNotification(context, session)
    }
  }

  private fun ensureComponentCallbacks(context: Context) {
    if (!componentCallbacksRegistered) {
      val app = context.applicationContext as? Application ?: return
      app.registerComponentCallbacks(object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {
          lastSession?.let { session ->
            showOrUpdateNotification(app, session)
          }
        }

        override fun onLowMemory() {}

        override fun onTrimMemory(level: Int) {}
      })
      componentCallbacksRegistered = true
    }
  }

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

    lastSession = session
    ensureComponentCallbacks(context)

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

    // Determine card background and text colors based on current system UI mode (Dark / Light)
    val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val cardColor = if (isNightMode) CARD_COLOR_DARK else CARD_COLOR_LIGHT
    val primaryTextColor = if (isNightMode) TEXT_PRIMARY_DARK else TEXT_PRIMARY_LIGHT
    val secondaryTextColor = if (isNightMode) TEXT_SECONDARY_DARK else TEXT_SECONDARY_LIGHT

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

    // Intent for "Toggle speaker" action
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

    // Build custom iOS-style RemoteViews card layout
    val notificationLayout = RemoteViews(context.packageName, R.layout.notification_ongoing_call).apply {
      setTextViewText(R.id.tv_notification_duration, session.formattedDuration)
      setTextViewText(R.id.tv_notification_caller_name, callerTitle)
      setTextColor(R.id.tv_notification_caller_name, primaryTextColor)
      setTextColor(R.id.tv_notification_duration, secondaryTextColor)

      // Configure speaker button icon and background state
      if (session.isSpeaker) {
        setImageViewResource(R.id.btn_notification_speaker, R.drawable.ic_notification_speaker_on)
        setInt(R.id.btn_notification_speaker, "setBackgroundResource", R.drawable.bg_notification_button_speaker_on)
      } else {
        setImageViewResource(R.id.btn_notification_speaker, R.drawable.ic_notification_speaker_off)
        setInt(R.id.btn_notification_speaker, "setBackgroundResource", R.drawable.bg_notification_button_speaker_off)
      }

      // Wire PendingIntents for interactive buttons & body
      setOnClickPendingIntent(R.id.btn_notification_hangup, hangUpPendingIntent)
      setOnClickPendingIntent(R.id.btn_notification_speaker, speakerPendingIntent)
      setOnClickPendingIntent(R.id.notification_root, contentPendingIntent)
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_menu_call)
      .setCustomContentView(notificationLayout)
      .setCustomBigContentView(notificationLayout)
      .setStyle(NotificationCompat.DecoratedCustomViewStyle())
      .setColorized(true)
      .setColor(cardColor)
      .setOngoing(true)
      .setAutoCancel(false)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setContentIntent(contentPendingIntent)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.notify(NOTIFICATION_ID, builder.build())
  }

  fun cancelNotification(context: Context) {
    lastSession = null
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.cancel(NOTIFICATION_ID)
  }
}
