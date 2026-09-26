package com.example

import android.content.Context
import android.media.ToneGenerator
import androidx.test.core.app.ApplicationProvider
import com.example.audio.DtmfPlayer
import com.example.model.CallState
import com.example.model.NavTab
import com.example.viewmodel.DialerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private fun createViewModel(): DialerViewModel {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    return DialerViewModel(application)
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Phone", appName)
  }

  @Test
  fun `entering digits 0 through 9 updates dialed digits immediately`() {
    val viewModel = createViewModel()
    for (digit in '0'..'9') {
      viewModel.appendDigit(digit)
    }
    assertEquals("0123456789", viewModel.dialedDigits.value)
  }

  @Test
  fun `entering star and pound appends correctly`() {
    val viewModel = createViewModel()
    viewModel.appendDigit('*')
    viewModel.appendDigit('1')
    viewModel.appendDigit('0')
    viewModel.appendDigit('0')
    viewModel.appendDigit('#')
    assertEquals("*100#", viewModel.dialedDigits.value)
  }

  @Test
  fun `deleting one character removes only the last digit`() {
    val viewModel = createViewModel()
    viewModel.appendDigit('1')
    viewModel.appendDigit('2')
    viewModel.appendDigit('3')
    assertEquals("123", viewModel.dialedDigits.value)

    viewModel.deleteDigit()
    assertEquals("12", viewModel.dialedDigits.value)

    viewModel.deleteDigit()
    assertEquals("1", viewModel.dialedDigits.value)
  }

  @Test
  fun `deleting all characters clears dialed buffer`() {
    val viewModel = createViewModel()
    viewModel.setDialedNumber("1234567890")
    assertEquals("1234567890", viewModel.dialedDigits.value)

    viewModel.clearDigits()
    assertEquals("", viewModel.dialedDigits.value)
  }

  @Test
  fun `empty state delete behavior does nothing and does not crash`() {
    val viewModel = createViewModel()
    viewModel.clearDigits()
    assertEquals("", viewModel.dialedDigits.value)

    viewModel.deleteDigit()
    assertEquals("", viewModel.dialedDigits.value)

    viewModel.deleteDigit()
    assertEquals("", viewModel.dialedDigits.value)
  }

  @Test
  fun `long press 0 inserts plus sign`() {
    val viewModel = createViewModel()
    viewModel.clearDigits()
    viewModel.appendPlus()
    assertEquals("+", viewModel.dialedDigits.value)

    viewModel.appendDigit('1')
    viewModel.appendDigit('2')
    assertEquals("+12", viewModel.dialedDigits.value)
  }

  @Test
  fun `preventing invalid repeated plus sign`() {
    val viewModel = createViewModel()
    viewModel.clearDigits()
    viewModel.appendPlus()
    assertEquals("+", viewModel.dialedDigits.value)

    // Attempting repeated plus should not add second plus
    viewModel.appendPlus()
    assertEquals("+", viewModel.dialedDigits.value)

    viewModel.appendDigit('+')
    assertEquals("+", viewModel.dialedDigits.value)

    viewModel.appendDigit('4')
    viewModel.appendDigit('4')
    viewModel.appendPlus()
    assertEquals("+44", viewModel.dialedDigits.value)
  }

  @Test
  fun `normal tap on 0 continues inserting 0`() {
    val viewModel = createViewModel()
    viewModel.clearDigits()
    viewModel.appendDigit('0')
    viewModel.appendDigit('0')
    assertEquals("00", viewModel.dialedDigits.value)
  }

  @Test
  fun `state persistence and raw number separated from formatted display`() {
    val viewModel = createViewModel()
    viewModel.clearDigits()
    viewModel.appendDigit('1')
    viewModel.appendDigit('2')
    viewModel.appendDigit('3')
    viewModel.appendDigit('4')
    viewModel.appendDigit('5')
    viewModel.appendDigit('6')

    // Raw value stays unadulterated
    assertEquals("123456", viewModel.dialedDigits.value)

    // Formatted value is derived
    val formatted = viewModel.formatDialerNumber(viewModel.dialedDigits.value)
    assertEquals("123 456", formatted)

    // International number with +
    val internationalFormatted = viewModel.formatDialerNumber("+447911123456")
    assertEquals("+447 911 1234 56", internationalFormatted)
  }

  @Test
  fun `dtmf player maps all standard keypad keys correctly`() {
    assertEquals(ToneGenerator.TONE_DTMF_0, DtmfPlayer.getToneForChar('0'))
    assertEquals(ToneGenerator.TONE_DTMF_1, DtmfPlayer.getToneForChar('1'))
    assertEquals(ToneGenerator.TONE_DTMF_2, DtmfPlayer.getToneForChar('2'))
    assertEquals(ToneGenerator.TONE_DTMF_3, DtmfPlayer.getToneForChar('3'))
    assertEquals(ToneGenerator.TONE_DTMF_4, DtmfPlayer.getToneForChar('4'))
    assertEquals(ToneGenerator.TONE_DTMF_5, DtmfPlayer.getToneForChar('5'))
    assertEquals(ToneGenerator.TONE_DTMF_6, DtmfPlayer.getToneForChar('6'))
    assertEquals(ToneGenerator.TONE_DTMF_7, DtmfPlayer.getToneForChar('7'))
    assertEquals(ToneGenerator.TONE_DTMF_8, DtmfPlayer.getToneForChar('8'))
    assertEquals(ToneGenerator.TONE_DTMF_9, DtmfPlayer.getToneForChar('9'))
    assertEquals(ToneGenerator.TONE_DTMF_S, DtmfPlayer.getToneForChar('*'))
    assertEquals(ToneGenerator.TONE_DTMF_P, DtmfPlayer.getToneForChar('#'))
    assertNull(DtmfPlayer.getToneForChar('+'))
    assertNull(DtmfPlayer.getToneForChar('X'))
  }

  @Test
  fun `tab selection transitions cleanly`() {
    val viewModel = createViewModel()
    viewModel.selectTab(NavTab.CONTACTS)
    assertEquals(NavTab.CONTACTS, viewModel.activeTab.value)

    viewModel.selectTab(NavTab.RECENTS)
    assertEquals(NavTab.RECENTS, viewModel.activeTab.value)
  }

  @Test
  fun `call session starts and ends properly`() {
    val viewModel = createViewModel()
    viewModel.startCall(name = "Test Caller", number = "1234567")
    val session = viewModel.callSession.value
    assertNotNull(session)
    assertEquals(CallState.ACTIVE, session?.state)
    assertEquals("Test Caller", session?.callerName)

    viewModel.endCall()
    assertNull(viewModel.callSession.value)
  }
}
