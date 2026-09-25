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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SearchBar
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors
import kotlinx.coroutines.launch

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

  // Group contacts by first letter
  val groupedContacts = contacts.groupBy { it.initial }
  val alphabet = ('A'..'Z').toList()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding(),
  ) {
    // Header with title and Add button
    ScreenHeader(
      title = stringResource(id = R.string.tab_contacts),
      trailingAction = {
        IconButton(
          onClick = onAddContactClick,
          modifier = Modifier.testTag("add_contact_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add Contact",
            tint = IosBlue,
            modifier = Modifier.size(28.dp),
          )
        }
      },
    )

    // Search Bar
    SearchBar(
      query = searchQuery,
      onQueryChange = onSearchQueryChange,
      placeholderText = stringResource(id = R.string.search_placeholder),
      onVoiceSearchClick = { },
    )

    // Main Contact List + Alphabet Scrubber
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("my_card_item"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ContactAvatar(
                initial = 'U',
                colorIndex = 1,
                size = 56.dp,
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "My Contact",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.SemiBold,
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
              modifier = Modifier.padding(start = 92.dp),
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
                .background(colors.groupedBackground)
                .padding(horizontal = 20.dp, vertical = 4.dp),
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("contact_item_${contact.id}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ContactAvatar(
                initial = contact.initial,
                colorIndex = contact.avatarColorIndex,
                size = 40.dp,
              )
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = contact.name,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = colors.textPrimary,
                )
                Text(
                  text = contact.phoneNumber,
                  fontSize = 13.sp,
                  color = colors.textSecondary,
                )
              }
            }
            HorizontalDivider(
              modifier = Modifier.padding(start = 74.dp),
              thickness = 0.5.dp,
              color = colors.separator,
            )
          }
        }
      }

      // Right-side Alphabet Index Scrubber
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
            color = IosBlue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clickable {
                scope.launch {
                  // Find index of header or item
                  val index = groupedContacts.keys.indexOf(letter)
                  if (index != -1) {
                    listState.animateScrollToItem(index)
                  }
                }
              }
              .padding(vertical = 1.dp, horizontal = 4.dp),
          )
        }
      }
    }
  }
}
