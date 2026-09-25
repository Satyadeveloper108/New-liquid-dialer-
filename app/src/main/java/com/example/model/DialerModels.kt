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
  INCOMING,
  ACTIVE,
  ENDED
}

data class Contact(
  val id: String,
  val name: String,
  val phoneNumber: String,
  val type: String = "mobile",
  val avatarColorIndex: Int = 0,
  val isFavorite: Boolean = false,
) {
  val initial: Char
    get() = name.firstOrNull()?.uppercaseChar() ?: '?'
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
) {
  val formattedDuration: String
    get() {
      val minutes = durationSeconds / 60
      val seconds = durationSeconds % 60
      return "%02d:%02d".format(minutes, seconds)
    }
}
