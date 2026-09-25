package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NavTab
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIosColors

data class TabItem(
  val tab: NavTab,
  val titleRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val tabs = listOf(
  TabItem(NavTab.FAVOURITES, R.string.tab_favourites, Icons.Filled.Star, Icons.Outlined.StarOutline),
  TabItem(NavTab.RECENTS, R.string.tab_recents, Icons.Filled.Schedule, Icons.Outlined.Schedule),
  TabItem(NavTab.CONTACTS, R.string.tab_contacts, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
  TabItem(NavTab.KEYPAD, R.string.tab_keypad, Icons.Filled.Dialpad, Icons.Outlined.Dialpad),
  TabItem(NavTab.VOICEMAIL, R.string.tab_voicemail, Icons.Filled.Voicemail, Icons.Outlined.Voicemail),
)

@Composable
fun IosBottomNav(
  currentTab: NavTab,
  unreadVoicemails: Int,
  onSelectTab: (NavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val navShape = RoundedCornerShape(32.dp)

  // Translucent glass surface background
  val glassBgColor = if (colors.isDark) {
    Color(0xE61E1E22) // Translucent dark charcoal
  } else {
    Color(0xF0FFFFFF) // Translucent frost white
  }

  // Border outline for subtle glass effect
  val borderColor = if (colors.isDark) {
    Color(0x33FFFFFF)
  } else {
    Color(0x1F000000)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 14.dp,
          shape = navShape,
          spotColor = if (colors.isDark) Color(0x66000000) else Color(0x24000000),
          ambientColor = if (colors.isDark) Color(0x33000000) else Color(0x1A000000),
        )
        .border(width = 0.8.dp, color = borderColor, shape = navShape),
      shape = navShape,
      color = glassBgColor,
      tonalElevation = 6.dp,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
          .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabs.forEach { item ->
          val isSelected = currentTab == item.tab
          val title = stringResource(id = item.titleRes)

          val itemInteractionSource = remember { MutableInteractionSource() }

          // Color animations for smooth transition
          val targetColor = if (isSelected) {
            IosBlue
          } else {
            if (colors.isDark) Color(0xFF98989D) else Color(0xFF000000)
          }

          val animatedColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 200),
            label = "tab_color_anim",
          )

          val pillBgColor = if (isSelected) {
            if (colors.isDark) Color(0x29007AFF) else Color(0x1A007AFF)
          } else {
            Color.Transparent
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .clip(RoundedCornerShape(25.dp))
              .background(pillBgColor)
              .clickable(
                interactionSource = itemInteractionSource,
                indication = null,
              ) {
                onSelectTab(item.tab)
              }
              .testTag("tab_${item.tab.name.lowercase()}"),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                  imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = title,
                  tint = animatedColor,
                  modifier = Modifier.size(24.dp),
                )

                // Voicemail unread indicator badge
                if (item.tab == NavTab.VOICEMAIL && unreadVoicemails > 0) {
                  Box(
                    modifier = Modifier
                      .offset(x = 8.dp, y = (-4).dp)
                      .size(16.dp)
                      .clip(CircleShape)
                      .background(IosRed)
                      .border(1.dp, glassBgColor, CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = unreadVoicemails.toString(),
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      lineHeight = 10.sp,
                    )
                  }
                }
              }

              Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = animatedColor,
                modifier = Modifier.padding(top = 2.dp),
              )
            }
          }
        }
      }
    }
  }
}
