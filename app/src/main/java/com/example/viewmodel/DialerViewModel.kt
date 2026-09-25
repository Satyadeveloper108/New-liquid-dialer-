package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleDataProvider
import com.example.model.CallRecord
import com.example.model.CallSession
import com.example.model.CallState
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.NavTab
import com.example.model.RecentsFilter
import com.example.model.VoicemailItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DialerViewModel : ViewModel() {

  private val _activeTab = MutableStateFlow(NavTab.KEYPAD)
  val activeTab: StateFlow<NavTab> = _activeTab.asStateFlow()

  private val _dialedDigits = MutableStateFlow("")
  val dialedDigits: StateFlow<String> = _dialedDigits.asStateFlow()

  val formattedNumber: StateFlow<String> = _dialedDigits.map { raw ->
    formatDialerNumber(raw)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  private val _recentsFilter = MutableStateFlow(RecentsFilter.ALL)
  val recentsFilter: StateFlow<RecentsFilter> = _recentsFilter.asStateFlow()

  private val _contactsSearchQuery = MutableStateFlow("")
  val contactsSearchQuery: StateFlow<String> = _contactsSearchQuery.asStateFlow()

  private val _contacts = MutableStateFlow(SampleDataProvider.sampleContacts)
  val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

  val filteredContacts: StateFlow<List<Contact>> = combine(_contacts, _contactsSearchQuery) { list, query ->
    if (query.isBlank()) {
      list.sortedBy { it.name }
    } else {
      list.filter {
        it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
      }.sortedBy { it.name }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.sampleContacts)

  val favouriteContacts: StateFlow<List<Contact>> = _contacts.map { list ->
    list.filter { it.isFavorite }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _recents = MutableStateFlow(SampleDataProvider.sampleRecents)
  val recents: StateFlow<List<CallRecord>> = combine(_recents, _recentsFilter) { list, filter ->
    when (filter) {
      RecentsFilter.ALL -> list
      RecentsFilter.MISSED -> list.filter { it.callType == CallType.MISSED }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.sampleRecents)

  private val _voicemails = MutableStateFlow(SampleDataProvider.sampleVoicemails)
  val voicemails: StateFlow<List<VoicemailItem>> = _voicemails.asStateFlow()

  val unreadVoicemailCount: StateFlow<Int> = _voicemails.map { list ->
    list.count { !it.isRead && !it.isDeleted }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

  private val _callSession = MutableStateFlow<CallSession?>(null)
  val callSession: StateFlow<CallSession?> = _callSession.asStateFlow()

  private val _isCallMinimized = MutableStateFlow(false)
  val isCallMinimized: StateFlow<Boolean> = _isCallMinimized.asStateFlow()

  private var callTimerJob: Job? = null

  fun selectTab(tab: NavTab) {
    _activeTab.value = tab
  }

  fun appendDigit(digit: Char) {
    _dialedDigits.update { it + digit }
  }

  fun deleteDigit() {
    _dialedDigits.update { if (it.isNotEmpty()) it.dropLast(1) else "" }
  }

  fun clearDigits() {
    _dialedDigits.value = ""
  }

  fun setDialedNumber(number: String) {
    _dialedDigits.value = number
  }

  fun setRecentsFilter(filter: RecentsFilter) {
    _recentsFilter.value = filter
  }

  fun setContactsSearchQuery(query: String) {
    _contactsSearchQuery.value = query
  }

  fun toggleFavorite(contactId: String) {
    _contacts.update { list ->
      list.map { contact ->
        if (contact.id == contactId) contact.copy(isFavorite = !contact.isFavorite) else contact
      }
    }
  }

  fun removeRecent(recordId: String) {
    _recents.update { list ->
      list.filter { it.id != recordId }
    }
  }

  fun markVoicemailAsRead(voicemailId: String) {
    _voicemails.update { list ->
      list.map {
        if (it.id == voicemailId) it.copy(isRead = true) else it
      }
    }
  }

  // Call Actions
  fun startCall(name: String? = null, number: String) {
    if (number.isBlank()) return
    val contactName = name ?: findContactName(number) ?: number
    callTimerJob?.cancel()
    _callSession.value = CallSession(
      callerName = contactName,
      phoneNumber = number,
      state = CallState.ACTIVE,
      durationSeconds = 0,
    )
    _isCallMinimized.value = false
    startCallTimer()
  }

  fun simulateIncomingCall(name: String = "Pupa Village", number: String = "+1 (555) 890-4321") {
    callTimerJob?.cancel()
    _callSession.value = CallSession(
      callerName = name,
      phoneNumber = number,
      state = CallState.INCOMING,
      durationSeconds = 0,
    )
    _isCallMinimized.value = false
  }

  fun acceptCall() {
    val current = _callSession.value ?: return
    _callSession.value = current.copy(state = CallState.ACTIVE)
    startCallTimer()
  }

  fun declineCall() {
    val current = _callSession.value
    if (current != null) {
      val newRecord = CallRecord(
        id = System.currentTimeMillis().toString(),
        contactName = current.callerName,
        phoneNumber = current.phoneNumber,
        callType = CallType.MISSED,
        timeFormatted = "Just now",
        dateFormatted = "Today",
      )
      _recents.update { listOf(newRecord) + it }
    }
    callTimerJob?.cancel()
    _callSession.value = null
    _isCallMinimized.value = false
  }

  fun endCall() {
    val current = _callSession.value
    if (current != null) {
      val newRecord = CallRecord(
        id = System.currentTimeMillis().toString(),
        contactName = current.callerName,
        phoneNumber = current.phoneNumber,
        callType = CallType.OUTGOING,
        timeFormatted = "Just now",
        dateFormatted = "Today",
      )
      _recents.update { listOf(newRecord) + it }
    }
    callTimerJob?.cancel()
    _callSession.value = null
    _isCallMinimized.value = false
  }

  fun minimizeCall() {
    _isCallMinimized.value = true
  }

  fun restoreCall() {
    _isCallMinimized.value = false
  }

  fun toggleMute() {
    _callSession.update { current ->
      current?.copy(isMuted = !current.isMuted)
    }
  }

  fun toggleSpeaker() {
    _callSession.update { current ->
      current?.copy(isSpeaker = !current.isSpeaker)
    }
  }

  fun toggleKeypad() {
    _callSession.update { current ->
      current?.copy(isKeypadOpen = !current.isKeypadOpen)
    }
  }

  private fun startCallTimer() {
    callTimerJob?.cancel()
    callTimerJob = viewModelScope.launch {
      while (true) {
        delay(1000)
        _callSession.update { current ->
          if (current?.state == CallState.ACTIVE) {
            current.copy(durationSeconds = current.durationSeconds + 1)
          } else {
            current
          }
        }
      }
    }
  }

  private fun findContactName(number: String): String? {
    val cleanNum = number.replace("[^0-9+]".toRegex(), "")
    return _contacts.value.firstOrNull {
      it.phoneNumber.replace("[^0-9+]".toRegex(), "") == cleanNum
    }?.name
  }

  private fun formatDialerNumber(raw: String): String {
    if (raw.isEmpty()) return ""
    // Format US numbers as (XXX) XXX-XXXX if 10 digits
    val digitsOnly = raw.filter { it.isDigit() }
    return when {
      raw.startsWith("*") || raw.startsWith("#") -> raw
      digitsOnly.length in 4..7 -> "${digitsOnly.take(3)}-${digitsOnly.drop(3)}"
      digitsOnly.length in 8..10 -> "(${digitsOnly.take(3)}) ${digitsOnly.substring(3, 6)}-${digitsOnly.drop(6)}"
      digitsOnly.length == 11 && digitsOnly.startsWith("1") -> "+1 (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.drop(7)}"
      else -> raw
    }
  }

  override fun onCleared() {
    super.onCleared()
    callTimerJob?.cancel()
  }
}
