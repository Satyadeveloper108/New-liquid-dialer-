package com.example.ui.screens

import android.view.HapticFeedbackConstants
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CallSession
import com.example.ui.components.CallActionButton
import com.example.ui.components.CallActionType
import com.example.ui.components.KeypadButton
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

/**
 * Active Call Screen with Liquid Glass controls and Theme Sync.
 * Layout:
 * - Upper Area: Minimize chevron, large caller name, and running call timer
 * - Center Grid: 3-column x 2-row fixed control order (slightly enlarged & shifted down):
 *     Row 1: Mute | Keypad | Speaker
 *     Row 2: Record Call | Add Call | Hold
 * - Bottom Area: Centered red circular End Call button
 * - Automatic System Theme Sync (Light / Dark mode adaptation)
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
  onAddCall: () -> Unit = {},
  onToggleHold: () -> Unit = {},
) {
  val colors = LocalIosColors.current
  var isRecordingActive by remember { mutableStateOf(false) }
  var isHoldActive by remember { mutableStateOf(false) }

  // Background adapts to system theme
  val bgGradient = if (colors.isDark) {
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

  val headerTextColor = if (colors.isDark) Color.White else colors.textPrimary
  val statusTextColor = if (isHoldActive) {
    Color(0xFFFF9500)
  } else {
    if (colors.isDark) Color(0xFFAAAAAA) else colors.textSecondary
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

    // 2. Upper Header: Caller Name & Duration
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = callSession.callerName,
        color = headerTextColor,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = if (isHoldActive) "on hold" else callSession.formattedDuration,
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
                onDigitClick = { },
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
      // Perfectly centered, with enlarged circular glass buttons (82.dp), expanded inter-row spacing (30.dp),
      // downward offset (60.dp top offset), and balanced comfortable spacing to the End Call button.
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
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = stringResource(id = R.string.call_speaker),
            isActive = callSession.isSpeaker,
            isDark = colors.isDark,
            onClick = onToggleSpeaker,
            testTag = "call_control_speaker",
          )
        }

        // ROW 2: Record Call | Add Call | Hold
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

          ActiveCallGlassButton(
            icon = Icons.Filled.Add,
            label = stringResource(id = R.string.call_add_call),
            isActive = false,
            isDark = colors.isDark,
            onClick = onAddCall,
            testTag = "call_control_add_call",
          )

          ActiveCallGlassButton(
            icon = Icons.Filled.Pause,
            label = stringResource(id = R.string.call_hold),
            isActive = isHoldActive,
            isDark = colors.isDark,
            onClick = {
              isHoldActive = !isHoldActive
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
  }
}

/**
 * Reusable Liquid Glass Circular Control Button for In-Call UI.
 * Features:
 * - Enlarged by ~9.7% (79.dp) with centered white/dark icon and crisp label
 * - Automatic System Theme Sync:
 *   - Dark Mode: Translucent charcoal glass gradient with soft ambient depth & white icons
 *   - Light Mode: Translucent smoky Liquid Glass gradient with soft diffused depth & dark icons
 * - Hairline light-catching specular rim (0.75.dp)
 * - Inner specular highlight sheen
 * - Distinct elevated active state
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

  // Surface brush based on theme and active state
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
    // Light Mode Liquid Glass
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

  // Border brush based on theme and active state
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
    // Light Mode border
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
      // Subtle inner top-left specular highlight sheen in inactive glass
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
