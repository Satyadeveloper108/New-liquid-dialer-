package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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

import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun DialerApp(
  viewModel: DialerViewModel,
  onRequestDefaultDialer: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val dialedDigits by viewModel.dialedDigits.collectAsStateWithLifecycle()
  val formattedNumber by viewModel.formattedNumber.collectAsStateWithLifecycle()
  val recentsFilter by viewModel.recentsFilter.collectAsStateWithLifecycle()
  val contactsSearchQuery by viewModel.contactsSearchQuery.collectAsStateWithLifecycle()
  val contacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
  val groupedContacts by viewModel.groupedContacts.collectAsStateWithLifecycle()
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

  // Hoisted LazyListStates to preserve scroll positions across tab navigation
  val contactsListState = rememberLazyListState()
  val recentsListState = rememberLazyListState()
  val favouritesListState = rememberLazyListState()

  var prefilledAddContactNumber by remember { mutableStateOf<String?>(null) }

  val isCallFullScreen = !isCallMinimized && (
    callSession?.state?.isActive == true ||
    callSession?.state?.isDialing == true ||
    callSession?.state?.isHolding == true ||
    (callSession?.state?.isDisconnected == true && (callSession?.wasAnswered == true || callSession?.isIncomingCall == false))
  )

  // BackHandler to handle custom state navigation or backstack (only active when no call is full-screen)
  BackHandler(enabled = activeTab != NavTab.KEYPAD && !isCallFullScreen) {
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
          transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
              (slideInHorizontally(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                initialOffsetX = { fullWidth -> fullWidth },
              ) + fadeIn(animationSpec = tween(160))) togetherWith
                (slideOutHorizontally(
                  animationSpec = tween(220, easing = FastOutSlowInEasing),
                  targetOffsetX = { fullWidth -> -fullWidth },
                ) + fadeOut(animationSpec = tween(120)))
            } else {
              (slideInHorizontally(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                initialOffsetX = { fullWidth -> -fullWidth },
              ) + fadeIn(animationSpec = tween(160))) togetherWith
                (slideOutHorizontally(
                  animationSpec = tween(220, easing = FastOutSlowInEasing),
                  targetOffsetX = { fullWidth -> fullWidth },
                ) + fadeOut(animationSpec = tween(120)))
            }
          },
          label = "tab_switch_transition",
        ) { targetTab ->
          when (targetTab) {
            NavTab.FAVOURITES -> {
              FavouritesScreen(
                favourites = favourites,
                listState = favouritesListState,
                onContactClick = { contact ->
                  viewModel.startOutgoingCall(contact.name, contact.phoneNumber)
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
                onRequestDefaultDialer = onRequestDefaultDialer,
                listState = recentsListState,
                onFilterSelected = { viewModel.setRecentsFilter(it) },
                onCallRecordClick = { record ->
                  viewModel.startOutgoingCall(record.contactName, record.phoneNumber)
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
                groupedContacts = groupedContacts,
                searchQuery = contactsSearchQuery,
                hasContactsPermission = hasContactsPermission,
                isRealDeviceContacts = isRealDeviceContacts,
                isLoadingContacts = isLoadingContacts,
                listState = contactsListState,
                onSearchQueryChange = { viewModel.setContactsSearchQuery(it) },
                onContactClick = { contact ->
                  viewModel.startOutgoingCall(contact.name, contact.phoneNumber)
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
                onStartCall = { number -> viewModel.startOutgoingCall(number = number) },
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

    // Mini Persistent Call Banner (Visible when active, dialing, or holding call is minimized)
    MiniCallBanner(
      callSession = callSession,
      visible = isCallMinimized && (callSession?.state?.isActive == true ||
                                    callSession?.state?.isDialing == true ||
                                    callSession?.state?.isHolding == true),
      onRestoreCall = { viewModel.restoreCall() },
      onEndCall = { viewModel.endCall() },
    )

    // Incoming Call Full-Screen Overlay (RINGING state)
    AnimatedVisibility(
      visible = callSession?.state?.isRinging == true,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
      ),
    ) {
      callSession?.let { session ->
        IncomingCallScreen(
          callSession = session,
          onAccept = { viewModel.acceptCall() },
          onDecline = { viewModel.declineCall() },
        )
      }
    }

    // Active & Outgoing Call Full-Screen Overlay (DIALING, ACTIVE, HOLDING, or DISCONNECTED for answered/outgoing calls)
    AnimatedVisibility(
      visible = (callSession?.state?.isActive == true ||
                 callSession?.state?.isDialing == true ||
                 callSession?.state?.isHolding == true ||
                 (callSession?.state?.isDisconnected == true && (callSession?.wasAnswered == true || callSession?.isIncomingCall == false))) && !isCallMinimized,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
      ),
    ) {
      callSession?.let { session ->
        ActiveCallScreen(
          callSession = session,
          onEndCall = { viewModel.endCall() },
          onToggleMute = { viewModel.toggleMute() },
          onToggleSpeaker = { viewModel.toggleSpeaker() },
          onToggleKeypad = { viewModel.toggleKeypad() },
          onMinimize = { viewModel.minimizeCall() },
          onToggleHold = { viewModel.toggleHold() },
          onDtmfTone = { char -> viewModel.sendDtmfTone(char) },
          onAddCall = { number -> viewModel.addSecondCall(number) },
          onSwapCalls = { viewModel.swapCalls() },
          onMergeCalls = { viewModel.mergeCalls() },
          onAcceptWaitingCall = { holdCurrent -> viewModel.acceptWaitingCall(holdCurrent) },
          onRejectWaitingCall = { viewModel.rejectWaitingCall() },
        )
      }
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
