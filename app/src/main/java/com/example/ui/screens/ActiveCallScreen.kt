package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.CallSession
import com.example.model.CallState
import com.example.ui.components.CallActionButton
import com.example.ui.components.CallActionType
import com.example.ui.components.KeypadButton
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

/**
 * Active Call Screen with Liquid Glass controls, Multi-call / Second Call handling,
 * Hold, Swap, Merge Conference, and Call Waiting.
 */
@Composable
fun ActiveCallScreen(
  callSession: CallSession,
  onEndCall: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleKeypad: () -> Unit,
  onMinimize: () -> Unit,
  modifier: Modifier = Modifier,
  onRecordCall: () -> Unit = {},
  onAddCall: (String) -> Unit = {},
  onToggleHold: () -> Unit = {},
  onDtmfTone: (Char) -> Unit = {},
  onSwapCalls: () -> Unit = {},
  onMergeCalls: () -> Unit = {},
  onAcceptWaitingCall: (Boolean) -> Unit = {},
  onRejectWaitingCall: () -> Unit = {},
) {
  val colors = LocalIosColors.current
  var isRecordingActive by remember { mutableStateOf(false) }
  var isHoldActive by remember { mutableStateOf(false) }
  var showAddCallDialog by remember { mutableStateOf(false) }

  // Background adapts to system theme
  val bgGradient = remember(colors.isDark) {
    if (colors.isDark) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFF1C1D24),
          Color(0xFF0F1014),
          Color(0xFF000000),
        ),
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFFFFFFFF),
          Color(0xFFF7F8FA),
          Color(0xFFECEEF3),
        ),
      )
    }
  }

  val headerTextColor = if (colors.isDark) Color.White else colors.textPrimary
  val isHoldEffective = isHoldActive || callSession.isOnHold || callSession.state.isHolding
  val statusTextColor = if (isHoldEffective) {
    Color(0xFFFF9500)
  } else {
    if (colors.isDark) Color(0xFFAAAAAA) else colors.textSecondary
  }

  val statusText = when (callSession.state) {
    CallState.DIALING -> "calling..."
    CallState.HOLDING -> "on hold"
    CallState.DISCONNECTED, CallState.ENDED -> "Call Ended"
    else -> if (isHoldEffective && callSession.heldCallName == null) "on hold" else callSession.formattedDuration
  }

  // Intercept system/hardware back button to cleanly minimize the call
  BackHandler(enabled = true) {
    onMinimize()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(bgGradient)
      .statusBarsPadding()
      .padding(horizontal = 20.dp, vertical = 16.dp),
  ) {
    // 1. Top Bar: Minimize Chevron
    IconButton(
      onClick = onMinimize,
      modifier = Modifier
        .align(Alignment.TopStart)
        .testTag("active_call_minimize_button"),
    ) {
      Icon(
        imageVector = Icons.Filled.KeyboardArrowDown,
        contentDescription = "Minimize Call",
        tint = headerTextColor,
        modifier = Modifier.size(32.dp),
      )
    }

    // 2. Upper Header: Caller Name, Held Call indicator, & Duration
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Held Call pill (Tap to Swap calls)
      if (callSession.heldCallName != null) {
        Surface(
          shape = CircleShape,
          color = if (colors.isDark) Color(0x33FFFFFF) else Color(0x1F000000),
          modifier = Modifier
            .clickable { onSwapCalls() }
            .padding(bottom = 8.dp)
            .testTag("active_call_swap_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${callSession.heldCallName} (on hold) • swap",
              color = Color(0xFFFF9500),
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Text(
        text = callSession.callerName,
        color = headerTextColor,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = statusText,
        color = statusTextColor,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
      )
    }

    // 3. Center Area: Either In-Call DTMF Keypad or 6-Control Liquid Glass Grid
    if (callSession.isKeypadOpen) {
      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val dtmfKeys = listOf(
          listOf("1" to "", "2" to "ABC", "3" to "DEF"),
          listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
          listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
          listOf("*" to "", "0" to "+", "#" to ""),
        )

        dtmfKeys.forEach { row ->
          Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            row.forEach { (digit, letters) ->
              KeypadButton(
                digit = digit,
                letters = letters,
                size = 68.dp,
                onDigitClick = {
                  onDtmfTone(digit.first())
                },
              )
            }
          }
        }

        TextButton(
          onClick = onToggleKeypad,
          modifier = Modifier.padding(top = 8.dp),
        ) {
          Text(text = "Hide", color = IosBlue, fontSize = 16.sp)
        }
      }
    } else {
      // 3-Column x 2-Row Liquid Glass Control Grid
      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxWidth()
          .padding(top = 60.dp, start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // ROW 1: Mute | Keypad | Speaker
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ActiveCallGlassButton(
            icon = if (callSession.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            label = stringResource(id = R.string.call_mute),
            isActive = callSession.isMuted,
            isDark = colors.isDark,
            onClick = onToggleMute,
            testTag = "call_control_mute",
          )

          ActiveCallGlassButton(
            icon = Icons.Filled.Dialpad,
            label = stringResource(id = R.string.call_keypad),
            isActive = false,
            isDark = colors.isDark,
            onClick = onToggleKeypad,
            testTag = "call_control_keypad",
          )

          ActiveCallGlassButton(
            icon = when {
              callSession.isBluetoothActive -> Icons.Filled.BluetoothAudio
              callSession.isSpeaker -> Icons.AutoMirrored.Filled.VolumeUp
              else -> Icons.AutoMirrored.Filled.VolumeUp
            },
            label = when {
              callSession.isBluetoothActive -> "Bluetooth"
              callSession.isSpeaker -> stringResource(id = R.string.call_speaker)
              else -> callSession.audioEndpointName.ifBlank { stringResource(id = R.string.call_speaker) }
            },
            isActive = callSession.isSpeaker || callSession.isBluetoothActive,
            isDark = colors.isDark,
            onClick = onToggleSpeaker,
            testTag = "call_control_speaker",
          )
        }

        // ROW 2: Record Call | Add Call / Swap / Merge | Hold
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ActiveCallGlassButton(
            icon = Icons.Filled.FiberManualRecord,
            label = stringResource(id = R.string.call_record),
            isActive = isRecordingActive,
            isDark = colors.isDark,
            onClick = {
              isRecordingActive = !isRecordingActive
              onRecordCall()
            },
            testTag = "call_control_record",
          )

          val addCallIcon = when {
            callSession.canMergeCalls -> Icons.Filled.Call
            callSession.canSwapCalls -> Icons.Filled.Call
            else -> Icons.Filled.Add
          }
          val addCallLabel = when {
            callSession.canMergeCalls -> "merge"
            callSession.canSwapCalls -> "swap"
            else -> stringResource(id = R.string.call_add_call)
          }

          ActiveCallGlassButton(
            icon = addCallIcon,
            label = addCallLabel,
            isActive = callSession.canMergeCalls || callSession.canSwapCalls,
            isDark = colors.isDark,
            onClick = {
              if (callSession.canMergeCalls) {
                onMergeCalls()
              } else if (callSession.canSwapCalls) {
                onSwapCalls()
              } else {
                showAddCallDialog = true
              }
            },
            testTag = "call_control_add_call",
          )

          ActiveCallGlassButton(
            icon = Icons.Filled.Pause,
            label = stringResource(id = R.string.call_hold),
            isActive = isHoldEffective,
            isDark = colors.isDark,
            onClick = {
              isHoldActive = !isHoldEffective
              onToggleHold()
            },
            testTag = "call_control_hold",
          )
        }
      }
    }

    // 4. Bottom Area: Centered Red End Call Button
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 42.dp),
      contentAlignment = Alignment.Center,
    ) {
      CallActionButton(
        type = CallActionType.END,
        onClick = onEndCall,
        testTag = "active_call_end",
      )
    }

    // 5. Call Waiting Floating Banner Overlay (Phase 4D)
    AnimatedVisibility(
      visible = callSession.hasWaitingCall,
      enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(top = 16.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (colors.isDark) Color(0xF21C1C1E) else Color(0xF2FFFFFF),
        shadowElevation = 14.dp,
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "Call Waiting",
            color = if (colors.isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = callSession.waitingCallName ?: "Incoming Call",
            color = headerTextColor,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!callSession.waitingCallNumber.isNullOrBlank()) {
            Text(
              text = callSession.waitingCallNumber,
              color = statusTextColor,
              fontSize = 14.sp,
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = onRejectWaitingCall,
              colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30)),
            ) {
              Text("Decline", fontWeight = FontWeight.SemiBold)
            }

            TextButton(
              onClick = { onAcceptWaitingCall(false) },
              colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9500)),
            ) {
              Text("End & Accept", fontWeight = FontWeight.SemiBold)
            }

            Button(
              onClick = { onAcceptWaitingCall(true) },
              colors = ButtonDefaults.buttonColors(containerColor = CallAcceptGreen),
              shape = CircleShape,
            ) {
              Text("Hold & Accept", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }

    // 6. In-Call Add Call Sheet / Dialog (Phase 4D)
    if (showAddCallDialog) {
      InCallAddCallDialog(
        isDark = colors.isDark,
        onDismiss = { showAddCallDialog = false },
        onCall = { number ->
          showAddCallDialog = false
          onAddCall(number)
        },
      )
    }
  }
}

