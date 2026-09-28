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
  val formattedDate: String
    get() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))

  val durationFormatted: String
    get() = String.format(Locale.getDefault(), "%d:%02d", durationMs / 60000, (durationMs % 60000) / 1000)

  val formattedDateTime: String
    get() = formattedDate

  val formattedDuration: String
    get() = durationFormatted
}
