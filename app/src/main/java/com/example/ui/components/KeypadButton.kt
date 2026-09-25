package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIosColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
  digit: String,
  letters: String = "",
  size: Dp = 78.dp,
  onDigitClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val view = LocalView.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val backgroundColor = if (isPressed) {
    colors.keypadButtonPressedBg
  } else {
    colors.keypadButtonBg
  }

  Surface(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .testTag("keypad_button_$digit")
      .semantics {
        contentDescription = if (letters.isNotEmpty()) "$digit, $letters" else digit
      }
      .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
          view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
          onDigitClick()
        },
        onLongClick = if (onLongClick != null) {
          {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongClick()
          }
        } else null,
      ),
    shape = CircleShape,
    color = backgroundColor,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(size),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = digit,
          color = colors.textPrimary,
          fontSize = if (digit == "*" || digit == "#") 38.sp else 34.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 36.sp,
        )
        if (letters.isNotEmpty()) {
          Text(
            text = letters,
            color = colors.textPrimary,
            fontSize = if (letters == "+") 14.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
          )
        }
      }
    }
  }
}
