package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIosColors

@Composable
fun RoundedIconButton(
  icon: ImageVector,
  contentDescription: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = 44.dp,
  iconSize: Dp = 22.dp,
  backgroundColor: Color? = null,
  iconTint: Color? = null,
  testTag: String = "rounded_icon_button",
) {
  val colors = LocalIosColors.current
  val resolvedBg = backgroundColor ?: colors.keypadButtonBg
  val resolvedTint = iconTint ?: colors.textPrimary

  Box(
    modifier = modifier
      .size(buttonSize)
      .clip(CircleShape)
      .background(resolvedBg),
    contentAlignment = Alignment.Center,
  ) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(buttonSize)
        .testTag(testTag),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = resolvedTint,
        modifier = Modifier.size(iconSize),
      )
    }
  }
}
