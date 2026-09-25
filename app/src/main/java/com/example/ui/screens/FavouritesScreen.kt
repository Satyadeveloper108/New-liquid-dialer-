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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Contact
import com.example.ui.components.ContactAvatar
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

@Composable
fun FavouritesScreen(
  favourites: List<Contact>,
  onContactClick: (Contact) -> Unit,
  onAddFavouriteClick: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding(),
  ) {
    // Header with title and Add button
    ScreenHeader(
      title = stringResource(id = R.string.tab_favourites),
      trailingAction = {
        IconButton(
          onClick = onAddFavouriteClick,
          modifier = Modifier.testTag("add_favourite_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add Favourite",
            tint = IosBlue,
            modifier = Modifier.size(28.dp),
          )
        }
      },
    )

    if (favourites.isEmpty()) {
      // Empty State
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(colors.keypadButtonBg),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.StarBorder,
              contentDescription = null,
              tint = colors.textSecondary,
              modifier = Modifier.size(36.dp),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(id = R.string.no_favourites),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(id = R.string.no_favourites_description),
            fontSize = 15.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
      ) {
        items(favourites, key = { it.id }) { contact ->
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
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = contact.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
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
                .background(colors.keypadButtonBg),
            ) {
              Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Call ${contact.name}",
                tint = CallAcceptGreen,
                modifier = Modifier.size(20.dp),
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Star toggler
            IconButton(
              onClick = { onToggleFavorite(contact.id) },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Remove Favourite",
                tint = IosBlue,
                modifier = Modifier.size(22.dp),
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
