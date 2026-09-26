package com.example.telecom

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import com.example.MainActivity

class AppInCallService : InCallService() {

  override fun onCallAdded(call: Call) {
    super.onCallAdded(call)
    TelecomCallManager.onCallAdded(call, this)

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
    TelecomCallManager.unregisterInCallService(this)
  }
}
