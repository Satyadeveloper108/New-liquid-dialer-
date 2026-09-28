package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallerNameResolver
import com.example.data.ContactsRepository
import com.example.data.SampleDataProvider
import com.example.model.CallRecord
import com.example.model.CallSession
import com.example.model.CallState
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.NavTab
import com.example.model.RecentsFilter
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import android.util.Log
import com.example.audio.AudioPlaybackManager
import com.example.audio.CallRecorder
import com.example.data.db.AppDatabase
import com.example.data.db.CallRecording
import com.example.data.db.RecordingRepository
import com.example.model.VoicemailItem
import com.example.notification.OngoingCallNotificationManager
import com.example.telecom.TelecomCallManager
import com.example.telecom.startCallRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DialerViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = ContactsRepository()
  private val recordingRepository = RecordingRepository(AppDatabase.getDatabase(application).callRecordingDao())
  private val callRecorder = CallRecorder(application)
  val audioPlaybackManager = AudioPlaybackManager(viewModelScope)

  private val _isRecordingActive = MutableStateFlow(false)
  val isRecordingActive: StateFlow<Boolean> = _isRecordingActive.asStateFlow()

  private val _hasRecordAudioPermission = MutableStateFlow(
    ContextCompat.checkSelfPermission(application, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
  )
  val hasRecordAudioPermission: StateFlow<Boolean> = _hasRecordAudioPermission.asStateFlow()

  private val _selectedCallRecordForDetails = MutableStateFlow<CallRecord?>(null)
  val selectedCallRecordForDetails: StateFlow<CallRecord?> = _selectedCallRecordForDetails.asStateFlow()

  val playingRecordingId: StateFlow<Long?> = audioPlaybackManager.playingRecordingId
  val isPlaybackPlaying: StateFlow<Boolean> = audioPlaybackManager.isPlaying
  val playbackPositionMs: StateFlow<Long> = audioPlaybackManager.currentPositionMs
  val playbackTotalDurationMs: StateFlow<Long> = audioPlaybackManager.totalDurationMs

  @OptIn(ExperimentalCoroutinesApi::class)
  val selectedRecordRecordings: StateFlow<List<CallRecording>> = _selectedCallRecordForDetails
    .flatMapLatest { record ->
      if (record == null) {
        flowOf(emptyList())
      } else {
        recordingRepository.getRecordingsForNumber(record.phoneNumber)
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _activeTab = MutableStateFlow(NavTab.KEYPAD)
  val activeTab: StateFlow<NavTab> = _activeTab.asStateFlow()

  private val _dialedDigits = MutableStateFlow("")
  val dialedDigits: StateFlow<String> = _dialedDigits.asStateFlow()

  val formattedNumber: StateFlow<String> = _dialedDigits.map { raw ->
    formatDialerNumber(raw)
  }.flowOn(Dispatchers.Default)
   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

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
      list
    } else {
      val cleanQuery = query.trim()
      list.filter {
        it.name.contains(cleanQuery, ignoreCase = true) || it.phoneNumber.contains(cleanQuery)
      }
    }
  }.flowOn(Dispatchers.Default)
   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.sampleContacts)

  val groupedContacts: StateFlow<Map<Char, List<Contact>>> = filteredContacts.map { list ->
    list.groupBy { it.initial }
  }.flowOn(Dispatchers.Default)
   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val favouriteContacts: StateFlow<List<Contact>> = _contacts.map { list ->
    list.filter { it.isFavorite }
  }.flowOn(Dispatchers.Default)
   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _recents = MutableStateFlow(SampleDataProvider.sampleRecents)
  val recents: StateFlow<List<CallRecord>> = combine(_recents, _recentsFilter) { list, filter ->
    when (filter) {
      RecentsFilter.ALL -> list
      RecentsFilter.MISSED -> list.filter { it.callType == CallType.MISSED }
    }
  }.flowOn(Dispatchers.Default)
   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.sampleRecents)

  private val _voicemails = MutableStateFlow(SampleDataProvider.sampleVoicemails)
  val voicemails: StateFlow<List<VoicemailItem>> = _voicemails.asStateFlow()

  val unreadVoicemailCount: StateFlow<Int> = _voicemails.map { list ->
    list.count { !it.isRead && !it.isDeleted }
  }.flowOn(Dispatchers.Default)
   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

  private val _callSession = MutableStateFlow<CallSession?>(null)
  val callSession: StateFlow<CallSession?> = _callSession.asStateFlow()

  private val _isCallMinimized = MutableStateFlow(false)
  val isCallMinimized: StateFlow<Boolean> = _isCallMinimized.asStateFlow()

  private var callTimerJob: Job? = null
  private var dialingJob: Job? = null
  private var callLogRefreshJob: Job? = null
  private var contactsRefreshJob: Job? = null
  private var callLogObserver: ContentObserver? = null
  private var contactsObserver: ContentObserver? = null
  private val dtmfPlayer = com.example.audio.DtmfPlayer()

  init {
    loadDeviceDataIfPermitted()

    // Initialize Telecom Call Manager with Application Context
    TelecomCallManager.init(application)

    // Connect real Telecom call lookup and state listener
    TelecomCallManager.setContactNameLookup { number ->
      findContactName(number)
    }

    viewModelScope.launch {
      var wasInRealCall = false
      TelecomCallManager.telecomCallSession.collect { realSession ->
        if (realSession != null) {
          if (!wasInRealCall) {
            _isCallMinimized.value = false
            wasInRealCall = true
          }
          _callSession.value = realSession
        } else {
          if (_callSession.value?.isRealCall == true || wasInRealCall) {
            stopRecordingAndSave()
            _callSession.value = null
            _isCallMinimized.value = false
            wasInRealCall = false
            // Real call finished - trigger immediate and staggered refresh of system call logs
            refreshCallLogsAfterCallEnded()
          }
        }
      }
    }

    // Manage persistent ongoing call notification tied to active call session
    viewModelScope.launch {
      _callSession.collect { session ->
        val context = getApplication<Application>()
        if (session != null && (session.state == CallState.ACTIVE || session.state == CallState.HOLDING)) {
          OngoingCallNotificationManager.showOrUpdateNotification(context, session)
        } else {
          OngoingCallNotificationManager.cancelNotification(context)
          if (session == null || session.state == CallState.DISCONNECTED || session.state == CallState.ENDED) {
            stopRecordingAndSave()
          }
        }
      }
    }
  }

  fun loadDeviceDataIfPermitted() {
    val context = getApplication<Application>()
    val hasContacts = repository.hasContactsPermission(context)
    _hasContactsPermission.value = hasContacts
    if (hasContacts) {
      loadRealContacts()
      registerContactsObserver()
    }

    val hasCallLog = repository.hasCallLogPermission(context)
    _hasCallLogPermission.value = hasCallLog
    if (hasCallLog) {
      loadRealCallLogs()
      registerCallLogObserver()
    }
  }

  private fun registerCallLogObserver() {
    if (callLogObserver != null) return
    val context = getApplication<Application>()
    if (!repository.hasCallLogPermission(context)) return

    try {
      callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
          super.onChange(selfChange, uri)
          scheduleCallLogRefresh(200L)
        }
      }
      context.contentResolver.registerContentObserver(
        CallLog.Calls.CONTENT_URI,
        true,
        callLogObserver!!,
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun unregisterCallLogObserver() {
    callLogObserver?.let { observer ->
      try {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      callLogObserver = null
    }
  }

  private fun registerContactsObserver() {
    if (contactsObserver != null) return
    val context = getApplication<Application>()
    if (!repository.hasContactsPermission(context)) return

    try {
      contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
          super.onChange(selfChange, uri)
          scheduleContactsRefresh(500L)
        }
      }
      context.contentResolver.registerContentObserver(
        ContactsContract.Contacts.CONTENT_URI,
        true,
        contactsObserver!!,
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun unregisterContactsObserver() {
    contactsObserver?.let { observer ->
      try {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      contactsObserver = null
    }
  }

  fun scheduleCallLogRefresh(delayMillis: Long = 300L) {
    callLogRefreshJob?.cancel()
    callLogRefreshJob = viewModelScope.launch {
      if (delayMillis > 0) {
        delay(delayMillis)
      }
      loadRealCallLogs()
    }
  }

  fun scheduleContactsRefresh(delayMillis: Long = 500L) {
    contactsRefreshJob?.cancel()
    contactsRefreshJob = viewModelScope.launch {
      if (delayMillis > 0) {
        delay(delayMillis)
      }
      loadRealContacts()
    }
  }

  fun refreshCallLogsAfterCallEnded() {
    viewModelScope.launch {
      // First quick refresh in case provider committed fast
      delay(400)
      loadRealCallLogs()
      // Staggered follow-up to guarantee Android Telecom async write has settled
      delay(1200)
      loadRealCallLogs()
    }
  }

  fun checkCallLogPermission(context: Context): Boolean {
    return repository.hasCallLogPermission(context)
  }

  fun checkContactsPermission(context: Context): Boolean {
    return repository.hasContactsPermission(context)
  }

  fun refreshPermissions(context: Context) {
    val hasContacts = repository.hasContactsPermission(context)
    val hasCall = repository.hasCallLogPermission(context)
    val hasAudio = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    _hasContactsPermission.value = hasContacts
    if (hasContacts) {
      if (_contacts.value.isEmpty()) {
        loadRealContacts()
      }
      registerContactsObserver()
    }
    _hasCallLogPermission.value = hasCall
    if (hasCall) {
      if (_recents.value.isEmpty()) {
        loadRealCallLogsWithRetry()
      }
      registerCallLogObserver()
    }
    _hasRecordAudioPermission.value = hasAudio
  }

  fun onRecordAudioPermissionResult(isGranted: Boolean) {
    _hasRecordAudioPermission.value = isGranted
  }

  fun onStartupPermissionsResult(contactsGranted: Boolean, callLogGranted: Boolean) {
    _hasContactsPermission.value = contactsGranted
    if (contactsGranted) {
      loadRealContacts()
      registerContactsObserver()
    }
    _hasCallLogPermission.value = callLogGranted
    if (callLogGranted) {
      registerCallLogObserver()
      loadRealCallLogsWithRetry()
    }
    val context = getApplication<Application>()
    val hasAudio = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    _hasRecordAudioPermission.value = hasAudio
  }

  fun onContactsPermissionResult(isGranted: Boolean) {
    _hasContactsPermission.value = isGranted
    if (isGranted) {
      loadRealContacts()
      registerContactsObserver()
    }
  }

  fun onCallLogPermissionResult(isGranted: Boolean) {
    _hasCallLogPermission.value = isGranted
    if (isGranted) {
      registerCallLogObserver()
      loadRealCallLogsWithRetry()
    }
  }

  fun loadRealCallLogsWithRetry() {
    viewModelScope.launch {
      loadRealCallLogs()
      delay(350)
      if (_recents.value.isEmpty()) {
        loadRealCallLogs()
      }
      delay(800)
      if (_recents.value.isEmpty()) {
        loadRealCallLogs()
      }
    }
  }

  fun loadRealContacts() {
    val context = getApplication<Application>()
    viewModelScope.launch(Dispatchers.IO) {
      _isLoadingContacts.value = true
      try {
        val deviceList = repository.getDeviceContacts(context)
        CallerNameResolver.clearCache()
        CallerNameResolver.populateCache(deviceList)
        _contacts.value = deviceList
        _isRealDeviceContacts.value = true
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        _isLoadingContacts.value = false
      }
    }
  }

  fun loadRealCallLogs() {
    val context = getApplication<Application>()
    viewModelScope.launch(Dispatchers.IO) {
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
    viewModelScope.launch(Dispatchers.IO) {
      val success = repository.addDeviceContact(context, name, phoneNumber, type)
      if (success) {
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "Contact \"$name\" saved to device!", Toast.LENGTH_SHORT).show()
        }
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
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "Contact \"$name\" added!", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  fun seedSampleContactsToDevice() {
    val context = getApplication<Application>()
    viewModelScope.launch(Dispatchers.IO) {
      _isLoadingContacts.value = true
      val count = repository.seedDemoContactsToDevice(context)
      withContext(Dispatchers.Main) {
        Toast.makeText(context, "Populated $count real contacts to device!", Toast.LENGTH_SHORT).show()
      }
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

    // Immediate in-memory update for 0-latency UI responsiveness
    _contacts.update { list ->
      list.map { contact ->
        if (contact.id == contactId) contact.copy(isFavorite = newFav) else contact
      }
    }

    // Persist to device asynchronously without blocking UI thread
    if (currentContact.isSystemContact) {
      viewModelScope.launch(Dispatchers.IO) {
        repository.toggleFavoriteInDevice(context, contactId, newFav)
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

  // In-app & Cellular Call Actions
  fun startOutgoingCall(name: String? = null, number: String) {
    startCall(name = name, number = number, initialState = CallState.DIALING)
  }

  fun startCall(
    name: String? = null,
    number: String,
    initialState: CallState = CallState.ACTIVE,
  ) {
    if (number.isBlank()) return
    val contactName = name ?: CallerNameResolver.fastResolve(number, _contacts.value) ?: number
    callTimerJob?.cancel()
    dialingJob?.cancel()

    // Trigger real cellular outgoing call
    val isReal = TelecomCallManager.placeOutgoingCall(getApplication(), number)

    _callSession.value = CallSession(
      callerName = contactName,
      phoneNumber = number,
      state = initialState,
      durationSeconds = 0,
      isRealCall = isReal,
      wasAnswered = (initialState == CallState.ACTIVE),
      isIncomingCall = false,
    )
    _isCallMinimized.value = false

    // If caller name was not provided and not yet cached, resolve asynchronously without blocking UI
    if (name == null && (contactName == number || contactName.isBlank())) {
      viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val asyncResolved = CallerNameResolver.resolveCallerName(
          context = getApplication(),
          phoneNumber = number,
          telecomCallerName = null,
          inMemoryContacts = _contacts.value,
        )
        if (asyncResolved.isNotBlank() && asyncResolved != number && asyncResolved != "Unknown") {
          _callSession.update { it?.copy(callerName = asyncResolved) }
        }
      }
    }

    if (initialState == CallState.ACTIVE) {
      startCallTimer()
    } else if (initialState == CallState.DIALING) {
      // In standalone or test environments where InCallService might not bind,
      // simulate realistic ringback connection after 3s
      dialingJob = viewModelScope.launch {
        delay(3000)
        if (_callSession.value?.state == CallState.DIALING) {
          _callSession.update { it?.copy(state = CallState.ACTIVE, wasAnswered = true) }
          startCallTimer()
        }
      }
    }
  }

  fun simulateIncomingCall(name: String = "Pupa Village", number: String = "+1 (555) 890-4321") {
    callTimerJob?.cancel()
    dialingJob?.cancel()
    _callSession.value = CallSession(
      callerName = name,
      phoneNumber = number,
      state = CallState.RINGING,
      durationSeconds = 0,
      wasAnswered = false,
      isIncomingCall = true,
    )
    _isCallMinimized.value = false
  }

  fun acceptCall() {
    TelecomCallManager.answer()
    val current = _callSession.value ?: return
    _callSession.value = current.copy(state = CallState.ACTIVE, wasAnswered = true)
    startCallTimer()
  }

  fun declineCall() {
    TelecomCallManager.reject()
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
    dialingJob?.cancel()
    _callSession.value = null
    _isCallMinimized.value = false
    refreshCallLogsAfterCallEnded()
  }

  fun endCall() {
    stopRecordingAndSave()
    val current = _callSession.value
    TelecomCallManager.endCall()
    if (current != null) {
      val newRecord = CallRecord(
        id = System.currentTimeMillis().toString(),
        contactName = current.callerName,
        phoneNumber = current.phoneNumber,
        callType = if (current.state.isRinging) CallType.MISSED else CallType.OUTGOING,
        timeFormatted = "Just now",
        dateFormatted = "Today",
      )
      _recents.update { listOf(newRecord) + it }
    }
    callTimerJob?.cancel()
    dialingJob?.cancel()

    if (current?.isRealCall == true) {
      // In lockstep with TelecomCallManager, transition to DISCONNECTED.
      // TelecomCallManager will set _telecomCallSession to null after the brief full-opacity confirmation.
      _callSession.update { it?.copy(state = CallState.DISCONNECTED) }
    } else {
      _callSession.value = null
      _isCallMinimized.value = false
    }
    refreshCallLogsAfterCallEnded()
  }

  fun toggleCallRecording() {
    val session = _callSession.value ?: return
    val context = getApplication<Application>()

    // Ensure call is fully connected and active before capturing audio
    if (session.state != CallState.ACTIVE) {
      Toast.makeText(context, "Wait for call to connect", Toast.LENGTH_SHORT).show()
      return
    }

    val hasAudioPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    _hasRecordAudioPermission.value = hasAudioPerm

    if (!hasAudioPerm) {
      Toast.makeText(context, "Microphone permission is required to record calls", Toast.LENGTH_SHORT).show()
      return
    }

    if (_isRecordingActive.value) {
      stopRecordingAndSave()
      return
    }

    // Check CallAudioState route before starting
    val activeCall = TelecomCallManager.getActiveTelecomCall()
    val audioRoute = TelecomCallManager.getCurrentAudioRoute()
    Log.d("DialerViewModel", "toggleCallRecording: CallAudioState.route=$audioRoute, SDK=${Build.VERSION.SDK_INT}")

    // Dual path: on Android 14+ (API 34+), attempt framework Call.startCallRecording()
    if (Build.VERSION.SDK_INT >= 34 && activeCall != null) {
      val frameworkStarted = activeCall.startCallRecording()
      if (frameworkStarted) {
        _isRecordingActive.value = true
        Log.i("DialerViewModel", "Framework Call.startCallRecording() started successfully")
        return
      } else {
        Log.d("DialerViewModel", "Framework Call.startCallRecording() not supported or failed, using MediaRecorder fallback")
      }
    }

    // MediaRecorder fallback using InCallService foreground service context
    startRecording(session.phoneNumber, session.callerName)
  }

  fun startRecording(phoneNumber: String, callerName: String) {
    val started = TelecomCallManager.startCallRecording(phoneNumber, callerName, callRecorder)
    _isRecordingActive.value = started
    if (!started) {
      val context = getApplication<Application>()
      Toast.makeText(context, "Unable to start recording", Toast.LENGTH_SHORT).show()
    }
  }

  fun stopRecordingAndSave() {
    if (!_isRecordingActive.value && !callRecorder.isRecordingActive()) return
    val recording = callRecorder.stopRecording()
    _isRecordingActive.value = false
    if (recording != null) {
      viewModelScope.launch {
        recordingRepository.insertRecording(recording)
      }
    }
  }

  fun openCallDetails(record: CallRecord) {
    _selectedCallRecordForDetails.value = record
  }

  fun closeCallDetails() {
    audioPlaybackManager.stop()
    _selectedCallRecordForDetails.value = null
  }

  fun playRecording(recording: CallRecording) {
    audioPlaybackManager.play(recording)
  }

  fun pausePlayback() {
    audioPlaybackManager.pause()
  }

  fun deleteRecording(recording: CallRecording) {
    if (audioPlaybackManager.playingRecordingId.value == recording.id) {
      audioPlaybackManager.stop()
    }
    viewModelScope.launch {
      recordingRepository.deleteRecording(recording)
    }
  }

  fun minimizeCall() {
    _isCallMinimized.value = true
  }

  fun restoreCall() {
    _isCallMinimized.value = false
  }

  fun toggleMute() {
    TelecomCallManager.toggleMute()
  }

  fun toggleSpeaker() {
    val context = getApplication<Application>()
    TelecomCallManager.toggleAudioEndpoint(context)
  }

  fun toggleHold() {
    TelecomCallManager.toggleHold()
    _callSession.update { current ->
      current?.copy(isOnHold = !current.isOnHold)
    }
  }

  fun sendDtmfTone(char: Char) {
    playDtmfTone(char)
    TelecomCallManager.playDtmfTone(char)
  }

  fun stopDtmfTone() {
    TelecomCallManager.stopDtmfTone()
  }

  fun toggleKeypad() {
    _callSession.update { current ->
      current?.copy(isKeypadOpen = !current.isKeypadOpen)
    }
  }

  // Phase 4D: Second Call, Swap, Merge & Call Waiting
  fun addSecondCall(number: String) {
    if (number.isBlank()) return
    TelecomCallManager.addSecondCall(getApplication(), number)
  }

  fun swapCalls() {
    TelecomCallManager.swapCalls()
    _callSession.update { current ->
      if (current != null && current.heldCallName != null) {
        val oldActiveName = current.callerName
        val oldActiveNumber = current.phoneNumber
        current.copy(
          callerName = current.heldCallName,
          phoneNumber = current.heldCallNumber ?: "",
          heldCallName = oldActiveName,
          heldCallNumber = oldActiveNumber,
          isOnHold = false,
        )
      } else {
        current
      }
    }
  }

  fun mergeCalls() {
    TelecomCallManager.mergeCalls()
    _callSession.update { current ->
      current?.copy(
        callerName = "Conference (${listOfNotNull(current.callerName, current.heldCallName).joinToString(", ")})",
        isConference = true,
        canMergeCalls = false,
        canSwapCalls = false,
        heldCallName = null,
        heldCallNumber = null,
      )
    }
  }

  fun acceptWaitingCall(holdCurrent: Boolean = true) {
    TelecomCallManager.acceptWaitingCall(holdCurrent)
    _callSession.update { current ->
      if (current?.hasWaitingCall == true) {
        val waitName = current.waitingCallName ?: "Caller 2"
        val waitNumber = current.waitingCallNumber ?: ""
        if (holdCurrent) {
          current.copy(
            heldCallName = current.callerName,
            heldCallNumber = current.phoneNumber,
            callerName = waitName,
            phoneNumber = waitNumber,
            hasWaitingCall = false,
            waitingCallName = null,
            waitingCallNumber = null,
            canSwapCalls = true,
            canMergeCalls = true,
          )
        } else {
          current.copy(
            callerName = waitName,
            phoneNumber = waitNumber,
            hasWaitingCall = false,
            waitingCallName = null,
            waitingCallNumber = null,
            heldCallName = null,
            heldCallNumber = null,
          )
        }
      } else {
        current
      }
    }
  }

  fun rejectWaitingCall() {
    TelecomCallManager.rejectWaitingCall()
    _callSession.update { current ->
      current?.copy(
        hasWaitingCall = false,
        waitingCallName = null,
        waitingCallNumber = null,
      )
    }
  }

  fun simulateWaitingCall(name: String = "Sarah Connor", number: String = "+1 (555) 765-4321") {
    _callSession.update { current ->
      current?.copy(
        waitingCallName = name,
        waitingCallNumber = number,
        hasWaitingCall = true,
      )
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

  fun findContactName(number: String): String? {
    val context = getApplication<Application>()
    val resolved = CallerNameResolver.resolveCallerName(
      context = context,
      phoneNumber = number,
      telecomCallerName = null,
      inMemoryContacts = _contacts.value,
    )
    return if (resolved.isNotBlank() && resolved != number && resolved != "Unknown") {
      resolved
    } else {
      null
    }
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
    stopRecordingAndSave()
    audioPlaybackManager.stop()
    unregisterCallLogObserver()
    unregisterContactsObserver()
    callLogRefreshJob?.cancel()
    contactsRefreshJob?.cancel()
    callTimerJob?.cancel()
    dtmfPlayer.release()
  }
}
