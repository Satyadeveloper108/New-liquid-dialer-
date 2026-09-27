package com.example

import android.content.Context
import android.media.ToneGenerator
import androidx.test.core.app.ApplicationProvider
import com.example.audio.DtmfPlayer
import com.example.data.CallerNameResolver
import com.example.model.CallState
import com.example.model.Contact
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

  @Test
  fun `call states cover ringing dialing active holding and disconnected`() {
    val viewModel = createViewModel()
    viewModel.simulateIncomingCall(name = "Incoming Tester", number = "5551234")
    assertEquals(CallState.RINGING, viewModel.callSession.value?.state)

    viewModel.acceptCall()
    assertEquals(CallState.ACTIVE, viewModel.callSession.value?.state)

    viewModel.toggleHold()
    assertEquals(true, viewModel.callSession.value?.isOnHold)

    viewModel.endCall()
    assertNull(viewModel.callSession.value)
  }

  @Test
  fun `declining incoming ringing call clears session immediately without active call state`() {
    val viewModel = createViewModel()
    viewModel.simulateIncomingCall(name = "Declined Caller", number = "5559876")
    val ringingSession = viewModel.callSession.value
    assertNotNull(ringingSession)
    assertEquals(CallState.RINGING, ringingSession?.state)
    assertEquals(true, ringingSession?.isIncomingCall)
    assertEquals(false, ringingSession?.wasAnswered)

    // Decline directly from ringing state
    viewModel.declineCall()
    assertNull(viewModel.callSession.value)

    // Verify missed call record is added to recents
    val firstRecent = viewModel.recents.value.firstOrNull()
    assertEquals("Declined Caller", firstRecent?.contactName)
  }

  @Test
  fun `ending call inserts call record immediately into recents`() {
    val viewModel = createViewModel()
    viewModel.startCall(name = "John Doe", number = "5551122")
    assertEquals(CallState.ACTIVE, viewModel.callSession.value?.state)

    viewModel.endCall()
    assertNull(viewModel.callSession.value)

    val firstRecent = viewModel.recents.value.firstOrNull()
    assertEquals("John Doe", firstRecent?.contactName)
  }

  @Test
  fun `second call and call waiting state handling`() {
    val viewModel = createViewModel()
    viewModel.startCall(name = "Call 1", number = "111")
    assertEquals(CallState.ACTIVE, viewModel.callSession.value?.state)

    viewModel.simulateWaitingCall(name = "Call 2", number = "222")
    assertEquals(true, viewModel.callSession.value?.hasWaitingCall)
    assertEquals("Call 2", viewModel.callSession.value?.waitingCallName)

    viewModel.acceptWaitingCall(holdCurrent = true)
    assertEquals("Call 2", viewModel.callSession.value?.callerName)
    assertEquals("Call 1", viewModel.callSession.value?.heldCallName)
    assertEquals(true, viewModel.callSession.value?.canSwapCalls)

    viewModel.swapCalls()
    assertEquals("Call 1", viewModel.callSession.value?.callerName)
    assertEquals("Call 2", viewModel.callSession.value?.heldCallName)

    viewModel.mergeCalls()
    assertEquals(true, viewModel.callSession.value?.isConference)

    viewModel.endCall()
    assertNull(viewModel.callSession.value)
  }

  @Test
  fun `local saved contact name takes priority over carrier caller id name`() {
    CallerNameResolver.clearCache()
    val savedContacts = listOf(
      Contact(
        id = "c_1",
        name = "Anil Kumar Jena",
        phoneNumber = "+919876543210",
        type = "mobile",
        avatarColorIndex = 0,
        isFavorite = false,
      ),
    )

    // Real cellular call comes in with carrier caller-ID name
    val resolved = CallerNameResolver.resolveCallerName(
      context = null,
      phoneNumber = "+919876543210",
      telecomCallerName = "Airtel / Unknown Caller",
      inMemoryContacts = savedContacts,
    )

    // Must prioritize locally saved contact name "Anil Kumar Jena"
    assertEquals("Anil Kumar Jena", resolved)
  }

  @Test
  fun `normalized phone-number matching handles all national and international variants`() {
    CallerNameResolver.clearCache()
    val savedContacts = listOf(
      Contact(
        id = "c_1",
        name = "Anil Kumar Jena",
        phoneNumber = "+919876543210",
        type = "mobile",
        avatarColorIndex = 0,
        isFavorite = false,
      ),
    )

    // Check all required formats:
    // +91XXXXXXXXXX, 91XXXXXXXXXX, 0XXXXXXXXXX, XXXXXXXXXX, with spaces and hyphens
    val testVariants = listOf(
      "+919876543210",
      "919876543210",
      "09876543210",
      "9876543210",
      "+91 98765-43210",
      "0 98765 43210",
      "(98765) 43210",
    )

    for (variant in testVariants) {
      val resolved = CallerNameResolver.resolveCallerName(
        context = null,
        phoneNumber = variant,
        telecomCallerName = "Telecom Carrier Name",
        inMemoryContacts = savedContacts,
      )
      assertEquals("Failed for variant: $variant", "Anil Kumar Jena", resolved)
    }
  }

  @Test
  fun `carrier caller id and raw number fallbacks when number is unsaved`() {
    CallerNameResolver.clearCache()
    val savedContacts = listOf(
      Contact(
        id = "c_1",
        name = "Anil Kumar Jena",
        phoneNumber = "+919876543210",
        type = "mobile",
        avatarColorIndex = 0,
        isFavorite = false,
      ),
    )

    // 1. Unsaved number with Telecom caller ID -> Telecom caller ID
    val withTelecomName = CallerNameResolver.resolveCallerName(
      context = null,
      phoneNumber = "+919999988888",
      telecomCallerName = "Bank Helpline",
      inMemoryContacts = savedContacts,
    )
    assertEquals("Bank Helpline", withTelecomName)

    // 2. Unsaved number without Telecom caller ID -> Raw phone number
    val rawFallback = CallerNameResolver.resolveCallerName(
      context = null,
      phoneNumber = "+919999988888",
      telecomCallerName = null,
      inMemoryContacts = savedContacts,
    )
    assertEquals("+919999988888", rawFallback)
  }

  @Test
  fun `ongoing call notification manager creates channel and cancels without error`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.notification.OngoingCallNotificationManager.createNotificationChannel(context)
    com.example.notification.OngoingCallNotificationManager.cancelNotification(context)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
    val channel = notificationManager?.getNotificationChannel(com.example.notification.OngoingCallNotificationManager.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(android.app.NotificationManager.IMPORTANCE_LOW, channel?.importance)
  }
}
