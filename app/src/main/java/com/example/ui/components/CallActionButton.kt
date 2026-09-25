package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed

enum class CallActionType {
  ACCEPT,
  DECLINE,
  END,
  CALL
}

@Composable
fun CallActionButton(
  type: CallActionType,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  size: Dp = 76.dp,
  iconSize: Dp = 36.dp,
  testTag: String = "call_action_${type.name.lowercase()}",
) {
  val (bgColor, icon, defaultDesc) = when (type) {
    CallActionType.ACCEPT -> Triple(CallAcceptGreen, Icons.Filled.Call, "Accept Call")
    CallActionType.CALL -> Triple(CallAcceptGreen, Icons.Filled.Call, "Call")
    CallActionType.DECLINE -> Triple(CallDeclineRed, Icons.Filled.CallEnd, "Decline Call")
    CallActionType.END -> Triple(CallDeclineRed, Icons.Filled.CallEnd, "End Call")
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(bgColor),
      contentAlignment = Alignment.Center,
    ) {
      IconButton(
        onClick = onClick,
        modifier = Modifier
          .size(size)
          .testTag(testTag),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label ?: defaultDesc,
          tint = Color.White,
          modifier = Modifier.size(iconSize),
        )
      }
    }

    if (label != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = label,
        color = Color.White,
        fontSize = 14.sp,
      )
    }
  }
}
