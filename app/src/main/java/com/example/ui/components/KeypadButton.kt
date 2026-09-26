package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosLightGlassBorderBottom
import com.example.ui.theme.IosLightGlassBorderTop
import com.example.ui.theme.IosLightGlassShadowAmbient
import com.example.ui.theme.IosLightGlassShadowSpot
import com.example.ui.theme.IosLightGlassTintCenter
import com.example.ui.theme.IosLightGlassTintEdge
import com.example.ui.theme.IosLightGlassTintPressed
import com.example.ui.theme.LocalIosColors

/**
 * Native iOS Keypad Button with Light Mode Liquid Glass Depth Refinement.
 *
 * Light Mode:
 * - Subtle smoky translucent charcoal tint (5%–9% alpha over frosted white)
 * - Slightly stronger tint near the edge than the center for glass refractive depth
 * - Hairline dual-tone border (top white highlight catch, bottom dark-neutral rim)
 * - Soft, diffused ambient drop shadow giving physical elevation from white page
 * - Subtle inner specular top-left highlight
 *
 * Dark Mode:
 * - Preserved dark charcoal glass styling with subtle depth
 */
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

  // 1. Surface Fill & Tint
  val lightSurfaceGradient = if (isPressed) {
    Brush.radialGradient(
      colors = listOf(
        IosLightGlassTintPressed,
        IosLightGlassTintPressed,
      ),
    )
  } else {
    Brush.radialGradient(
      colors = listOf(
        IosLightGlassTintCenter, // ~5% alpha in center
        IosLightGlassTintEdge,   // ~9% alpha near rim
      ),
      radius = 110f,
    )
  }

  // 2. Hairline Border
  val lightBorderBrush = Brush.verticalGradient(
    colors = listOf(
      IosLightGlassBorderTop,    // ~50% white specular top catch
      IosLightGlassBorderBottom, // ~17% dark-neutral hairline rim
    ),
  )

  val darkBorderBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0x33FFFFFF),
      Color(0x1AFFFFFF),
    ),
  )

  // 3. Shadow Elevation & Color
  val currentElevation = if (colors.isDark) {
    if (isPressed) 0.5.dp else 2.dp
  } else {
    if (isPressed) 1.dp else 3.5.dp
  }

  val spotShadowColor = if (colors.isDark) Color(0x40000000) else IosLightGlassShadowSpot
  val ambientShadowColor = if (colors.isDark) Color(0x20000000) else IosLightGlassShadowAmbient

  Box(
    modifier = modifier
      .size(size)
      .shadow(
        elevation = currentElevation,
        shape = CircleShape,
        spotColor = spotShadowColor,
        ambientColor = ambientShadowColor,
      )
      .clip(CircleShape)
      .then(
        if (colors.isDark) {
          val darkBg = if (isPressed) Color(0xFF3A3A3C) else Color(0xFF2C2C2E)
          Modifier.background(darkBg)
        } else {
          // Light Mode: Frosted white base + subtle smoky charcoal depth gradient
          Modifier
            .background(Color(0xF0FFFFFF))
            .background(lightSurfaceGradient)
        }
      )
      .border(
        width = 0.6.dp,
        brush = if (colors.isDark) darkBorderBrush else lightBorderBrush,
        shape = CircleShape,
      )
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
    contentAlignment = Alignment.Center,
  ) {
    // Subtle inner top-left specular highlight reflection in Light Mode
    if (!colors.isDark && !isPressed) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color(0x4DFFFFFF), // ~30% white reflection
                Color.Transparent,
              ),
              center = Offset(x = 55f, y = 45f),
              radius = 75f,
            ),
          ),
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = digit,
        color = colors.textPrimary,
        fontSize = if (digit == "*" || digit == "#") 38.sp else 35.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 36.sp,
      )
      if (letters.isNotEmpty()) {
        Text(
          text = letters,
          color = colors.textPrimary,
          fontSize = if (letters == "+") 14.sp else 10.5.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = if (letters == "+") 0.sp else 0.5.sp,
        )
      }
    }
  }
}
