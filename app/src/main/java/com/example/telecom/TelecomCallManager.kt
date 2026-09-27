package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.data.CallerNameResolver
import com.example.model.CallSession
import com.example.model.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object TelecomCallManager {

  private val scope = CoroutineScope(Dispatchers.Main)

  // Tracking all active cellular calls (Primary, Held, Waiting, Conference)
  private val callList = mutableListOf<Call>()
  private var activeCall: Call? = null
  private var heldCall: Call? = null
  private var waitingCall: Call? = null
  private var isConferenceState = false

  // Track incoming ringing calls vs answered/outgoing calls
  private val incomingRingingCalls = mutableSetOf<Call>()
  private val answeredCalls = mutableSetOf<Call>()

  private var inCallService: InCallService? = null
  private var contactNameLookup: ((String) -> String?)? = null

  // API 34+ CallEndpoint state
  private var currentCallEndpoint: Any? = null
  private var availableCallEndpoints: List<Any> = emptyList()

  // API < 34 legacy audio state
  private var currentCallAudioState: CallAudioState? = null

  private val _telecomCallSession = MutableStateFlow<CallSession?>(null)
  val telecomCallSession: StateFlow<CallSession?> = _telecomCallSession.asStateFlow()

  private var callDurationJob: Job? = null
  private var disconnectionCleanupJob: Job? = null
  private var dtmfStopJob: Job? = null

  private val callCallback = object : Call.Callback() {
    override fun onStateChanged(call: Call, state: Int) {
      handleStateChange(call, state)
    }

    override fun onDetailsChanged(call: Call, details: Call.Details) {
      updateCallDetails(call, details)
    }
  }

  private var appContext: Context? = null

  fun init(context: Context) {
    appContext = context.applicationContext
  }

  fun setContactNameLookup(lookup: (String) -> String?) {
    contactNameLookup = lookup
  }

  fun registerInCallService(service: InCallService) {
    inCallService = service
    appContext = service.applicationContext
  }

  fun unregisterInCallService(service: InCallService) {
    if (inCallService == service) {
      inCallService = null
      currentCallEndpoint = null
      availableCallEndpoints = emptyList()
      currentCallAudioState = null
      callList.clear()
      incomingRingingCalls.clear()
      answeredCalls.clear()
      activeCall = null
      heldCall = null
      waitingCall = null
      isConferenceState = false
    }
  }

  // ==========================================
  // CALL LIFECYCLE (MULTI-CALL TRACKING)
  // ==========================================

  fun onCallAdded(call: Call, service: InCallService) {
    inCallService = service
    if (!callList.contains(call)) {
      callList.add(call)
    }
    call.registerCallback(callCallback)
    disconnectionCleanupJob?.cancel()

    if (call.state == Call.STATE_RINGING) {
      incomingRingingCalls.add(call)
      if (activeCall != null || heldCall != null) {
        // Second incoming call -> Call Waiting
        waitingCall = call
      } else {
        activeCall = call
      }
    } else {
      if (call.state == Call.STATE_ACTIVE) {
        answeredCalls.add(call)
      }
      // Outgoing or active call
      if (activeCall != null && activeCall != call) {
        // Automatically put existing active call on hold
        try {
          activeCall?.hold()
        } catch (e: Exception) {
          e.printStackTrace()
        }
        heldCall = activeCall
        activeCall = call
      } else if (activeCall == null) {
        activeCall = call
      }
    }

    refreshMultiCallSessionState()

    // Resolve caller name in background if needed to ensure local saved contact name takes priority
    scope.launch(Dispatchers.IO) {
      val num = extractPhoneNumber(call)
      if (num.isNotBlank()) {
        val resolved = CallerNameResolver.resolveCallerName(service, num, null)
        if (resolved.isNotBlank() && resolved != num && resolved != "Unknown") {
          refreshMultiCallSessionState()
        }
      }
    }
  }

  fun onCallRemoved(call: Call) {
    val wasIncomingNeverAnswered = incomingRingingCalls.contains(call) && !answeredCalls.contains(call)
    incomingRingingCalls.remove(call)
    val wasAnswered = answeredCalls.remove(call)

    call.unregisterCallback(callCallback)
    callList.remove(call)

    if (waitingCall == call) {
      waitingCall = null
    }

    if (heldCall == call) {
      heldCall = null
    }

    if (activeCall == call) {
      activeCall = null
      // Automatically unhold the held call if one remains
      if (heldCall != null) {
        val nextActive = heldCall
        heldCall = null
        activeCall = nextActive
        try {
          nextActive?.unhold()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }

    if (callList.isEmpty()) {
      isConferenceState = false
      callDurationJob?.cancel()
      dtmfStopJob?.cancel()

      if (wasIncomingNeverAnswered) {
        // Declined / missed incoming call that was never answered: dismiss immediately with no "Call Ended" flash
        disconnectionCleanupJob?.cancel()
        _telecomCallSession.value = null
      } else {
        _telecomCallSession.update { current ->
          current?.copy(
            state = CallState.DISCONNECTED,
            heldCallName = null,
            heldCallNumber = null,
            canSwapCalls = false,
            canMergeCalls = false,
            isConference = false,
            waitingCallName = null,
            waitingCallNumber = null,
            hasWaitingCall = false,
            wasAnswered = wasAnswered,
          )
        }

        // Keep DISCONNECTED status visible briefly ("Call Ended") for answered/outgoing calls at full opacity before resetting
        if (disconnectionCleanupJob?.isActive != true) {
          disconnectionCleanupJob = scope.launch {
            delay(600)
            _telecomCallSession.value = null
          }
        }
      }
    } else {
      refreshMultiCallSessionState()
    }
  }

  private fun handleStateChange(call: Call, state: Int) {
    when (state) {
      Call.STATE_ACTIVE -> {
        answeredCalls.add(call)
        incomingRingingCalls.remove(call)
        if (waitingCall == call) {
          waitingCall = null
        }
        activeCall = call
        val other = callList.firstOrNull { it != call && it.state == Call.STATE_HOLDING }
        if (other != null) {
          heldCall = other
        }
      }
      Call.STATE_HOLDING -> {
        answeredCalls.add(call)
        if (activeCall == call) {
          heldCall = call
          val other = callList.firstOrNull { it != call && it.state == Call.STATE_ACTIVE }
          if (other != null) {
            activeCall = other
          }
        } else {
          heldCall = call
        }
      }
      Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
        if (waitingCall == call) waitingCall = null
        val wasIncomingNeverAnswered = incomingRingingCalls.contains(call) && !answeredCalls.contains(call)
        if (wasIncomingNeverAnswered) {
          incomingRingingCalls.remove(call)
          if (activeCall == call) activeCall = null
          if (heldCall == call) heldCall = null
          callList.remove(call)
          if (callList.isEmpty()) {
            disconnectionCleanupJob?.cancel()
            _telecomCallSession.value = null
            return
          }
        }
      }
    }

    if (call.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true) {
      isConferenceState = true
    }

    refreshMultiCallSessionState()
  }

  private fun updateCallDetails(call: Call, details: Call.Details) {
    if (details.hasProperty(Call.Details.PROPERTY_CONFERENCE)) {
      isConferenceState = true
    }
    refreshMultiCallSessionState()
  }

  private fun refreshMultiCallSessionState() {
    val primary = activeCall ?: heldCall ?: waitingCall ?: callList.firstOrNull()
    if (primary == null) {
      return
    }

    val isIncoming = incomingRingingCalls.contains(primary)
    val wasAnswered = answeredCalls.contains(primary) || primary.state == Call.STATE_ACTIVE || primary.state == Call.STATE_HOLDING

    val primaryState = when (primary.state) {
      Call.STATE_RINGING -> CallState.RINGING
      Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.DIALING
      Call.STATE_ACTIVE -> CallState.ACTIVE
      Call.STATE_HOLDING -> CallState.HOLDING
      Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallState.DISCONNECTED
      else -> CallState.ACTIVE
    }

    val primaryNumber = extractPhoneNumber(primary)
    val primaryName = extractCallerName(primary, primaryNumber)

    val secondary = heldCall?.takeIf { it != primary }
      ?: callList.firstOrNull { it != primary && it != waitingCall }
    val heldNumber = secondary?.let { extractPhoneNumber(it) }
    val heldName = secondary?.let { extractCallerName(it, heldNumber ?: "") }

    val waiting = waitingCall
    val waitNumber = waiting?.let { extractPhoneNumber(it) }
    val waitName = waiting?.let { extractCallerName(it, waitNumber ?: "") }

    val canSwap = (activeCall != null && heldCall != null) ||
      (primary.details?.can(Call.Details.CAPABILITY_SWAP_CONFERENCE) == true)

    val canMerge = (activeCall != null && heldCall != null && !isConferenceState) ||
      (primary.details?.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) == true)

    val isHold = primary.state == Call.STATE_HOLDING && secondary == null

    _telecomCallSession.update { existing ->
      val hasBeenAnswered = existing?.wasAnswered == true || wasAnswered
      if (existing != null) {
        existing.copy(
          callerName = if (isConferenceState) "Conference Call" else primaryName,
          phoneNumber = primaryNumber.ifBlank { existing.phoneNumber },
          state = primaryState,
          isOnHold = isHold,
          heldCallName = heldName,
          heldCallNumber = heldNumber,
          canSwapCalls = canSwap,
          canMergeCalls = canMerge,
          isConference = isConferenceState,
          waitingCallName = waitName,
          waitingCallNumber = waitNumber,
          hasWaitingCall = waiting != null,
          isRealCall = true,
          wasAnswered = hasBeenAnswered,
          isIncomingCall = isIncoming,
        )
      } else {
        CallSession(
          callerName = if (isConferenceState) "Conference Call" else primaryName,
          phoneNumber = primaryNumber,
          state = primaryState,
          durationSeconds = 0,
          isOnHold = isHold,
          heldCallName = heldName,
          heldCallNumber = heldNumber,
          canSwapCalls = canSwap,
          canMergeCalls = canMerge,
          isConference = isConferenceState,
          waitingCallName = waitName,
          waitingCallNumber = waitNumber,
          hasWaitingCall = waiting != null,
          isRealCall = true,
          wasAnswered = hasBeenAnswered,
          isIncomingCall = isIncoming,
        )
      }
    }

    if (primaryState == CallState.ACTIVE && !isHold) {
      startDurationTimer()
    } else if (primaryState == CallState.DISCONNECTED && callList.isEmpty()) {
      callDurationJob?.cancel()
    }
  }

  private fun extractPhoneNumber(call: Call): String {
    val handle = call.details?.handle?.schemeSpecificPart
    if (!handle.isNullOrBlank()) return handle

    val gateway = call.details?.gatewayInfo?.originalAddress?.schemeSpecificPart
    if (!gateway.isNullOrBlank()) return gateway

    val extrasNumber = call.details?.extras?.getString(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)
      ?: call.details?.intentExtras?.getString("android.telephony.extra.INCOMING_NUMBER")
    if (!extrasNumber.isNullOrBlank()) return extrasNumber

    return ""
  }

  private fun extractCallerName(call: Call, phoneNumber: String): String {
    val telecomCallerName = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }

    // Priority 1 & 2: Local Device Contacts lookup (exact or normalized match)
    val resolvedLocal = contactNameLookup?.invoke(phoneNumber)
      ?: inCallService?.let { CallerNameResolver.resolveCallerName(it, phoneNumber, null) }

    if (!resolvedLocal.isNullOrBlank() && resolvedLocal != phoneNumber && resolvedLocal != "Unknown") {
      return resolvedLocal
    }

    // Priority 3: Telecom-provided contact/caller-ID name
    if (!telecomCallerName.isNullOrBlank() && telecomCallerName != phoneNumber && telecomCallerName != "Unknown") {
      return telecomCallerName
    }

    // Priority 4: Raw phone number as final fallback
    return phoneNumber.ifBlank { "Unknown" }
  }

  private fun startDurationTimer() {
    if (callDurationJob?.isActive == true) return
    callDurationJob = scope.launch {
      while (true) {
        delay(1000)
        _telecomCallSession.update { current ->
          if (current?.state == CallState.ACTIVE && !current.isOnHold) {
            current.copy(durationSeconds = current.durationSeconds + 1)
          } else {
            current
          }
        }
      }
    }
  }

  // ==========================================
  // PHASE 4D: SECOND CALL, CALL WAITING, SWAP & MERGE
  // ==========================================

  /**
   * Starts a second real cellular call while placing the current call on hold.
   */
  fun addSecondCall(context: Context, number: String): Boolean {
    val cleanNumber = number.trim()
    if (cleanNumber.isEmpty()) return false

    // Hold the active call
    if (activeCall != null) {
      try {
        activeCall?.hold()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      heldCall = activeCall
    }

    val currentSession = _telecomCallSession.value
    val heldName = currentSession?.callerName ?: "Call 1"
    val heldNum = currentSession?.phoneNumber ?: ""

    // Update session immediately to show second call dialing with first call held
    val newCallerName = contactNameLookup?.invoke(cleanNumber) ?: cleanNumber
    _telecomCallSession.update { current ->
      current?.copy(
        callerName = newCallerName,
        phoneNumber = cleanNumber,
        state = CallState.DIALING,
        durationSeconds = 0,
        heldCallName = heldName,
        heldCallNumber = heldNum,
        canSwapCalls = true,
        canMergeCalls = true,
        isConference = false,
      ) ?: CallSession(
        callerName = newCallerName,
        phoneNumber = cleanNumber,
        state = CallState.DIALING,
        durationSeconds = 0,
        heldCallName = heldName,
        heldCallNumber = heldNum,
        canSwapCalls = true,
        canMergeCalls = true,
        isConference = false,
        isRealCall = true,
      )
    }

    return placeOutgoingCall(context, cleanNumber)
  }

  /**
   * Switches / swaps between the active call and the held call.
   */
  fun swapCalls() {
    val active = activeCall
    val held = heldCall

    // 1. Telecom swap conference capability
    if (active?.details?.can(Call.Details.CAPABILITY_SWAP_CONFERENCE) == true) {
      try {
        active.swapConference()
        return
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    // 2. Standard hold & unhold swap
    if (active != null && held != null) {
      try {
        active.hold()
        held.unhold()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      activeCall = held
      heldCall = active
      refreshMultiCallSessionState()
      return
    }

    // 3. Fallback / simulated swap
    _telecomCallSession.update { current ->
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

  /**
   * Merges the two calls into a single conference call when supported.
   */
  fun mergeCalls() {
    val active = activeCall
    val held = heldCall

    if (active?.details?.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) == true) {
      try {
        active.mergeConference()
        isConferenceState = true
        refreshMultiCallSessionState()
        return
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    if (active != null && held != null) {
      try {
        active.conference(held)
        isConferenceState = true
        refreshMultiCallSessionState()
        return
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    // Fallback in-app conference state
    isConferenceState = true
    _telecomCallSession.update { current ->
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

  /**
   * Answers a waiting incoming call.
   * If holdCurrent is true, the active call is placed on hold.
   * If false, the active call is ended first.
   */
  fun acceptWaitingCall(holdCurrent: Boolean = true) {
    val waiting = waitingCall

    if (holdCurrent) {
      try {
        activeCall?.hold()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      heldCall = activeCall
    } else {
      try {
        activeCall?.disconnect()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      activeCall = null
    }

    if (waiting != null) {
      try {
        waiting.answer(VideoProfile.STATE_AUDIO_ONLY)
        activeCall = waiting
        waitingCall = null
      } catch (e: Exception) {
        e.printStackTrace()
      }
      refreshMultiCallSessionState()
    } else {
      // Standalone simulation fallback
      _telecomCallSession.update { current ->
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
  }

  /**
   * Rejects / sends to voicemail the waiting incoming call without affecting the active call.
   */
  fun rejectWaitingCall() {
    val waiting = waitingCall
    if (waiting != null) {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          waiting.reject(Call.REJECT_REASON_DECLINED)
        } else {
          @Suppress("DEPRECATION")
          waiting.reject(false, null)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
      waitingCall = null
      refreshMultiCallSessionState()
    } else {
      // Standalone simulation fallback
      _telecomCallSession.update { current ->
        current?.copy(
          hasWaitingCall = false,
          waitingCallName = null,
          waitingCallNumber = null,
        )
      }
    }
  }

  // ==========================================
  // AUDIO CONTROLS (PHASE 4C)
  // ==========================================

  fun toggleMute(): Boolean {
    val currentMute = _telecomCallSession.value?.isMuted ?: false
    val newMute = !currentMute
    setMuted(newMute)
    return newMute
  }

  fun setMuted(muted: Boolean) {
    try {
      inCallService?.setMuted(muted)
      _telecomCallSession.update { it?.copy(isMuted = muted) }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun toggleAudioEndpoint(context: Context) {
    val service = inCallService
    if (service == null) {
      _telecomCallSession.update { current ->
        current?.copy(isSpeaker = !current.isSpeaker)
      }
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      switchCallEndpointApi34(service, context)
    } else {
      switchAudioRouteLegacy(service)
    }
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  private fun switchCallEndpointApi34(service: InCallService, context: Context) {
    val endpoints = availableCallEndpoints.filterIsInstance<CallEndpoint>()
    if (endpoints.isEmpty()) return

    val current = currentCallEndpoint as? CallEndpoint
    val nextEndpoint = getNextEndpointApi34(current, endpoints) ?: return

    val executor = ContextCompat.getMainExecutor(context)
    val callback = object : OutcomeReceiver<Void, CallEndpointException> {
      override fun onResult(result: Void?) {
        currentCallEndpoint = nextEndpoint
        syncEndpointSessionState()
      }
      override fun onError(error: CallEndpointException) {
        error.printStackTrace()
      }
    }

    try {
      service.requestCallEndpointChange(nextEndpoint, executor, callback)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  private fun getNextEndpointApi34(
    current: CallEndpoint?,
    endpoints: List<CallEndpoint>,
  ): CallEndpoint? {
    if (endpoints.size == 1) return endpoints.first()

    val currentType = current?.endpointType ?: CallEndpoint.TYPE_EARPIECE
    val speaker = endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_SPEAKER }
    val bluetooth = endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_BLUETOOTH }
    val earpiece = endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_EARPIECE }
      ?: endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_WIRED_HEADSET }

    return when (currentType) {
      CallEndpoint.TYPE_EARPIECE, CallEndpoint.TYPE_WIRED_HEADSET -> speaker ?: bluetooth ?: earpiece
      CallEndpoint.TYPE_SPEAKER -> bluetooth ?: earpiece ?: speaker
      CallEndpoint.TYPE_BLUETOOTH -> earpiece ?: speaker ?: bluetooth
      else -> endpoints.firstOrNull { it != current } ?: endpoints.first()
    }
  }

  private fun switchAudioRouteLegacy(service: InCallService) {
    val audioState = currentCallAudioState
    val currentSession = _telecomCallSession.value
    val isCurrentlySpeaker = currentSession?.isSpeaker == true
    val isBtAvail = currentSession?.isBluetoothAvailable == true ||
      (audioState?.supportedRouteMask?.and(CallAudioState.ROUTE_BLUETOOTH) != 0)
    val isCurrentlyBt = currentSession?.isBluetoothActive == true ||
      (audioState?.route?.and(CallAudioState.ROUTE_BLUETOOTH) != 0)

    val targetRoute = when {
      isBtAvail && !isCurrentlySpeaker && !isCurrentlyBt -> CallAudioState.ROUTE_SPEAKER
      isBtAvail && isCurrentlySpeaker -> CallAudioState.ROUTE_BLUETOOTH
      isBtAvail && isCurrentlyBt -> CallAudioState.ROUTE_EARPIECE
      isCurrentlySpeaker -> CallAudioState.ROUTE_EARPIECE
      else -> CallAudioState.ROUTE_SPEAKER
    }

    try {
      @Suppress("DEPRECATION")
      service.setAudioRoute(targetRoute)
      val isSpeaker = targetRoute == CallAudioState.ROUTE_SPEAKER
      val isBt = targetRoute == CallAudioState.ROUTE_BLUETOOTH
      _telecomCallSession.update { current ->
        current?.copy(
          isSpeaker = isSpeaker,
          isBluetoothActive = isBt,
          audioEndpointName = if (isBt) "Bluetooth" else if (isSpeaker) "speaker" else "earpiece",
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setSpeaker(speaker: Boolean) {
    try {
      val service = inCallService
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && service != null) {
        val endpoints = availableCallEndpoints.filterIsInstance<CallEndpoint>()
        val target = if (speaker) {
          endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_SPEAKER }
        } else {
          endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_EARPIECE }
            ?: endpoints.firstOrNull { it.endpointType == CallEndpoint.TYPE_WIRED_HEADSET }
        }
        if (target != null) {
          val executor = ContextCompat.getMainExecutor(service)
          service.requestCallEndpointChange(
            target,
            executor,
            object : OutcomeReceiver<Void, CallEndpointException> {
              override fun onResult(result: Void?) {
                currentCallEndpoint = target
                syncEndpointSessionState()
              }
              override fun onError(error: CallEndpointException) {
                error.printStackTrace()
              }
            },
          )
          return
        }
      }

      val route = if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
      @Suppress("DEPRECATION")
      service?.setAudioRoute(route)
      _telecomCallSession.update { it?.copy(isSpeaker = speaker) }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun toggleHold(): Boolean {
    val currentHold = _telecomCallSession.value?.isOnHold ?: false
    val newHold = !currentHold
    setHold(newHold)
    return newHold
  }

  fun setHold(hold: Boolean) {
    try {
      val callToHold = activeCall ?: heldCall
      if (hold) {
        callToHold?.hold()
      } else {
        callToHold?.unhold()
      }
      _telecomCallSession.update { it?.copy(isOnHold = hold) }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun playDtmfTone(char: Char) {
    try {
      val call = activeCall ?: heldCall
      call?.playDtmfTone(char)
      dtmfStopJob?.cancel()
      dtmfStopJob = scope.launch {
        delay(200)
        call?.stopDtmfTone()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun stopDtmfTone() {
    try {
      dtmfStopJob?.cancel()
      val call = activeCall ?: heldCall
      call?.stopDtmfTone()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // Pre-API 34 Audio state change
  fun onAudioStateChanged(audioState: CallAudioState) {
    currentCallAudioState = audioState

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && currentCallEndpoint != null) {
      return
    }

    val isMuted = audioState.isMuted
    val isSpeaker = (audioState.route and CallAudioState.ROUTE_SPEAKER) != 0
    val isBtActive = (audioState.route and CallAudioState.ROUTE_BLUETOOTH) != 0
    val isBtAvailable = (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0

    val endpointName = when {
      isBtActive -> "Bluetooth"
      isSpeaker -> "speaker"
      else -> "earpiece"
    }

    _telecomCallSession.update { current ->
      current?.copy(
        isMuted = isMuted,
        isSpeaker = isSpeaker,
        isBluetoothActive = isBtActive,
        isBluetoothAvailable = isBtAvailable,
        audioEndpointName = endpointName,
      )
    }
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
    currentCallEndpoint = callEndpoint
    syncEndpointSessionState()
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint>) {
    availableCallEndpoints = availableEndpoints
    syncEndpointSessionState()
  }

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  fun onMuteStateChanged(isMuted: Boolean) {
    _telecomCallSession.update { current ->
      current?.copy(isMuted = isMuted)
    }
  }

  private fun syncEndpointSessionState() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val endpoint = currentCallEndpoint as? CallEndpoint
      val available = availableCallEndpoints.filterIsInstance<CallEndpoint>()

      val isSpeaker = endpoint?.endpointType == CallEndpoint.TYPE_SPEAKER
      val isBtActive = endpoint?.endpointType == CallEndpoint.TYPE_BLUETOOTH
      val isBtAvailable = available.any { it.endpointType == CallEndpoint.TYPE_BLUETOOTH }

      val endpointName = when (endpoint?.endpointType) {
        CallEndpoint.TYPE_BLUETOOTH -> endpoint.endpointName.toString().ifBlank { "Bluetooth" }
        CallEndpoint.TYPE_SPEAKER -> "speaker"
        CallEndpoint.TYPE_WIRED_HEADSET -> "headset"
        else -> "earpiece"
      }

      _telecomCallSession.update { current ->
        current?.copy(
          isSpeaker = isSpeaker,
          isBluetoothActive = isBtActive,
          isBluetoothAvailable = isBtAvailable,
          audioEndpointName = endpointName,
        )
      }
    }
  }

  // Answer & End
  fun answer() {
    try {
      val call = waitingCall ?: activeCall ?: heldCall
      call?.let {
        answeredCalls.add(it)
        incomingRingingCalls.remove(it)
        it.answer(VideoProfile.STATE_AUDIO_ONLY)
      }
      if (call == waitingCall) {
        activeCall = waitingCall
        waitingCall = null
      }
      refreshMultiCallSessionState()
      startDurationTimer()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun reject() {
    try {
      val call = waitingCall ?: activeCall ?: heldCall
      call?.let {
        incomingRingingCalls.remove(it)
        answeredCalls.remove(it)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          it.reject(Call.REJECT_REASON_DECLINED)
        } else {
          @Suppress("DEPRECATION")
          it.reject(false, null)
        }
      }
      if (call == waitingCall) {
        waitingCall = null
      } else if (call == activeCall) {
        activeCall = null
      } else if (call == heldCall) {
        heldCall = null
      }
      if (call != null) {
        callList.remove(call)
      }

      if (callList.isEmpty()) {
        disconnectionCleanupJob?.cancel()
        _telecomCallSession.value = null
      } else {
        refreshMultiCallSessionState()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun endCall() {
    try {
      if (activeCall != null) {
        activeCall?.disconnect()
        // If there is a held call, unhold it to continue
        if (heldCall != null) {
          val next = heldCall
          heldCall = null
          activeCall = next
          next?.unhold()
          refreshMultiCallSessionState()
          return
        }
      } else if (heldCall != null) {
        heldCall?.disconnect()
      } else if (callList.isNotEmpty()) {
        callList.forEach { it.disconnect() }
      }
      callDurationJob?.cancel()
      dtmfStopJob?.cancel()
      disconnectionCleanupJob?.cancel()
      _telecomCallSession.update { it?.copy(state = CallState.DISCONNECTED) }
      disconnectionCleanupJob = scope.launch {
        delay(600)
        _telecomCallSession.value = null
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun placeOutgoingCall(context: Context, number: String): Boolean {
    val cleanNumber = number.trim()
    if (cleanNumber.isEmpty()) return false

    val uri = Uri.fromParts("tel", cleanNumber, null)
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    val hasCallPhonePermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CALL_PHONE,
    ) == PackageManager.PERMISSION_GRANTED

    // 1. Attempt TelecomManager.placeCall directly if permitted
    if (hasCallPhonePermission && telecomManager != null) {
      try {
        val extras = Bundle()
        telecomManager.placeCall(uri, extras)
        return true
      } catch (e: SecurityException) {
        e.printStackTrace()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    // 2. Fallback to Intent.ACTION_CALL / ACTION_DIAL
    return try {
      val intent = if (hasCallPhonePermission) {
        Intent(Intent.ACTION_CALL, uri)
      } else {
        Intent(Intent.ACTION_DIAL, uri)
      }
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }
}
