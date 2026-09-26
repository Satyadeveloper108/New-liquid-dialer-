package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CallState
import com.example.model.NavTab
import com.example.ui.components.AddContactDialog
import com.example.ui.components.IosBottomNav
import com.example.ui.components.MiniCallBanner
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.FavouritesScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.KeypadScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.VoicemailScreen
import com.example.ui.theme.LocalIosColors
import com.example.viewmodel.DialerViewModel

@Composable
fun DialerApp(
  viewModel: DialerViewModel,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val dialedDigits by viewModel.dialedDigits.collectAsStateWithLifecycle()
  val formattedNumber by viewModel.formattedNumber.collectAsStateWithLifecycle()
  val recentsFilter by viewModel.recentsFilter.collectAsStateWithLifecycle()
  val contactsSearchQuery by viewModel.contactsSearchQuery.collectAsStateWithLifecycle()
  val contacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
  val favourites by viewModel.favouriteContacts.collectAsStateWithLifecycle()
  val recents by viewModel.recents.collectAsStateWithLifecycle()
  val voicemails by viewModel.voicemails.collectAsStateWithLifecycle()
  val unreadVoicemails by viewModel.unreadVoicemailCount.collectAsStateWithLifecycle()
  val callSession by viewModel.callSession.collectAsStateWithLifecycle()
  val isCallMinimized by viewModel.isCallMinimized.collectAsStateWithLifecycle()

  val hasContactsPermission by viewModel.hasContactsPermission.collectAsStateWithLifecycle()
  val hasCallLogPermission by viewModel.hasCallLogPermission.collectAsStateWithLifecycle()
  val isRealDeviceContacts by viewModel.isRealDeviceContacts.collectAsStateWithLifecycle()
  val isLoadingContacts by viewModel.isLoadingContacts.collectAsStateWithLifecycle()

  var prefilledAddContactNumber by remember { mutableStateOf<String?>(null) }

  // BackHandler to handle custom state navigation or backstack
  BackHandler(enabled = activeTab != NavTab.KEYPAD) {
    viewModel.selectTab(NavTab.KEYPAD)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background),
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = colors.background,
      bottomBar = {
        IosBottomNav(
          currentTab = activeTab,
          unreadVoicemails = unreadVoicemails,
          onSelectTab = { viewModel.selectTab(it) },
        )
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        AnimatedContent(
          targetState = activeTab,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "tab_switch_transition",
        ) { targetTab ->
          when (targetTab) {
            NavTab.FAVOURITES -> {
              FavouritesScreen(
                favourites = favourites,
                onContactClick = { contact ->
                  viewModel.startCall(contact.name, contact.phoneNumber)
                },
                onAddFavouriteClick = {
                  viewModel.selectTab(NavTab.CONTACTS)
                },
                onToggleFavorite = { contactId ->
                  viewModel.toggleFavorite(contactId)
                },
              )
            }

            NavTab.RECENTS -> {
              RecentsScreen(
                recents = recents,
                selectedFilter = recentsFilter,
                hasCallLogPermission = hasCallLogPermission,
                onFilterSelected = { viewModel.setRecentsFilter(it) },
                onCallRecordClick = { record ->
                  viewModel.startCall(record.contactName, record.phoneNumber)
                },
                onDeleteRecord = { id ->
                  viewModel.removeRecent(id)
                },
                onPermissionResult = { isGranted ->
                  viewModel.onCallLogPermissionResult(isGranted)
                },
              )
            }

            NavTab.CONTACTS -> {
              ContactsScreen(
                contacts = contacts,
                searchQuery = contactsSearchQuery,
                hasContactsPermission = hasContactsPermission,
                isRealDeviceContacts = isRealDeviceContacts,
                isLoadingContacts = isLoadingContacts,
                onSearchQueryChange = { viewModel.setContactsSearchQuery(it) },
                onContactClick = { contact ->
                  viewModel.startCall(contact.name, contact.phoneNumber)
                },
                onSaveNewContact = { name, phone, type ->
                  viewModel.saveNewContact(name, phone, type)
                },
                onSeedDemoContacts = {
                  viewModel.seedSampleContactsToDevice()
                },
                onPermissionResult = { isGranted ->
                  viewModel.onContactsPermissionResult(isGranted)
                },
                onToggleFavorite = { contactId ->
                  viewModel.toggleFavorite(contactId)
                },
              )
            }

            NavTab.KEYPAD -> {
              KeypadScreen(
                dialedDigits = dialedDigits,
                formattedNumber = formattedNumber,
                onDigitPress = { char ->
                  viewModel.playDtmfTone(char)
                  viewModel.appendDigit(char)
                },
                onDeleteDigit = { viewModel.deleteDigit() },
                onClearDigits = { viewModel.clearDigits() },
                onSetDialedNumber = { viewModel.setDialedNumber(it) },
                onStartCall = { number -> viewModel.startCall(number = number) },
                onSimulateIncomingCall = { viewModel.simulateIncomingCall() },
                onAddNumberToContact = { number ->
                  prefilledAddContactNumber = number
                },
              )
            }

            NavTab.VOICEMAIL -> {
              VoicemailScreen(
                voicemails = voicemails,
                onVoicemailClick = { voicemail ->
                  viewModel.markVoicemailAsRead(voicemail.id)
                },
              )
            }
          }
        }
      }
    }

    // Mini Persistent Call Banner (Visible when active call is minimized)
    MiniCallBanner(
      callSession = callSession,
      visible = isCallMinimized && callSession?.state == CallState.ACTIVE,
      onRestoreCall = { viewModel.restoreCall() },
      onEndCall = { viewModel.endCall() },
    )

    // Incoming Call Full-Screen Overlay
    if (callSession?.state == CallState.INCOMING) {
      IncomingCallScreen(
        callSession = callSession!!,
        onAccept = { viewModel.acceptCall() },
        onDecline = { viewModel.declineCall() },
      )
    }

    // Active Call Full-Screen Overlay (when not minimized)
    if (callSession?.state == CallState.ACTIVE && !isCallMinimized) {
      ActiveCallScreen(
        callSession = callSession!!,
        onEndCall = { viewModel.endCall() },
        onToggleMute = { viewModel.toggleMute() },
        onToggleSpeaker = { viewModel.toggleSpeaker() },
        onToggleKeypad = { viewModel.toggleKeypad() },
        onMinimize = { viewModel.minimizeCall() },
      )
    }

    // Add Contact Dialog from Keypad "Add Number"
    prefilledAddContactNumber?.let { number ->
      AddContactDialog(
        onDismiss = { prefilledAddContactNumber = null },
        onSaveContact = { name, phone, type ->
          viewModel.saveNewContact(name, phone, type)
          prefilledAddContactNumber = null
        },
      )
    }
  }
}
