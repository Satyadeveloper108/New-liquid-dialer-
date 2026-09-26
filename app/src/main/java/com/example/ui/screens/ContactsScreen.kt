package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors
import kotlinx.coroutines.launch

/**
 * Contacts Screen matching original iOS reference:
 * - Top bar with circular back button, centered title, and circular add button
 * - Rounded search capsule with black search and mic icons
 * - "My Card" profile header
 * - Large readable avatars with bold contact names
 * - Blue alphabetical index on right
 */
@Composable
fun ContactsScreen(
  contacts: List<Contact>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onContactClick: (Contact) -> Unit,
  onAddContactClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  val groupedContacts = contacts.groupBy { it.initial }
  val alphabet = ('A'..'Z').toList()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding(),
  ) {
    // 1. Top Bar matching iOS: Back Circle | Centered "Contacts" | Add Circle
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
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
          .clickable { /* Back action */ },
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = colors.textPrimary,
          modifier = Modifier.size(20.dp),
        )
      }

      Text(
        text = stringResource(id = R.string.tab_contacts),
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
          .clickable { onAddContactClick() }
          .testTag("add_contact_button"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "Add Contact",
          tint = colors.textPrimary,
          modifier = Modifier.size(22.dp),
        )
      }
    }

    // 2. Search Capsule Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      val searchBg = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
      val searchBorder = if (colors.isDark) Color(0x26FFFFFF) else Color(0x14000000)

      TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
          Text(
            text = stringResource(id = R.string.search_placeholder),
            color = colors.textSecondary,
            fontSize = 16.sp,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
              )
            }
          } else {
            IconButton(onClick = { }) {
              Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Voice",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
          focusedContainerColor = searchBg,
          unfocusedContainerColor = searchBg,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          cursorColor = IosBlue,
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .border(0.5.dp, searchBorder, RoundedCornerShape(22.dp))
          .testTag("search_bar"),
      )
    }

    // 3. Main Contact List + Alphabet Scrubber
    Box(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
      ) {
        // My Card Section
        if (searchQuery.isEmpty()) {
          item(key = "my_card") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("my_card_item"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ContactAvatar(
                initial = 'G',
                colorIndex = 1,
                size = 52.dp,
              )
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = "Gyanaranjan Jena",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Bold,
                  color = colors.textPrimary,
                )
                Text(
                  text = stringResource(id = R.string.my_card),
                  fontSize = 13.sp,
                  color = colors.textSecondary,
                )
              }
            }
            HorizontalDivider(
              modifier = Modifier.padding(start = 82.dp),
              thickness = 0.5.dp,
              color = colors.separator,
            )
          }
        }

        // Alphabetically Grouped Contacts
        groupedContacts.forEach { (initial, contactList) ->
          item(key = "header_$initial") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
              Text(
                text = initial.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
              )
            }
          }

          items(contactList, key = { it.id }) { contact ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onContactClick(contact) }
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("contact_item_${contact.id}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ContactAvatar(
                initial = contact.initial,
                colorIndex = contact.avatarColorIndex,
                size = 46.dp,
              )
              Spacer(modifier = Modifier.width(14.dp))
              Text(
                text = contact.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
              )
            }
            HorizontalDivider(
              modifier = Modifier.padding(start = 76.dp),
              thickness = 0.5.dp,
              color = colors.separator,
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(72.dp))
        }
      }

      // 4. Alphabet Scrubber Bar on Right Edge
      Column(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        alphabet.forEach { letter ->
          Text(
            text = letter.toString(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = IosBlue,
            modifier = Modifier
              .clickable {
                val index = contacts.indexOfFirst { it.initial == letter }
                if (index >= 0) {
                  scope.launch { listState.animateScrollToItem(index) }
                }
              }
              .padding(vertical = 0.5.dp, horizontal = 2.dp),
          )
        }
        Text(
          text = "#",
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          color = IosBlue,
          modifier = Modifier.padding(vertical = 0.5.dp, horizontal = 2.dp),
        )
      }
    }
  }
}
