package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CallActionButton
import com.example.ui.components.CallActionType
import com.example.ui.components.KeypadButton
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

@Composable
fun KeypadScreen(
  dialedDigits: String,
  formattedNumber: String,
  onDigitPress: (Char) -> Unit,
  onDeleteDigit: () -> Unit,
  onClearDigits: () -> Unit,
  onSetDialedNumber: (String) -> Unit,
  onStartCall: (String) -> Unit,
  onSimulateIncomingCall: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding()
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Top Bar with Simulator Trigger Button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier
          .clip(CircleShape)
          .clickable { onSimulateIncomingCall() }
          .testTag("preview_incoming_call_button"),
        color = colors.keypadButtonBg,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.PhoneCallback,
            contentDescription = "Simulate Incoming Call",
            tint = IosBlue,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Test Incoming Call",
            color = IosBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }

    // Dialed Number Display Area
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = if (formattedNumber.isNotEmpty()) formattedNumber else " ",
        fontSize = if (formattedNumber.length > 12) 30.sp else 38.sp,
        fontWeight = FontWeight.Light,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            // Paste clipboard if available
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!item.isNullOrBlank()) {
              onSetDialedNumber(item)
            }
          }
          .testTag("keypad_display_text"),
      )

      AnimatedVisibility(
        visible = dialedDigits.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Text(
          text = "Add Number",
          color = IosBlue,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier
            .padding(top = 4.dp)
            .clickable {
              // Future Phase: Add Contact
            }
            .testTag("add_number_action"),
        )
      }
    }

    // 3x4 Circular Dialpad Grid
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Row 1: 1, 2 ABC, 3 DEF
      Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        KeypadButton(digit = "1", letters = "", onDigitClick = { onDigitPress('1') })
        KeypadButton(digit = "2", letters = "A B C", onDigitClick = { onDigitPress('2') })
        KeypadButton(digit = "3", letters = "D E F", onDigitClick = { onDigitPress('3') })
      }

      // Row 2: 4 GHI, 5 JKL, 6 MNO
      Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        KeypadButton(digit = "4", letters = "G H I", onDigitClick = { onDigitPress('4') })
        KeypadButton(digit = "5", letters = "J K L", onDigitClick = { onDigitPress('5') })
        KeypadButton(digit = "6", letters = "M N O", onDigitClick = { onDigitPress('6') })
      }

      // Row 3: 7 PQRS, 8 TUV, 9 WXYZ
      Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        KeypadButton(digit = "7", letters = "P Q R S", onDigitClick = { onDigitPress('7') })
        KeypadButton(digit = "8", letters = "T U V", onDigitClick = { onDigitPress('8') })
        KeypadButton(digit = "9", letters = "W X Y Z", onDigitClick = { onDigitPress('9') })
      }

      // Row 4: *, 0 +, #
      Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        KeypadButton(digit = "*", letters = "", onDigitClick = { onDigitPress('*') })
        KeypadButton(
          digit = "0",
          letters = "+",
          onDigitClick = { onDigitPress('0') },
          onLongClick = { onDigitPress('+') },
        )
        KeypadButton(digit = "#", letters = "", onDigitClick = { onDigitPress('#') })
      }

      // Row 5: Call Button & Backspace
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Spacer(modifier = Modifier.size(78.dp))

        // Center: Green Call Button
        Box(
          modifier = Modifier.padding(horizontal = 28.dp),
          contentAlignment = Alignment.Center,
        ) {
          CallActionButton(
            type = CallActionType.CALL,
            size = 78.dp,
            onClick = {
              if (dialedDigits.isNotEmpty()) {
                onStartCall(dialedDigits)
              }
            },
            testTag = "keypad_call_button",
          )
        }

        // Right: Backspace Button
        Box(
          modifier = Modifier.size(78.dp),
          contentAlignment = Alignment.Center,
        ) {
          androidx.compose.animation.AnimatedVisibility(
            visible = dialedDigits.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            IconButton(
              onClick = onDeleteDigit,
              modifier = Modifier
                .size(48.dp)
                .testTag("keypad_backspace_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Delete Digit",
                tint = colors.textSecondary,
                modifier = Modifier.size(28.dp),
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
  }
}
