package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.Contact
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.LocalIosColors

/**
 * Favourites Screen matching original iOS reference:
 * - Centered "Favourites" title
 * - Top-right circular "+" button in a thin-bordered pill
 * - Clean white background
 * - Centered bold "No Favourites" text in empty state
 */
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun FavouritesScreen(
  favourites: List<Contact>,
  onContactClick: (Contact) -> Unit,
  onAddFavouriteClick: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
) {
  val colors = LocalIosColors.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding(),
  ) {
    // Top Bar matching iOS: Centered Title + Top-Right Circular Add Button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Spacer(modifier = Modifier.size(36.dp))

      Text(
        text = stringResource(id = R.string.tab_favourites),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
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
          .clickable { onAddFavouriteClick() }
          .testTag("add_favourite_button"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "Add Favourite",
          tint = colors.textPrimary,
          modifier = Modifier.size(22.dp),
        )
      }
    }

    if (favourites.isEmpty()) {
      // Empty State: Vertically and horizontally centered "No Favourites"
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(id = R.string.no_favourites),
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = colors.textPrimary,
        )
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
      ) {
        items(
          items = favourites,
          key = { it.id },
          contentType = { "favourite_contact" },
        ) { contact ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onContactClick(contact) }
              .padding(horizontal = 20.dp, vertical = 12.dp)
              .testTag("favourite_item_${contact.id}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ContactAvatar(
              initial = contact.initial,
              colorIndex = contact.avatarColorIndex,
              size = 48.dp,
              photoUri = contact.photoUri,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = contact.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
              )
              Text(
                text = "${contact.type} • ${contact.phoneNumber}",
                fontSize = 13.sp,
                color = colors.textSecondary,
              )
            }

            // Quick Call Icon
            IconButton(
              onClick = { onContactClick(contact) },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)),
            ) {
              Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Call ${contact.name}",
                tint = CallAcceptGreen,
                modifier = Modifier.size(20.dp),
              )
            }
          }

          HorizontalDivider(
            modifier = Modifier.padding(start = 84.dp),
            thickness = 0.5.dp,
            color = colors.separator,
          )
        }
      }
    }
  }
}
