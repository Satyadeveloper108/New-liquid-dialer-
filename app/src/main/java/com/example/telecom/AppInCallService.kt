package com.example.telecom

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.notification.OngoingCallNotificationManager

class AppInCallService : InCallService() {

  override fun onCreate() {
    super.onCreate()
    TelecomCallManager.registerInCallService(this)
    startForegroundCallService()
  }

  private fun startForegroundCallService() {
    try {
      OngoingCallNotificationManager.createNotificationChannel(this)
      val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      val notification: Notification = NotificationCompat.Builder(this, OngoingCallNotificationManager.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_call)
        .setContentTitle("Phone Call Active")
        .setContentText("Call service and audio capture active")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setOngoing(true)
        .setContentIntent(pendingIntent)
        .build()

      val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
      } else {
        0
      }

      ServiceCompat.startForeground(this, 101, notification, foregroundType)
      Log.d(TAG, "ServiceCompat.startForeground started successfully with type=$foregroundType")
    } catch (e: Exception) {
      Log.e(TAG, "Error invoking ServiceCompat.startForeground", e)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    OngoingCallNotificationManager.onConfigurationChanged(this)
  }

  override fun onCallAdded(call: Call) {
    super.onCallAdded(call)
    TelecomCallManager.onCallAdded(call, this)
    startForegroundCallService()

    // Bring Dialer app to foreground so the user sees Incoming or Active Call UI
    try {
      val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_SINGLE_TOP or
          Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      startActivity(intent)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onCallRemoved(call: Call) {
    super.onCallRemoved(call)
    TelecomCallManager.onCallRemoved(call)
  }

  // Legacy CallAudioState (API < 34)
  @Deprecated("Deprecated in Java")
  override fun onCallAudioStateChanged(audioState: CallAudioState) {
    super.onCallAudioStateChanged(audioState)
    TelecomCallManager.onAudioStateChanged(audioState)
  }

  // Modern CallEndpoint APIs (API 34+)
  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
    super.onCallEndpointChanged(callEndpoint)
    TelecomCallManager.onCallEndpointChanged(callEndpoint)
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  override fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint>) {
    super.onAvailableCallEndpointsChanged(availableEndpoints)
    TelecomCallManager.onAvailableCallEndpointsChanged(availableEndpoints)
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  override fun onMuteStateChanged(isMuted: Boolean) {
    super.onMuteStateChanged(isMuted)
    TelecomCallManager.onMuteStateChanged(isMuted)
  }

  override fun onBringToForeground(showDialpad: Boolean) {
    super.onBringToForeground(showDialpad)
    try {
      val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
      startActivity(intent)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    } catch (e: Exception) {
      Log.w(TAG, "Error in stopForeground", e)
    }
    TelecomCallManager.unregisterInCallService(this)
  }

  companion object {
    private const val TAG = "AppInCallService"
  }
}