/**
 * Clean Liquid Glass modal dialog to enter or dial a number for a second real cellular call.
 */
@Composable
private fun InCallAddCallDialog(
  isDark: Boolean,
  onDismiss: () -> Unit,
  onCall: (String) -> Unit,
) {
  var dialedNumber by remember { mutableStateOf("") }
  val textColor = if (isDark) Color.White else Color(0xFF15161A)

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = if (isDark) Color(0xF21C1C1E) else Color(0xF2F2F2F7),
      shadowElevation = 16.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Add Call",
          color = textColor,
          fontSize = 19.sp,
          fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Display entered number with Backspace
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
            .padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = dialedNumber.ifEmpty { "Enter phone number" },
            color = if (dialedNumber.isEmpty()) Color(0xFF8E8E93) else textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )

          if (dialedNumber.isNotEmpty()) {
            IconButton(
              onClick = { dialedNumber = dialedNumber.dropLast(1) },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666),
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compact Keypad
        val keys = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("*", "0", "#"),
        )

        keys.forEach { row ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            row.forEach { digit ->
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(if (isDark) Color(0x28FFFFFF) else Color(0x18000000))
                  .clickable { dialedNumber += digit },
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = digit,
                  color = textColor,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Medium,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: Cancel & Call
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
          ) {
            Text("Cancel", color = IosBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
          }

          Spacer(modifier = Modifier.width(12.dp))

          Button(
            onClick = {
              if (dialedNumber.isNotBlank()) {
                onCall(dialedNumber)
              }
            },
            enabled = dialedNumber.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
              containerColor = CallAcceptGreen,
              disabledContainerColor = Color(0x4034C759),
            ),
            shape = CircleShape,
            modifier = Modifier.weight(1f),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Call",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Call", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

/**
 * Reusable Liquid Glass Circular Control Button for In-Call UI.
 */
@Composable
private fun ActiveCallGlassButton(
  icon: ImageVector,
  label: String,
  isActive: Boolean,
  isDark: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  size: Dp = 82.dp,
  iconSize: Dp = 34.dp,
) {
  val view = LocalView.current
  val interactionSource = remember { MutableInteractionSource() }

  val surfaceBrush = if (isDark) {
    if (isActive) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xF5FFFFFF),
          Color(0xD0E8ECF2),
        ),
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0x3BFFFFFF),
          Color(0x1AFFFFFF),
        ),
      )
    }
  } else {
    if (isActive) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFF202127),
          Color(0xFF2C2D35),
        ),
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0x0E000000),
          Color(0x1A000000),
        ),
      )
    }
  }

  val borderBrush = if (isDark) {
    if (isActive) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFFFFFFFF),
          Color(0xB3FFFFFF),
        ),
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0x60FFFFFF),
          Color(0x18FFFFFF),
        ),
      )
    }
  } else {
    if (isActive) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0x60FFFFFF),
          Color(0x30FFFFFF),
        ),
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0x40FFFFFF),
          Color(0x28000000),
        ),
      )
    }
  }

  val contentColor = if (isDark) {
    if (isActive) Color(0xFF15161A) else Color.White
  } else {
    if (isActive) Color.White else Color(0xFF15161A)
  }

  val labelColor = if (isDark) {
    if (isActive) Color.White else Color(0xFFE2E4E9)
  } else {
    if (isActive) Color(0xFF111215) else Color(0xFF2D2E34)
  }

  val shadowElevation = if (isActive) 6.dp else 4.dp
  val shadowSpotColor = if (isDark) {
    if (isActive) Color(0x60000000) else Color(0x40000000)
  } else {
    if (isActive) Color(0x28000000) else Color(0x16000000)
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .width(100.dp)
      .testTag(testTag),
  ) {
    Box(
      modifier = Modifier
        .size(size)
        .shadow(
          elevation = shadowElevation,
          shape = CircleShape,
          spotColor = shadowSpotColor,
          ambientColor = if (isDark) Color(0x26000000) else Color(0x10000000),
        )
        .clip(CircleShape)
        .background(if (!isDark && !isActive) Color(0xEEFFFFFF) else Color.Transparent)
        .background(surfaceBrush)
        .border(
          width = 0.75.dp,
          brush = borderBrush,
          shape = CircleShape,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
          },
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (!isActive) {
        val highlightColor = if (isDark) Color(0x28FFFFFF) else Color(0x50FFFFFF)
        Box(
          modifier = Modifier
            .matchParentSize()
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  highlightColor,
                  Color.Transparent,
                ),
                center = Offset(x = 65f, y = 55f),
                radius = 75f,
              ),
            ),
        )
      }

      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(iconSize),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = label,
      color = labelColor,
      fontSize = 13.sp,
      fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
  }
}
