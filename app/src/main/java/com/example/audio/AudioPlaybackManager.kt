package com.example.audio

import android.media.MediaPlayer
import android.util.Log
import com.example.data.db.CallRecording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlaybackManager(private val scope: CoroutineScope) {

  private var mediaPlayer: MediaPlayer? = null
  private var progressJob: Job? = null

  private val _playingRecordingId = MutableStateFlow<Long?>(null)
  val playingRecordingId: StateFlow<Long?> = _playingRecordingId.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _currentPositionMs = MutableStateFlow(0L)
  val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

  private val _totalDurationMs = MutableStateFlow(0L)
  val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

  fun play(recording: CallRecording) {
    if (_playingRecordingId.value == recording.id && mediaPlayer != null) {
      if (_isPlaying.value) {
        pause()
      } else {
        resume()
      }
      return
    }

    stop()

    val file = File(recording.filePath)
    if (!file.exists()) {
      Log.e(TAG, "Recording file does not exist: ${recording.filePath}")
      return
    }

    try {
      val player = MediaPlayer().apply {
        setDataSource(recording.filePath)
        prepare()
        setOnCompletionListener {
          stop()
        }
      }

      mediaPlayer = player
      _playingRecordingId.value = recording.id
      _totalDurationMs.value = player.duration.toLong().coerceAtLeast(recording.durationMs)
      _currentPositionMs.value = 0L

      player.start()
      _isPlaying.value = true

      startProgressLoop()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start audio playback", e)
      stop()
    }
  }

  fun pause() {
    try {
      mediaPlayer?.let { player ->
        if (player.isPlaying) {
          player.pause()
          _isPlaying.value = false
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error pausing playback", e)
    }
  }

  fun resume() {
    try {
      mediaPlayer?.let { player ->
        player.start()
        _isPlaying.value = true
        startProgressLoop()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error resuming playback", e)
    }
  }

  fun seekTo(positionMs: Long) {
    try {
      mediaPlayer?.let { player ->
        player.seekTo(positionMs.toInt())
        _currentPositionMs.value = positionMs
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error seeking playback", e)
    }
  }

  fun stop() {
    progressJob?.cancel()
    progressJob = null

    try {
      mediaPlayer?.apply {
        if (isPlaying) {
          stop()
        }
        reset()
        release()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error releasing MediaPlayer", e)
    } finally {
      mediaPlayer = null
      _playingRecordingId.value = null
      _isPlaying.value = false
      _currentPositionMs.value = 0L
      _totalDurationMs.value = 0L
    }
  }

  private fun startProgressLoop() {
    progressJob?.cancel()
    progressJob = scope.launch(Dispatchers.Main) {
      while (isActive && _isPlaying.value) {
        mediaPlayer?.let { player ->
          try {
            if (player.isPlaying) {
              _currentPositionMs.value = player.currentPosition.toLong()
            }
          } catch (e: Exception) {
            // Ignore position read error
          }
        }
        delay(100L)
      }
    }
  }

  companion object {
    private const val TAG = "AudioPlaybackManager"
  }
}
