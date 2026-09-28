package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.telecom.Call
import android.util.Log
import com.example.data.db.CallRecording
import java.io.File

/**
 * High-performance, 100% silent call recording engine.
 * Emits zero beeps, tones, sounds, or audio announcements to either party.
 * Tests multiple audio sources (VOICE_COMMUNICATION, MIC, VOICE_RECOGNITION)
 * using InCallService context for foreground microphone capture.
 */
class CallRecorder(private val defaultContext: Context) {

  private var mediaRecorder: MediaRecorder? = null
  private var currentRecordingFile: File? = null
  private var recordingStartTime: Long = 0L
  private var targetPhoneNumber: String = ""
  private var targetCallerName: String = ""
  private var isRecording: Boolean = false

  fun isRecordingActive(): Boolean = isRecording

  /**
   * Starts recording the call silently without any beeps, tones, or alerts.
   * If a real Telecom call is provided, verifies that the call is in Call.STATE_ACTIVE.
   */
  @Synchronized
  fun startRecording(
    phoneNumber: String,
    callerName: String,
    serviceContext: Context? = null,
    telecomCall: Call? = null,
  ): Boolean {
    if (isRecording) {
      Log.d(TAG, "Recording already in progress.")
      return true
    }

    if (telecomCall != null && telecomCall.state != Call.STATE_ACTIVE) {
      Log.w(TAG, "Cannot start recording: telecomCall is in state ${telecomCall.state}, not Call.STATE_ACTIVE")
      return false
    }

    val effectiveContext = serviceContext ?: defaultContext

    try {
      val recordingsDir = File(effectiveContext.filesDir, "call_recordings").apply {
        if (!exists()) mkdirs()
      }

      val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }.ifEmpty { "unknown" }
      val timestamp = System.currentTimeMillis()
      val outputFile = File(recordingsDir, "call_${cleanPhone}_${timestamp}.m4a")

      // Ordered candidates: VOICE_COMMUNICATION (duplex in-call with AEC), MIC, VOICE_RECOGNITION
      val candidateSources = listOf(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
        MediaRecorder.AudioSource.MIC to "MIC",
        MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
      )

      var successfulRecorder: MediaRecorder? = null
      var activeSourceName = ""

      for ((source, sourceName) in candidateSources) {
        var testRecorder: MediaRecorder? = null
        try {
          testRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(effectiveContext)
          } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
          }

          testRecorder.setAudioSource(source)
          testRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
          testRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
          testRecorder.setAudioChannels(1)
          testRecorder.setAudioEncodingBitRate(128000)
          testRecorder.setAudioSamplingRate(44100)
          testRecorder.setOutputFile(outputFile.absolutePath)
          testRecorder.prepare()
          testRecorder.start()

          successfulRecorder = testRecorder
          activeSourceName = sourceName
          Log.d(
            TAG,
            "startRecording state=${telecomCall?.state ?: "ACTIVE"} source=$sourceName file=${outputFile.absolutePath}"
          )
          break
        } catch (e: Exception) {
          Log.w(TAG, "AudioSource $sourceName failed on prepare/start, trying fallback", e)
          try {
            testRecorder?.reset()
            testRecorder?.release()
          } catch (ignored: Exception) {}
        }
      }

      if (successfulRecorder == null) {
        Log.e(TAG, "All candidate audio sources failed to initialize MediaRecorder")
        if (outputFile.exists()) {
          outputFile.delete()
        }
        cleanup()
        return false
      }

      mediaRecorder = successfulRecorder
      currentRecordingFile = outputFile
      recordingStartTime = timestamp
      targetPhoneNumber = phoneNumber
      targetCallerName = callerName
      isRecording = true

      Log.i(TAG, "Silent call recording successfully active with source=$activeSourceName for: $phoneNumber")
      return true
    } catch (e: Exception) {
      Log.e(TAG, "Fatal failure starting call recording", e)
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
      Log.w(TAG, "Recording file empty or too short (${file?.length() ?: 0} bytes), deleting broken entry")
      try {
        file?.delete()
      } catch (e: Exception) {
        Log.w(TAG, "Error deleting invalid recording file", e)
      }
      return null
    }

    Log.d(TAG, "stopRecording fileSize=${file.length()} bytes, duration=${durationMs}ms file=${file.absolutePath}")
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
