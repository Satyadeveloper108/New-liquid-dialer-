package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactsRepository
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

class DialerViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = ContactsRepository()

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

  // Real Contacts State
  private val _hasContactsPermission = MutableStateFlow(repository.hasContactsPermission(application))
  val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

  private val _hasCallLogPermission = MutableStateFlow(repository.hasCallLogPermission(application))
  val hasCallLogPermission: StateFlow<Boolean> = _hasCallLogPermission.asStateFlow()

  private val _isLoadingContacts = MutableStateFlow(false)
  val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts.asStateFlow()

  private val _isRealDeviceContacts = MutableStateFlow(false)
  val isRealDeviceContacts: StateFlow<Boolean> = _isRealDeviceContacts.asStateFlow()

  private val _contacts = MutableStateFlow<List<Contact>>(SampleDataProvider.sampleContacts)
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
  private val dtmfPlayer = com.example.audio.DtmfPlayer()

  init {
    loadDeviceDataIfPermitted()
  }

  fun loadDeviceDataIfPermitted() {
    val context = getApplication<Application>()
    val hasContacts = repository.hasContactsPermission(context)
    _hasContactsPermission.value = hasContacts
    if (hasContacts) {
      loadRealContacts()
    }

    val hasCallLog = repository.hasCallLogPermission(context)
    _hasCallLogPermission.value = hasCallLog
    if (hasCallLog) {
      loadRealCallLogs()
    }
  }

  fun onContactsPermissionResult(isGranted: Boolean) {
    _hasContactsPermission.value = isGranted
    if (isGranted) {
      loadRealContacts()
    }
  }

  fun onCallLogPermissionResult(isGranted: Boolean) {
    _hasCallLogPermission.value = isGranted
    if (isGranted) {
      loadRealCallLogs()
    }
  }

  fun loadRealContacts() {
    val context = getApplication<Application>()
    viewModelScope.launch {
      _isLoadingContacts.value = true
      try {
        val deviceList = repository.getDeviceContacts(context)
        if (deviceList.isNotEmpty()) {
          _contacts.value = deviceList
          _isRealDeviceContacts.value = true
        } else {
          _isRealDeviceContacts.value = true
          _contacts.value = emptyList()
        }
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        _isLoadingContacts.value = false
      }
    }
  }

  fun loadRealCallLogs() {
    val context = getApplication<Application>()
    viewModelScope.launch {
      try {
        val logs = repository.getDeviceCallLogs(context)
        if (logs.isNotEmpty()) {
          _recents.value = logs
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun saveNewContact(name: String, phoneNumber: String, type: String = "mobile") {
    val context = getApplication<Application>()
    viewModelScope.launch {
      val success = repository.addDeviceContact(context, name, phoneNumber, type)
      if (success) {
        Toast.makeText(context, "Contact \"$name\" saved to device!", Toast.LENGTH_SHORT).show()
        loadRealContacts()
      } else {
        // Add to local state if writing to contacts provider fails or is forbidden
        val newContact = Contact(
          id = System.currentTimeMillis().toString(),
          name = name,
          phoneNumber = phoneNumber,
          type = type,
          avatarColorIndex = Math.abs(name.hashCode()) % 8,
          isFavorite = false,
          isSystemContact = false,
        )
        _contacts.update { listOf(newContact) + it }
        Toast.makeText(context, "Contact \"$name\" added!", Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun seedSampleContactsToDevice() {
    val context = getApplication<Application>()
    viewModelScope.launch {
      _isLoadingContacts.value = true
      val count = repository.seedDemoContactsToDevice(context)
      Toast.makeText(context, "Populated $count real contacts to device!", Toast.LENGTH_SHORT).show()
      loadRealContacts()
    }
  }

  fun selectTab(tab: NavTab) {
    _activeTab.value = tab
  }

  fun playDtmfTone(char: Char) {
    dtmfPlayer.playTone(char)
  }

  fun appendDigit(digit: Char) {
    _dialedDigits.update { current ->
      if (digit == '+') {
        if (current.contains('+')) current else current + '+'
      } else {
        current + digit
      }
    }
  }

  fun appendPlus() {
    appendDigit('+')
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
    val context = getApplication<Application>()
    val currentContact = _contacts.value.firstOrNull { it.id == contactId } ?: return
    val newFav = !currentContact.isFavorite

    viewModelScope.launch {
      if (currentContact.isSystemContact) {
        repository.toggleFavoriteInDevice(context, contactId, newFav)
      }
      _contacts.update { list ->
        list.map { contact ->
          if (contact.id == contactId) contact.copy(isFavorite = newFav) else contact
        }
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

  // Real device phone call launch
  fun placeRealDeviceCall(context: Context, number: String) {
    if (number.isBlank()) return
    try {
      val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:${Uri.encode(number)}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      e.printStackTrace()
      startCall(number = number)
    }
  }

  // In-app Call Actions
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

  fun formatDialerNumber(raw: String): String {
    if (raw.isEmpty()) return ""
    if (raw.startsWith("*") || raw.startsWith("#") || raw.contains("#") || raw.contains("*")) {
      return raw
    }
    if (raw.startsWith("+")) {
      val afterPlus = raw.drop(1)
      return when {
        afterPlus.isEmpty() -> "+"
        afterPlus.length <= 3 -> "+$afterPlus"
        afterPlus.length <= 6 -> "+${afterPlus.take(3)} ${afterPlus.drop(3)}"
        afterPlus.length <= 10 -> "+${afterPlus.take(3)} ${afterPlus.substring(3, 6)} ${afterPlus.drop(6)}"
        else -> "+${afterPlus.take(3)} ${afterPlus.substring(3, 6)} ${afterPlus.substring(6, 10)} ${afterPlus.drop(10)}"
      }
    }
    val digitsOnly = raw.filter { it.isDigit() }
    return when {
      digitsOnly.length in 4..7 -> "${digitsOnly.take(3)} ${digitsOnly.drop(3)}"
      digitsOnly.length in 8..10 -> "${digitsOnly.take(3)} ${digitsOnly.substring(3, 6)} ${digitsOnly.drop(6)}"
      digitsOnly.length > 10 -> "${digitsOnly.take(3)} ${digitsOnly.substring(3, 6)} ${digitsOnly.substring(6, 10)} ${digitsOnly.drop(10)}"
      else -> raw
    }
  }

  override fun onCleared() {
    super.onCleared()
    callTimerJob?.cancel()
    dtmfPlayer.release()
  }
}
