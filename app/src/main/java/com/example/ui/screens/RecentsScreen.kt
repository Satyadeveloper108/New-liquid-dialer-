package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.RecentsFilter
import com.example.ui.components.ContactAvatar
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIosColors

/**
 * Recents Screen matching original iOS reference:
 * - Top row: Edit pill, All/Missed segmented control, right circular filter button
 * - Large title "Recents"
 * - Avatar with initial or silhouette
 * - Bold name (red for missed calls), call arrow + metadata
 * - Timestamp and circular Info (i) icon in iOS Blue
 */
@Composable
fun RecentsScreen(
  recents: List<CallRecord>,
  selectedFilter: RecentsFilter,
  onFilterSelected: (RecentsFilter) -> Unit,
  onCallRecordClick: (CallRecord) -> Unit,
  onDeleteRecord: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  var isEditMode by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding(),
  ) {
    // 1. Top Bar: Edit Pill | Segmented Control | Filter Circle
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
          .clickable { isEditMode = !isEditMode }
          .padding(horizontal = 16.dp)
          .testTag("recents_edit_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (isEditMode) "Done" else "Edit",
          color = colors.textPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      SegmentedControl(
        selectedFilter = selectedFilter,
        onFilterSelected = onFilterSelected,
        modifier = Modifier.width(180.dp),
      )

      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF))
          .border(
            width = 0.5.dp,
            color = if (colors.isDark) Color(0x26FFFFFF) else Color(0x1F000000),
            shape = CircleShape,
          )
          .clickable { /* Filter menu action */ },
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Menu,
          contentDescription = "Filter",
          tint = colors.textPrimary,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    // 2. Large Title "Recents"
    Text(
      text = stringResource(id = R.string.tab_recents),
      fontSize = 34.sp,
      fontWeight = FontWeight.Bold,
      color = colors.textPrimary,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    if (recents.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(id = R.string.no_recents),
          fontSize = 18.sp,
          color = colors.textSecondary,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
      ) {
        items(recents, key = { it.id }) { item ->
          val isMissed = item.callType == CallType.MISSED
          val titleColor = if (isMissed) IosRed else colors.textPrimary

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onCallRecordClick(item) }
              .padding(horizontal = 16.dp, vertical = 10.dp)
              .testTag("recent_item_${item.id}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Delete action in edit mode
            AnimatedVisibility(
              visible = isEditMode,
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              IconButton(
                onClick = { onDeleteRecord(item.id) },
                modifier = Modifier
                  .padding(end = 8.dp)
                  .size(28.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.RemoveCircle,
                  contentDescription = "Delete call entry",
                  tint = IosRed,
                  modifier = Modifier.size(24.dp),
                )
              }
            }

            // Circular Avatar (Initial or Silhouette)
            if (item.contactName.firstOrNull()?.isLetter() == true) {
              ContactAvatar(
                initial = item.contactName.first().uppercaseChar(),
                colorIndex = (item.id.hashCode().coerceAtLeast(0) % 5),
                size = 46.dp,
              )
            } else {
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
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Contact Name / Number & Subtitle
            Column(
              modifier = Modifier.weight(1f),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = item.contactName,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = titleColor,
                )
                if (item.repeatCount > 1) {
                  Text(
                    text = " (${item.repeatCount})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                  )
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
              ) {
                val arrowIcon = when (item.callType) {
                  CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
                  CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                  CallType.MISSED -> Icons.Filled.CallMissed
                }
                Icon(
                  imageVector = arrowIcon,
                  contentDescription = null,
                  tint = colors.textSecondary,
                  modifier = Modifier.size(13.dp),
                )
                Text(
                  text = "${item.phoneType.lowercase()} • ${item.phoneNumber}",
                  fontSize = 13.sp,
                  color = colors.textSecondary,
                )
              }
            }

            // Time / Date
            Text(
              text = item.timeFormatted,
              fontSize = 14.sp,
              color = colors.textSecondary,
              modifier = Modifier.padding(end = 8.dp),
            )

            // Circular Info (i) button in iOS Blue
            IconButton(
              onClick = { /* Info details */ },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Call Info",
                tint = IosBlue,
                modifier = Modifier.size(22.dp),
              )
            }
          }

          HorizontalDivider(
            modifier = Modifier.padding(start = 76.dp),
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
}
