package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VoicemailItem
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

@Composable
fun VoicemailScreen(
  voicemails: List<VoicemailItem>,
  onVoicemailClick: (VoicemailItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  var selectedVoicemailId by remember { mutableStateOf<String?>(null) }
  var isPlaying by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding(),
  ) {
    // Header with Greeting and Edit actions
    ScreenHeader(
      title = stringResource(id = R.string.tab_voicemail),
      leadingAction = {
        TextButton(
          onClick = { },
          modifier = Modifier.testTag("voicemail_greeting_button"),
        ) {
          Text(
            text = stringResource(id = R.string.voicemail_greeting),
            color = IosBlue,
            fontSize = 17.sp,
          )
        }
      },
      trailingAction = {
        TextButton(
          onClick = { },
          modifier = Modifier.testTag("voicemail_edit_button"),
        ) {
          Text(
            text = stringResource(id = R.string.voicemail_edit),
            color = IosBlue,
            fontSize = 17.sp,
          )
        }
      },
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
    ) {
      items(voicemails.filter { !it.isDeleted }, key = { it.id }) { item ->
        val isExpanded = selectedVoicemailId == item.id

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedVoicemailId = if (isExpanded) null else item.id
              onVoicemailClick(item)
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("voicemail_item_${item.id}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
          ) {
            // Blue Unread Dot
            Box(
              modifier = Modifier
                .padding(top = 6.dp, end = 10.dp)
                .size(10.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (!item.isRead) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(IosBlue),
                )
              }
            }

            // Caller & Transcription
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.callerName,
                fontSize = 17.sp,
                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                color = colors.textPrimary,
              )

              if (item.transcription.isNotEmpty()) {
                Text(
                  text = item.transcription,
                  fontSize = 14.sp,
                  color = colors.textSecondary,
                  maxLines = if (isExpanded) 10 else 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(top = 2.dp),
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Date & Duration
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = item.dateFormatted,
                fontSize = 14.sp,
                color = colors.textSecondary,
              )
              Text(
                text = item.durationFormatted,
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
              )
            }
          }

          // Expandable Playback Controls
          AnimatedVisibility(visible = isExpanded) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 20.dp),
            ) {
              LinearProgressIndicator(
                progress = { if (isPlaying) 0.45f else 0.0f },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp)),
                color = IosBlue,
                trackColor = colors.keypadButtonBg,
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = { isPlaying = !isPlaying },
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.keypadButtonBg),
                ) {
                  Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Voicemail",
                    tint = colors.textPrimary,
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                  IconButton(onClick = { }) {
                    Icon(
                      imageVector = Icons.Filled.Share,
                      contentDescription = "Share",
                      tint = IosBlue,
                      modifier = Modifier.size(22.dp),
                    )
                  }
                  IconButton(onClick = { }) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = "Delete",
                      tint = IosBlue,
                      modifier = Modifier.size(22.dp),
                    )
                  }
                }
              }
            }
          }
        }

        HorizontalDivider(
          modifier = Modifier.padding(start = 40.dp),
          thickness = 0.5.dp,
          color = colors.separator,
        )
      }

      // Deleted Voicemails Section
      item(key = "deleted_voicemails") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("deleted_voicemails_row"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(id = R.string.voicemail_deleted),
            fontSize = 17.sp,
            color = colors.textPrimary,
          )
          Text(
            text = "0",
            fontSize = 15.sp,
            color = colors.textSecondary,
          )
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
      }

      // Information notice regarding carrier voicemail support
      item(key = "carrier_notice") {
        com.example.ui.components.GlassSurface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          shape = RoundedCornerShape(12.dp),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Filled.Info,
              contentDescription = null,
              tint = IosBlue,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Visual Voicemail requires carrier support. Standard carrier voicemail can also be accessed by holding 1 on the Keypad.",
              fontSize = 12.sp,
              color = colors.textSecondary,
              lineHeight = 16.sp,
            )
          }
        }
      }
    }
  }
}
