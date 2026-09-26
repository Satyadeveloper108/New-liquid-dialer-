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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
  val icon: ImageVector,
)

private val tabs = listOf(
  TabItem(NavTab.FAVOURITES, R.string.tab_favourites, Icons.Filled.Star),
  TabItem(NavTab.RECENTS, R.string.tab_recents, Icons.Filled.Schedule),
  TabItem(NavTab.CONTACTS, R.string.tab_contacts, Icons.Filled.AccountCircle),
  TabItem(NavTab.KEYPAD, R.string.tab_keypad, Icons.Filled.Dialpad),
  TabItem(NavTab.VOICEMAIL, R.string.tab_voicemail, Icons.Filled.Voicemail),
)

/**
 * Floating Liquid Glass iOS Bottom Navigation Bar.
 *
 * Light Mode:
 * - Frosted white surface infused with subtle black/charcoal tint (6%–10% alpha)
 * - Noticeable separation from the white background
 * - Hairline dual-tone border with top highlight catch and dark-neutral rim
 * - Diffused ambient drop shadow giving physical floating depth
 * - Top edge specular reflection line
 * - Selected tab in subtle translucent blue capsule
 *
 * Dark Mode:
 * - Preserved dark charcoal glass with high-contrast active state
 */
@Composable
fun IosBottomNav(
  currentTab: NavTab,
  unreadVoicemails: Int,
  onSelectTab: (NavTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val navShape = RoundedCornerShape(36.dp)

  // 1. Surface Gradient
  val lightNavTintGradient = Brush.verticalGradient(
    colors = listOf(
      Color(0x0F000000), // ~6% dark neutral tint at top
      Color(0x1A000000), // ~10% dark neutral tint at bottom
    ),
  )

  // 2. Hairline Border Brush
  val lightBorderBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0x99FFFFFF), // Reflective top catch light
      Color(0x2B000000), // ~17% dark-neutral bottom rim
    ),
  )

  val darkBorderBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0x33FFFFFF),
      Color(0x1AFFFFFF),
    ),
  )

  // 3. Shadow Parameters
  val elevation = if (colors.isDark) 8.dp else 10.dp
  val spotShadowColor = if (colors.isDark) Color(0x50000000) else Color(0x26000000)
  val ambientShadowColor = if (colors.isDark) Color(0x20000000) else Color(0x12000000)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = elevation,
          shape = navShape,
          spotColor = spotShadowColor,
          ambientColor = ambientShadowColor,
        )
        .clip(navShape)
        .then(
          if (colors.isDark) {
            Modifier.background(Color(0xF01C1C1E))
          } else {
            // Light Mode: Frosted white + smoky charcoal depth gradient
            Modifier
              .background(Color(0xEEFFFFFF))
              .background(lightNavTintGradient)
          }
        )
        .border(
          width = 0.65.dp,
          brush = if (colors.isDark) darkBorderBrush else lightBorderBrush,
          shape = navShape,
        ),
    ) {
      // Subtle top specular highlight reflection line in Light Mode
      if (!colors.isDark) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color(0x80FFFFFF),
                  Color.Transparent,
                ),
              ),
            ),
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(58.dp)
          .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabs.forEach { item ->
          val isSelected = currentTab == item.tab
          val title = stringResource(id = item.titleRes)
          val itemInteractionSource = remember { MutableInteractionSource() }

          val targetContentColor = if (isSelected) {
            IosBlue
          } else {
            if (colors.isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
          }

          val animatedColor by animateColorAsState(
            targetValue = targetContentColor,
            animationSpec = tween(durationMillis = 150),
            label = "tab_color_anim",
          )

          val pillShape = RoundedCornerShape(20.dp)
          val pillBgColor = if (isSelected) {
            if (colors.isDark) Color(0x33FFFFFF) else Color(0x1A007AFF)
          } else {
            Color.Transparent
          }

          val pillBorderColor = if (isSelected) {
            if (colors.isDark) Color(0x40FFFFFF) else Color(0x29007AFF)
          } else {
            Color.Transparent
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(pillShape)
              .background(pillBgColor)
              .border(0.5.dp, pillBorderColor, pillShape)
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
                  imageVector = item.icon,
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
                      .testTag("voicemail_badge"),
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
                color = animatedColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
              )
            }
          }
        }
      }
    }
  }
}
