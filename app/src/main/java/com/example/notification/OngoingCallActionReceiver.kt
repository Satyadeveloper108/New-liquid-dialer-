package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.telecom.TelecomCallManager

class OngoingCallActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      OngoingCallNotificationManager.ACTION_HANG_UP -> {
        TelecomCallManager.endCall()
        OngoingCallNotificationManager.cancelNotification(context)
      }
      OngoingCallNotificationManager.ACTION_TOGGLE_SPEAKER -> {
        val currentSession = TelecomCallManager.telecomCallSession.value
        val isCurrentlySpeaker = currentSession?.isSpeaker == true
        val targetSpeaker = !isCurrentlySpeaker
        TelecomCallManager.setSpeaker(targetSpeaker)
        currentSession?.let {
          OngoingCallNotificationManager.showOrUpdateNotification(
            context,
            it.copy(isSpeaker = targetSpeaker),
          )
        }
      }
    }
  }
}
