package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecentsFilter
import com.example.ui.theme.LocalIosColors

@Composable
fun SegmentedControl(
  selectedFilter: RecentsFilter,
  onFilterSelected: (RecentsFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val items = listOf(RecentsFilter.ALL to "All", RecentsFilter.MISSED to "Missed")

  Box(
    modifier = modifier
      .height(32.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
      .padding(2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxHeight(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items.forEach { (filter, label) ->
        val isSelected = selectedFilter == filter
        val textColor by animateColorAsState(
          targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
          animationSpec = tween(durationMillis = 200),
          label = "segmentedTextColor",
        )

        val itemModifier = if (isSelected) {
          Modifier
            .weight(1f)
            .fillMaxHeight()
            .shadow(1.dp, RoundedCornerShape(7.dp))
            .background(
              if (colors.isDark) Color(0xFF636366) else Color.White,
              RoundedCornerShape(7.dp),
            )
        } else {
          Modifier
            .weight(1f)
            .fillMaxHeight()
        }

        Box(
          modifier = itemModifier
            .clip(RoundedCornerShape(7.dp))
            .clickable { onFilterSelected(filter) }
            .testTag("segment_${label.lowercase()}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
          )
        }
      }
    }
  }
}
