package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CallRecording
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.components.ContactAvatar
import com.example.ui.components.GlassSurface
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIosColors
import java.util.Locale

@Composable
fun CallDetailsScreen(
  callRecord: CallRecord,
  recordings: List<CallRecording>,
  playingRecordingId: Long?,
  isPlaybackPlaying: Boolean,
  playbackPositionMs: Long,
  playbackTotalDurationMs: Long,
  onBack: () -> Unit,
  onCall: (String) -> Unit,
  onPlayRecording: (CallRecording) -> Unit,
  onPauseRecording: () -> Unit,
  onDeleteRecording: (CallRecording) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val context = LocalContext.current
  var recordingToDelete by remember { mutableStateOf<CallRecording?>(null) }

  BackHandler {
    onBack()
  }

  val avatarInitial = callRecord.contactName.firstOrNull { it.isLetter() }?.uppercaseChar()
  val colorIndex = (callRecord.id.hashCode().coerceAtLeast(0) % 5)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background),
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding(),
      contentPadding = PaddingValues(bottom = 90.dp),
    ) {
      // Top Navigation Bar
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 8.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
              contentDescription = "Back",
              tint = IosBlue,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Recents",
              color = IosBlue,
              fontSize = 17.sp,
              fontWeight = FontWeight.Normal,
            )
          }

          Spacer(modifier = Modifier.weight(1f))

          Text(
            text = "Call Info",
            color = colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 48.dp),
          )
        }
      }

      // Contact Hero Profile Card
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          ContactAvatar(
            initial = avatarInitial ?: '#',
            colorIndex = colorIndex,
            size = 84.dp,
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = callRecord.contactName,
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "${callRecord.phoneType} • ${callRecord.phoneNumber}",
            color = colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
          )

          Spacer(modifier = Modifier.height(18.dp))

          // Quick Action Buttons (Call, Message, Share)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            DetailActionButton(
              icon = Icons.Filled.Call,
              label = "call",
              onClick = { onCall(callRecord.phoneNumber) },
              colors = colors,
            )

            DetailActionButton(
              icon = Icons.Filled.Message,
              label = "message",
              onClick = {
                try {
                  val sendIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("sms:${callRecord.phoneNumber}")
                  }
                  context.startActivity(sendIntent)
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              },
              colors = colors,
            )
          }
        }
      }

      // Recent Call Log Entry Details Card
      item {
        PaddingWrapper {
          GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            isDark = colors.isDark,
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = callRecord.formattedDate.ifBlank { "Call Record" },
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                val (callIcon, typeText, typeColor) = when (callRecord.callType) {
                  CallType.INCOMING -> Triple(Icons.AutoMirrored.Filled.CallReceived, "Incoming Call", colors.textPrimary)
                  CallType.OUTGOING -> Triple(Icons.AutoMirrored.Filled.CallMade, "Outgoing Call", colors.textPrimary)
                  CallType.MISSED -> Triple(Icons.Filled.CallMissed, "Missed Call", IosRed)
                }

                Icon(
                  imageVector = callIcon,
                  contentDescription = null,
                  tint = typeColor,
                  modifier = Modifier.size(16.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                  text = typeText,
                  color = typeColor,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                  text = callRecord.timeFormatted,
                  color = colors.textSecondary,
                  fontSize = 14.sp,
                )
              }

              if (callRecord.durationFormatted.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Duration: ${callRecord.durationFormatted}",
                  color = colors.textSecondary,
                  fontSize = 13.sp,
                  modifier = Modifier.padding(start = 24.dp),
                )
              }
            }
          }
        }
      }

      // Section Header: Recordings (Count)
      item {
        Spacer(modifier = Modifier.height(12.dp))
        PaddingWrapper {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Recordings (${recordings.size})",
              color = colors.textPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // Recordings List or Empty State
      if (recordings.isEmpty()) {
        item {
          PaddingWrapper {
            GlassSurface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              isDark = colors.isDark,
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = "No call recordings yet",
                  color = colors.textPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Recordings made during calls with this number will appear here with instant in-app playback.",
                  color = colors.textSecondary,
                  fontSize = 13.sp,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }
      } else {
        items(recordings, key = { it.id }) { recording ->
          val isThisPlaying = playingRecordingId == recording.id && isPlaybackPlaying
          val isThisSelected = playingRecordingId == recording.id

          PaddingWrapper {
            GlassSurface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              shape = RoundedCornerShape(14.dp),
              isDark = colors.isDark,
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .combinedClickable(
                    onClick = {
                      if (isThisPlaying) {
                        onPauseRecording()
                      } else {
                        onPlayRecording(recording)
                      }
                    },
                    onLongClick = {
                      recordingToDelete = recording
                    },
                  )
                  .padding(14.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  // In-App Play / Pause Button
                  Surface(
                    shape = CircleShape,
                    color = if (isThisPlaying) IosBlue else (if (colors.isDark) Color(0x33FFFFFF) else Color(0x1F000000)),
                    modifier = Modifier
                      .size(42.dp)
                      .clip(CircleShape),
                    onClick = {
                      if (isThisPlaying) {
                        onPauseRecording()
                      } else {
                        onPlayRecording(recording)
                      }
                    },
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Icon(
                        imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isThisPlaying) "Pause" else "Play",
                        tint = if (isThisPlaying) Color.White else colors.textPrimary,
                        modifier = Modifier.size(24.dp),
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  // Recording Title (Date/Time) & Subtitle (Duration)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = recording.formattedDateTime,
                      color = colors.textPrimary,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                      text = "Duration: ${recording.formattedDuration}",
                      color = colors.textSecondary,
                      fontSize = 13.sp,
                    )
                  }

                  // Delete Button
                  IconButton(
                    onClick = { recordingToDelete = recording },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Delete,
                      contentDescription = "Delete recording",
                      tint = colors.textSecondary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }

                // In-App Playback Progress Bar (Visible when playing)
                AnimatedVisibility(
                  visible = isThisSelected,
                  enter = fadeIn(),
                  exit = fadeOut(),
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 10.dp),
                  ) {
                    val progress = if (playbackTotalDurationMs > 0) {
                      (playbackPositionMs.toFloat() / playbackTotalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                      progress = { progress },
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                      color = IosBlue,
                      trackColor = if (colors.isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = formatMillis(playbackPositionMs),
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                      )
                      Text(
                        text = recording.formattedDuration,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  recordingToDelete?.let { recording ->
    AlertDialog(
      onDismissRequest = { recordingToDelete = null },
      title = {
        Text("Delete Recording", fontWeight = FontWeight.Bold)
      },
      text = {
        Text("Are you sure you want to delete this call recording? This action cannot be undone.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteRecording(recording)
            recordingToDelete = null
          },
        ) {
          Text("Delete", color = IosRed, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { recordingToDelete = null }) {
          Text("Cancel", color = IosBlue)
        }
      },
    )
  }
}

@Composable
private fun DetailActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  colors: com.example.ui.theme.IosThemeColors,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(horizontal = 8.dp),
  ) {
    Surface(
      shape = CircleShape,
      color = if (colors.isDark) Color(0x33FFFFFF) else Color(0x14000000),
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape),
      onClick = onClick,
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = IosBlue,
          modifier = Modifier.size(22.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = label,
      color = IosBlue,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun PaddingWrapper(content: @Composable () -> Unit) {
  Box(modifier = Modifier.padding(horizontal = 16.dp)) {
    content()
  }
}

private fun formatMillis(millis: Long): String {
  val totalSecs = (millis / 1000).coerceAtLeast(0)
  val mins = totalSecs / 60
  val secs = totalSecs % 60
  return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
