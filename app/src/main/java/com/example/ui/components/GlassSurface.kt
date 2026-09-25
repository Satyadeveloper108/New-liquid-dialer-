package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIosColors

@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  elevation: Dp = 4.dp,
  content: @Composable BoxScope.() -> Unit,
) {
  val colors = LocalIosColors.current
  val glassBg = if (colors.isDark) Color(0xE61C1C1E) else Color(0xF2FFFFFF)
  val glassBorder = if (colors.isDark) Color(0x33FFFFFF) else Color(0x1F000000)

  Surface(
    modifier = modifier
      .shadow(elevation = elevation, shape = shape)
      .border(width = 0.5.dp, color = glassBorder, shape = shape),
    shape = shape,
    color = glassBg,
  ) {
    Box(content = content)
  }
}
