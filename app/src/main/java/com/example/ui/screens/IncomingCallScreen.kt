package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CallSession
import com.example.ui.components.CallActionButton
import com.example.ui.components.CallActionType
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallDeclineRed

@Composable
fun IncomingCallScreen(
  callSession: CallSession,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF1C1D22),
            Color(0xFF0D0E11),
            Color(0xFF000000),
          ),
        ),
      )
      .statusBarsPadding()
      .padding(horizontal = 28.dp, vertical = 24.dp),
  ) {
    // Upper area: Caller Information
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "mobile",
        color = Color(0xFF8E8E93),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = callSession.callerName,
        color = Color.White,
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = callSession.phoneNumber,
        color = Color(0xFFAAAAAA),
        fontSize = 17.sp,
      )
    }

    // Lower area: Action Options & Accept/Decline Controls
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(bottom = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Secondary Quick Actions: Remind Me & Message
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable { },
        ) {
          Icon(
            imageVector = Icons.Filled.Alarm,
            contentDescription = "Remind Me",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(id = R.string.call_remind_me),
            color = Color.White,
            fontSize = 13.sp,
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable { },
        ) {
          Icon(
            imageVector = Icons.Filled.Message,
            contentDescription = "Message",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(id = R.string.call_type_reply),
            color = Color.White,
            fontSize = 13.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Accept & Decline Buttons
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CallActionButton(
          type = com.example.ui.components.CallActionType.DECLINE,
          label = stringResource(id = R.string.call_decline),
          onClick = onDecline,
          testTag = "incoming_call_decline",
        )

        CallActionButton(
          type = com.example.ui.components.CallActionType.ACCEPT,
          label = stringResource(id = R.string.call_accept),
          onClick = onAccept,
          testTag = "incoming_call_accept",
        )
      }
    }
  }
}
