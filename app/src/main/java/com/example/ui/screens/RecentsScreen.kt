package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.RecentsFilter
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIosColors

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
    // Header with Edit button, SegmentedControl, and Title
    ScreenHeader(
      title = stringResource(id = R.string.tab_recents),
      leadingAction = {
        TextButton(
          onClick = { isEditMode = !isEditMode },
          modifier = Modifier.testTag("recents_edit_button"),
        ) {
          Text(
            text = if (isEditMode) "Done" else "Edit",
            color = IosBlue,
            fontSize = 17.sp,
            fontWeight = if (isEditMode) FontWeight.SemiBold else FontWeight.Normal,
          )
        }
      },
      centerContent = {
        SegmentedControl(
          selectedFilter = selectedFilter,
          onFilterSelected = onFilterSelected,
          modifier = Modifier.width(180.dp),
        )
      },
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
              .padding(horizontal = 16.dp, vertical = 12.dp)
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

            // Direction / Type Icon
            val (icon, tint) = when (item.callType) {
              CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to colors.textSecondary
              CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to colors.textSecondary
              CallType.MISSED -> Icons.Filled.CallMissed to IosRed
            }

            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = tint,
              modifier = Modifier
                .padding(end = 12.dp)
                .size(16.dp),
            )

            // Contact Name / Number & Type
            Column(
              modifier = Modifier.weight(1f),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = item.contactName,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = titleColor,
                )
                if (item.repeatCount > 1) {
                  Text(
                    text = " (${item.repeatCount})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                  )
                }
              }

              Text(
                text = "${item.phoneType} • ${item.phoneNumber}",
                fontSize = 13.sp,
                color = colors.textSecondary,
              )
            }

            // Time / Date
            Text(
              text = item.timeFormatted,
              fontSize = 14.sp,
              color = colors.textSecondary,
              modifier = Modifier.padding(end = 8.dp),
            )

            // Info (i) button
            IconButton(
              onClick = {
                // Info dialog / details
              },
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
            modifier = Modifier.padding(start = 44.dp),
            thickness = 0.5.dp,
            color = colors.separator,
          )
        }
      }
    }
  }
}
