package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Handles standard DTMF keypad tone playback using Android's ToneGenerator.
 * Designed to fail gracefully if audio hardware or ToneGenerator is unavailable.
 */
class DtmfPlayer {

  private var toneGenerator: ToneGenerator? = null

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 80)
    } catch (e: Throwable) {
      Log.w("DtmfPlayer", "ToneGenerator unavailable or not supported in this environment: ${e.message}")
      toneGenerator = null
    }
  }

  /**
   * Plays the standard DTMF tone corresponding to the keypad character.
   */
  fun playTone(char: Char, durationMs: Int = 150) {
    val tone = getToneForChar(char) ?: return
    try {
      toneGenerator?.startTone(tone, durationMs)
    } catch (e: Throwable) {
      Log.w("DtmfPlayer", "Failed to play DTMF tone for '$char': ${e.message}")
    }
  }

  /**
   * Stops any currently playing tone.
   */
  fun stopTone() {
    try {
      toneGenerator?.stopTone()
    } catch (e: Throwable) {
      // Ignored
    }
  }

  /**
   * Releases native ToneGenerator resources to prevent audio resource leaks.
   */
  fun release() {
    try {
      toneGenerator?.release()
    } catch (e: Throwable) {
      // Ignored
    } finally {
      toneGenerator = null
    }
  }

  companion object {
    /**
     * Maps standard keypad characters ('0'-'9', '*', '#') to ToneGenerator DTMF constants.
     * Note: '+' is not a standard DTMF tone key and returns null.
     */
    fun getToneForChar(char: Char): Int? {
      return when (char) {
        '0' -> ToneGenerator.TONE_DTMF_0
        '1' -> ToneGenerator.TONE_DTMF_1
        '2' -> ToneGenerator.TONE_DTMF_2
        '3' -> ToneGenerator.TONE_DTMF_3
        '4' -> ToneGenerator.TONE_DTMF_4
        '5' -> ToneGenerator.TONE_DTMF_5
        '6' -> ToneGenerator.TONE_DTMF_6
        '7' -> ToneGenerator.TONE_DTMF_7
        '8' -> ToneGenerator.TONE_DTMF_8
        '9' -> ToneGenerator.TONE_DTMF_9
        '*' -> ToneGenerator.TONE_DTMF_S
        '#' -> ToneGenerator.TONE_DTMF_P
        else -> null
      }
    }
  }
}
