package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.data.db.CallRecording
import java.io.File

/**
 * High-performance, 100% silent call recording engine.
 * Emits zero beeps, tones, sounds, or audio announcements to either party.
 */
class CallRecorder(private val context: Context) {

  private var mediaRecorder: MediaRecorder? = null
  private var currentRecordingFile: File? = null
  private var recordingStartTime: Long = 0L
  private var targetPhoneNumber: String = ""
  private var targetCallerName: String = ""
  private var isRecording: Boolean = false

  fun isRecordingActive(): Boolean = isRecording

  /**
   * Starts recording the call silently without any beeps, tones, or alerts.
   */
  @Synchronized
  fun startRecording(phoneNumber: String, callerName: String): Boolean {
    if (isRecording) {
      Log.d(TAG, "Recording already in progress.")
      return true
    }

    try {
      val recordingsDir = File(context.filesDir, "call_recordings").apply {
        if (!exists()) mkdirs()
      }

      val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }.ifEmpty { "unknown" }
      val timestamp = System.currentTimeMillis()
      val outputFile = File(recordingsDir, "call_${cleanPhone}_${timestamp}.m4a")

      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }

      // VOICE_COMMUNICATION captures in-call audio with hardware AEC / noise suppression.
      // If voice communication is restricted by OEM hardware, fallback safely to MIC without crashing.
      try {
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
      } catch (e: Exception) {
        Log.w(TAG, "VOICE_COMMUNICATION audio source unavailable, falling back to MIC", e)
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
      }

      recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      recorder.setAudioEncodingBitRate(128000)
      recorder.setAudioSamplingRate(44100)
      recorder.setOutputFile(outputFile.absolutePath)

      recorder.prepare()
      // Zero beep / sound generation — purely silent start
      recorder.start()

      mediaRecorder = recorder
      currentRecordingFile = outputFile
      recordingStartTime = timestamp
      targetPhoneNumber = phoneNumber
      targetCallerName = callerName
      isRecording = true

      Log.i(TAG, "Silent call recording started successfully for: $phoneNumber")
      return true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start silent call recording", e)
      cleanup()
      return false
    }
  }

  /**
   * Stops the ongoing recording and returns the CallRecording metadata entity.
   */
  @Synchronized
  fun stopRecording(): CallRecording? {
    if (!isRecording) return null

    val durationMs = (System.currentTimeMillis() - recordingStartTime).coerceAtLeast(0L)
    val file = currentRecordingFile
    val phone = targetPhoneNumber
    val name = targetCallerName
    val timestamp = recordingStartTime

    try {
      mediaRecorder?.apply {
        try {
          stop()
        } catch (e: Exception) {
          Log.w(TAG, "Error stopping MediaRecorder", e)
        }
        reset()
        release()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception during MediaRecorder release", e)
    } finally {
      cleanup()
    }

    if (file == null || !file.exists() || file.length() <= 0L || durationMs < 500L) {
      Log.w(TAG, "Recording file empty or too short, discarded")
      file?.delete()
      return null
    }

    Log.i(TAG, "Call recording completed. File size: ${file.length()} bytes, duration: ${durationMs}ms")
    return CallRecording(
      phoneNumber = phone,
      callerName = name.ifBlank { phone },
      filePath = file.absolutePath,
      timestamp = timestamp,
      durationMs = durationMs,
    )
  }

  @Synchronized
  private fun cleanup() {
    mediaRecorder = null
    currentRecordingFile = null
    recordingStartTime = 0L
    targetPhoneNumber = ""
    targetCallerName = ""
    isRecording = false
  }

  companion object {
    private const val TAG = "CallRecorder"
  }
}
