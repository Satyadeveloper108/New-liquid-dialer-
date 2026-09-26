package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.IosLightGlassBorderBottom
import com.example.ui.theme.IosLightGlassBorderTop
import com.example.ui.theme.IosLightGlassShadowAmbient
import com.example.ui.theme.IosLightGlassShadowSpot
import com.example.ui.theme.LocalIosColors

/**
 * Reusable Liquid Glass surface featuring:
 * 1. Translucent frosted glass body with subtle smoky depth tint
 * 2. Hairline dual-tone specular border reflecting top light
 * 3. Soft diffused low-opacity ambient drop shadow
 */
@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  elevation: Dp = 6.dp,
  content: @Composable BoxScope.() -> Unit,
) {
  val colors = LocalIosColors.current

  // Glass gradient fill
  val glassGradient = if (colors.isDark) {
    Brush.verticalGradient(
      colors = listOf(
        Color(0xE626262A), // Top slightly lighter for specular reflection
        Color(0xE61B1B1E), // Bottom deeper charcoal
      ),
    )
  } else {
    Brush.verticalGradient(
      colors = listOf(
        Color(0x0C000000), // ~5% smoky tint at top
        Color(0x17000000), // ~9% smoky tint at bottom
      ),
    )
  }

  // Specular hairline glass border
  val glassBorderBrush = if (colors.isDark) {
    Brush.verticalGradient(
      colors = listOf(
        Color(0x40FFFFFF), // Subtle top rim highlight
        Color(0x14FFFFFF), // Translucent lower rim
      ),
    )
  } else {
    Brush.verticalGradient(
      colors = listOf(
        IosLightGlassBorderTop,    // Crisp white top reflection catch
        IosLightGlassBorderBottom, // Soft dark-neutral rim boundary
      ),
    )
  }

  val spotShadow = if (colors.isDark) Color(0x66000000) else IosLightGlassShadowSpot
  val ambientShadow = if (colors.isDark) Color(0x40000000) else IosLightGlassShadowAmbient

  Box(
    modifier = modifier
      .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = spotShadow,
        ambientColor = ambientShadow,
      )
      .clip(shape)
      .then(
        if (colors.isDark) {
          Modifier.background(glassGradient)
        } else {
          Modifier
            .background(Color(0xEEFFFFFF))
            .background(glassGradient)
        }
      )
      .border(width = 0.65.dp, brush = glassBorderBrush, shape = shape),
  ) {
    content()
  }
}
