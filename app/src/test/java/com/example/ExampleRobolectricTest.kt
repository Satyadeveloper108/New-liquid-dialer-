package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CallState
import com.example.model.NavTab
import com.example.viewmodel.DialerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Phone", appName)
  }

  @Test
  fun `keypad appends and deletes digits correctly`() {
    val viewModel = DialerViewModel()
    viewModel.appendDigit('5')
    viewModel.appendDigit('5')
    viewModel.appendDigit('5')
    assertEquals("555", viewModel.dialedDigits.value)

    viewModel.deleteDigit()
    assertEquals("55", viewModel.dialedDigits.value)

    viewModel.clearDigits()
    assertEquals("", viewModel.dialedDigits.value)
  }

  @Test
  fun `tab selection transitions cleanly`() {
    val viewModel = DialerViewModel()
    viewModel.selectTab(NavTab.CONTACTS)
    assertEquals(NavTab.CONTACTS, viewModel.activeTab.value)

    viewModel.selectTab(NavTab.RECENTS)
    assertEquals(NavTab.RECENTS, viewModel.activeTab.value)
  }

  @Test
  fun `call session starts and ends properly`() {
    val viewModel = DialerViewModel()
    viewModel.startCall(name = "Test Caller", number = "1234567")
    val session = viewModel.callSession.value
    assertNotNull(session)
    assertEquals(CallState.ACTIVE, session?.state)
    assertEquals("Test Caller", session?.callerName)

    viewModel.endCall()
    assertEquals(null, viewModel.callSession.value)
  }
}
