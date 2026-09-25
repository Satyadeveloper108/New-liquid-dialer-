package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = IosBlue,
  onPrimary = Color.White,
  secondary = IosDarkKeypadBg,
  onSecondary = Color.White,
  tertiary = IosGreen,
  background = IosDarkBackground,
  onBackground = IosDarkTextPrimary,
  surface = IosDarkCard,
  onSurface = IosDarkTextPrimary,
  surfaceVariant = IosDarkTertiaryBg,
  outline = IosDarkSeparator,
  error = IosRed,
)

private val LightColorScheme = lightColorScheme(
  primary = IosBlue,
  onPrimary = Color.White,
  secondary = IosLightKeypadBg,
  onSecondary = IosLightTextPrimary,
  tertiary = IosGreen,
  background = IosLightBackground,
  onBackground = IosLightTextPrimary,
  surface = IosLightCard,
  onSurface = IosLightTextPrimary,
  surfaceVariant = IosLightGroupedBackground,
  outline = IosLightSeparator,
  error = IosRed,
)

@Immutable
data class IosThemeColors(
  val isDark: Boolean,
  val background: Color,
  val groupedBackground: Color,
  val cardBackground: Color,
  val separator: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val keypadButtonBg: Color,
  val keypadButtonPressedBg: Color,
  val navBackground: Color,
  val selectedPillBg: Color,
  val accentBlue: Color = IosBlue,
  val callGreen: Color = CallAcceptGreen,
  val endCallRed: Color = CallDeclineRed,
)

val LocalIosColors = staticCompositionLocalOf {
  IosThemeColors(
    isDark = false,
    background = IosLightBackground,
    groupedBackground = IosLightGroupedBackground,
    cardBackground = IosLightCard,
    separator = IosLightSeparator,
    textPrimary = IosLightTextPrimary,
    textSecondary = IosLightTextSecondary,
    keypadButtonBg = IosLightKeypadBg,
    keypadButtonPressedBg = IosLightKeypadPressed,
    navBackground = IosLightNavBg,
    selectedPillBg = IosLightSelectedPill,
  )
}

@Composable
fun PhoneTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val iosColors = if (darkTheme) {
    IosThemeColors(
      isDark = true,
      background = IosDarkBackground,
      groupedBackground = IosDarkGroupedBackground,
      cardBackground = IosDarkCard,
      separator = IosDarkSeparator,
      textPrimary = IosDarkTextPrimary,
      textSecondary = IosDarkTextSecondary,
      keypadButtonBg = IosDarkKeypadBg,
      keypadButtonPressedBg = IosDarkKeypadPressed,
      navBackground = IosDarkNavBg,
      selectedPillBg = IosDarkSelectedPill,
    )
  } else {
    IosThemeColors(
      isDark = false,
      background = IosLightBackground,
      groupedBackground = IosLightGroupedBackground,
      cardBackground = IosLightCard,
      separator = IosLightSeparator,
      textPrimary = IosLightTextPrimary,
      textSecondary = IosLightTextSecondary,
      keypadButtonBg = IosLightKeypadBg,
      keypadButtonPressedBg = IosLightKeypadPressed,
      navBackground = IosLightNavBg,
      selectedPillBg = IosLightSelectedPill,
    )
  }

  CompositionLocalProvider(LocalIosColors provides iosColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
    )
  }
}
