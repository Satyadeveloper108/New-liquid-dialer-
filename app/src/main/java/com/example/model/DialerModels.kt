package com.example.model

enum class NavTab {
  FAVOURITES,
  RECENTS,
  CONTACTS,
  KEYPAD,
  VOICEMAIL
}

enum class CallType {
  INCOMING,
  OUTGOING,
  MISSED
}

enum class RecentsFilter {
  ALL,
  MISSED
}

enum class CallState {
  RINGING,
  DIALING,
  ACTIVE,
  HOLDING,
  DISCONNECTED,
  INCOMING,
  ENDED;

  val isRinging: Boolean
    get() = this == RINGING || this == INCOMING

  val isDialing: Boolean
    get() = this == DIALING

  val isActive: Boolean
    get() = this == ACTIVE

  val isHolding: Boolean
    get() = this == HOLDING

  val isDisconnected: Boolean
    get() = this == DISCONNECTED || this == ENDED
}

data class Contact(
  val id: String,
  val name: String,
  val phoneNumber: String,
  val type: String = "mobile",
  val avatarColorIndex: Int = 0,
  val isFavorite: Boolean = false,
  val photoUri: String? = null,
  val isSystemContact: Boolean = false,
) {
  val initial: Char
    get() = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '?'
}

data class CallRecord(
  val id: String,
  val contactName: String,
  val phoneNumber: String,
  val callType: CallType,
  val timeFormatted: String,
  val dateFormatted: String,
  val repeatCount: Int = 1,
  val phoneType: String = "mobile",
)

data class VoicemailItem(
  val id: String,
  val callerName: String,
  val phoneNumber: String,
  val dateFormatted: String,
  val durationFormatted: String,
  val transcription: String,
  val isRead: Boolean,
  val isDeleted: Boolean = false,
)

data class CallSession(
  val callerName: String,
  val phoneNumber: String,
  val state: CallState,
  val durationSeconds: Int = 0,
  val isMuted: Boolean = false,
  val isSpeaker: Boolean = false,
  val isKeypadOpen: Boolean = false,
  val isRealCall: Boolean = false,
  val isOnHold: Boolean = false,
  val isBluetoothAvailable: Boolean = false,
  val isBluetoothActive: Boolean = false,
  val audioEndpointName: String = "speaker",
  val heldCallName: String? = null,
  val heldCallNumber: String? = null,
  val canSwapCalls: Boolean = false,
  val canMergeCalls: Boolean = false,
  val isConference: Boolean = false,
  val waitingCallName: String? = null,
  val waitingCallNumber: String? = null,
  val hasWaitingCall: Boolean = false,
  val wasAnswered: Boolean = false,
  val isIncomingCall: Boolean = false,
) {
  val hasMultipleCalls: Boolean
    get() = heldCallName != null || isConference || hasWaitingCall

  val formattedDuration: String
    get() {
      val minutes = durationSeconds / 60
      val seconds = durationSeconds % 60
      return "%02d:%02d".format(minutes, seconds)
    }
}
