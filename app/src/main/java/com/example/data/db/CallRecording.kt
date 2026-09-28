package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "call_recordings")
data class CallRecording(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val phoneNumber: String,
  val callerName: String,
  val filePath: String,
  val timestamp: Long = System.currentTimeMillis(),
  val durationMs: Long = 0L,
) {
  val formattedDateTime: String
    get() {
      val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
      return sdf.format(Date(timestamp))
    }

  val formattedDuration: String
    get() {
      val totalSecs = (durationMs / 1000).coerceAtLeast(0)
      val mins = totalSecs / 60
      val secs = totalSecs % 60
      return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
