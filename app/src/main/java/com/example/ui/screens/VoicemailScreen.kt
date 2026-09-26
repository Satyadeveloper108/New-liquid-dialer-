package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

/**
 * Voicemail Screen matching original iOS reference:
 * - Top bar with Edit and Greeting pills
 * - Large title "Voicemail"
 * - Avatar, unread blue dot, bold caller name/number
 * - Gray transcript preview and right-aligned timestamp + duration
 * - "Deleted Voicemails" section with count and chevron
 */
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
    // 1. Top Bar: Edit Pill | Greeting Pill
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .height(34.dp)
          .clip(RoundedCornerShape(17.dp))
          .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF))
          .border(
            width = 0.5.dp,
            color = if (colors.isDark) Color(0x26FFFFFF) else Color(0x1F000000),
            shape = RoundedCornerShape(17.dp),
          )
          .clickable { }
          .padding(horizontal = 16.dp)
          .testTag("voicemail_edit_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(id = R.string.voicemail_edit),
          color = colors.textPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      Box(
        modifier = Modifier
          .height(34.dp)
          .clip(RoundedCornerShape(17.dp))
          .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF))
          .border(
            width = 0.5.dp,
            color = if (colors.isDark) Color(0x26FFFFFF) else Color(0x1F000000),
            shape = RoundedCornerShape(17.dp),
          )
          .clickable { }
          .padding(horizontal = 16.dp)
          .testTag("voicemail_greeting_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(id = R.string.voicemail_greeting),
          color = colors.textPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    // 2. Large Title "Voicemail"
    Text(
      text = stringResource(id = R.string.tab_voicemail),
      fontSize = 34.sp,
      fontWeight = FontWeight.Bold,
      color = colors.textPrimary,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
    ) {
      val activeVoicemails = voicemails.filter { !it.isDeleted }
      val deletedCount = voicemails.count { it.isDeleted }.coerceAtLeast(1)

      items(activeVoicemails, key = { it.id }) { item ->
        val isExpanded = selectedVoicemailId == item.id

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedVoicemailId = if (isExpanded) null else item.id
              onVoicemailClick(item)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("voicemail_item_${item.id}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
          ) {
            // Avatar
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF8FA3C7)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Unread Dot + Caller & Transcription
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.isRead) {
                  Box(
                    modifier = Modifier
                      .padding(end = 6.dp)
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(IosBlue),
                  )
                }
                Text(
                  text = item.callerName,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = colors.textPrimary,
                )
              }

              if (item.transcription.isNotEmpty()) {
                Text(
                  text = "\"${item.transcription}\"",
                  fontSize = 14.sp,
                  color = colors.textSecondary,
                  maxLines = if (isExpanded) 10 else 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(top = 2.dp),
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Date & Duration
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = item.dateFormatted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
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
                .padding(top = 14.dp, start = 58.dp),
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
                  .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = { isPlaying = !isPlaying },
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)),
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
          modifier = Modifier.padding(start = 74.dp),
          thickness = 0.5.dp,
          color = colors.separator,
        )
      }

      // 3. "Deleted Voicemails" row at the bottom matching reference
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Deleted Voicemails",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = deletedCount.toString(),
              fontSize = 15.sp,
              color = colors.textSecondary,
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = colors.textSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 16.dp),
          thickness = 0.5.dp,
          color = colors.separator,
        )
      }

      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }
}
