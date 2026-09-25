package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIosColors

@Composable
fun ScreenHeader(
  title: String,
  modifier: Modifier = Modifier,
  leadingAction: (@Composable () -> Unit)? = null,
  trailingAction: (@Composable () -> Unit)? = null,
  centerContent: (@Composable () -> Unit)? = null,
) {
  val colors = LocalIosColors.current

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
  ) {
    // Top Action Row
    if (leadingAction != null || trailingAction != null || centerContent != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
          leadingAction?.invoke()
        }

        Box(
          modifier = Modifier.weight(2f, fill = false),
          contentAlignment = Alignment.Center,
        ) {
          centerContent?.invoke()
        }

        Box(
          modifier = Modifier.weight(1f, fill = false),
          contentAlignment = Alignment.CenterEnd,
        ) {
          trailingAction?.invoke()
        }
      }
    }

    // Large iOS Title
    Text(
      text = title,
      fontSize = 32.sp,
      fontWeight = FontWeight.Bold,
      color = colors.textPrimary,
      letterSpacing = (-0.5).sp,
      modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
    )
  }
}
