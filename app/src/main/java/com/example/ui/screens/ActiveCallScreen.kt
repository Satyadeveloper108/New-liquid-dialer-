package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CallSession
import com.example.ui.components.CallActionButton
import com.example.ui.components.CallActionType
import com.example.ui.components.KeypadButton
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallButtonActiveBg
import com.example.ui.theme.CallButtonGlassBg
import com.example.ui.theme.CallDeclineRed
import com.example.ui.theme.IosBlue

@Composable
fun ActiveCallScreen(
  callSession: CallSession,
  onEndCall: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleKeypad: () -> Unit,
  onMinimize: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF1F2026),
            Color(0xFF101115),
            Color(0xFF000000),
          ),
        ),
      )
      .statusBarsPadding()
      .padding(horizontal = 24.dp, vertical = 16.dp),
  ) {
    // Minimize button in top-left
    IconButton(
      onClick = onMinimize,
      modifier = Modifier
        .align(Alignment.TopStart)
        .testTag("active_call_minimize_button"),
    ) {
      Icon(
        imageVector = Icons.Filled.KeyboardArrowDown,
        contentDescription = "Minimize Call",
        tint = Color.White,
        modifier = Modifier.size(32.dp),
      )
    }

    // Upper area: Timer and Caller Name
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = callSession.callerName,
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = callSession.formattedDuration,
        color = Color(0xFFAAAAAA),
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
      )
    }

    // Center area: either DTMF in-call Keypad or standard call controls
    if (callSession.isKeypadOpen) {
      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // DTMF Keys
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
      // 6 In-Call Control Buttons Grid
      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Row 1: Mute, Keypad, Audio
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          CallControlButton(
            icon = if (callSession.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            label = stringResource(id = R.string.call_mute),
            isActive = callSession.isMuted,
            onClick = onToggleMute,
            tag = "call_control_mute",
          )

          CallControlButton(
            icon = Icons.Filled.Dialpad,
            label = stringResource(id = R.string.call_keypad),
            isActive = false,
            onClick = onToggleKeypad,
            tag = "call_control_keypad",
          )

          CallControlButton(
            icon = Icons.Filled.VolumeUp,
            label = stringResource(id = R.string.call_audio),
            isActive = callSession.isSpeaker,
            onClick = onToggleSpeaker,
            tag = "call_control_audio",
          )
        }

        // Row 2: Add Call / More, FaceTime / Video, etc.
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround,
        ) {
          CallControlButton(
            icon = Icons.Filled.MoreHoriz,
            label = stringResource(id = R.string.call_more),
            isActive = false,
            onClick = { },
            tag = "call_control_more",
          )

          CallControlButton(
            icon = Icons.Filled.Videocam,
            label = stringResource(id = R.string.call_video),
            isActive = false,
            onClick = { },
            tag = "call_control_video",
          )
        }
      }
    }

    // Bottom area: End Call Button
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 36.dp),
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

@Composable
private fun CallControlButton(
  icon: ImageVector,
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  val bgColor = if (isActive) CallButtonActiveBg else CallButtonGlassBg
  val iconColor = if (isActive) Color.Black else Color.White

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.testTag(tag),
  ) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(bgColor),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = iconColor,
        modifier = Modifier.size(28.dp),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = label,
      color = Color.White,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
    )
  }
}
