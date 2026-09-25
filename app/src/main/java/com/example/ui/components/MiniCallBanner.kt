package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallSession
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed

@Composable
fun MiniCallBanner(
  callSession: CallSession?,
  visible: Boolean,
  onRestoreCall: () -> Unit,
  onEndCall: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible && callSession != null,
    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    modifier = modifier,
  ) {
    if (callSession == null) return@AnimatedVisibility

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(32.dp))
          .clickable { onRestoreCall() }
          .testTag("mini_call_banner"),
        color = Color(0xE61C1C1E),
        shadowElevation = 10.dp,
        tonalElevation = 6.dp,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Left: Green Call Status Indicator
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CallAcceptGreen),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Active Call",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = callSession.callerName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = callSession.formattedDuration,
                color = CallAcceptGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
              )
            }
          }

          // Right: Quick End Call Button
          IconButton(
            onClick = onEndCall,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(CallDeclineRed)
              .testTag("mini_banner_end_call"),
          ) {
            Icon(
              imageVector = Icons.Filled.CallEnd,
              contentDescription = "End Call",
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}
