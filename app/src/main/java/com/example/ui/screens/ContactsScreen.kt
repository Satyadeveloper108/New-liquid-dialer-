package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.Contact
import com.example.ui.components.AddContactDialog
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
  contacts: List<Contact>,
  searchQuery: String,
  hasContactsPermission: Boolean,
  isRealDeviceContacts: Boolean,
  isLoadingContacts: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onContactClick: (Contact) -> Unit,
  onSaveNewContact: (name: String, phone: String, type: String) -> Unit,
  onSeedDemoContacts: () -> Unit,
  onPermissionResult: (Boolean) -> Unit,
  onToggleFavorite: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalIosColors.current
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  var showAddDialog by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { isGranted ->
    onPermissionResult(isGranted)
  }

  val groupedContacts = remember(contacts) { contacts.groupBy { it.initial } }
  val letterToScrollIndex = remember(groupedContacts, searchQuery, hasContactsPermission, contacts.isEmpty(), isLoadingContacts) {
    var currentIndex = 0
    if (searchQuery.isEmpty()) {
      currentIndex++ // "my_card"
    }
    if (hasContactsPermission && contacts.isEmpty() && !isLoadingContacts) {
      currentIndex++ // "empty_device_contacts"
    }
    val map = mutableMapOf<Char, Int>()
    groupedContacts.forEach { (initial, list) ->
      map[initial] = currentIndex
      currentIndex += 1 + list.size // header + list items
    }
    map
  }

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

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = stringResource(id = R.string.tab_contacts),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = colors.textPrimary,
        )
        if (hasContactsPermission) {
          Text(
            text = "${contacts.size} contacts",
            fontSize = 11.sp,
            color = colors.textSecondary,
          )
        }
      }

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
          .clickable { showAddDialog = true }
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

    // 3. Permission Banner if Contacts Permission is NOT yet granted
    if (!hasContactsPermission) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFEBF3FF))
          .border(0.5.dp, IosBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
          .padding(14.dp),
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.ContactPhone,
              contentDescription = null,
              tint = IosBlue,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Device Contacts Sync",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = colors.textPrimary,
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Grant contacts permission to read and search real contacts saved on this phone.",
            fontSize = 13.sp,
            color = colors.textSecondary,
            lineHeight = 18.sp,
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedButton(
              onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
              colors = ButtonDefaults.elevatedButtonColors(
                containerColor = IosBlue,
                contentColor = Color.White,
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("allow_contacts_permission_btn"),
            ) {
              Text("Allow Contacts Access", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. Main Contact List + Alphabet Scrubber
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
                initial = 'M',
                colorIndex = 1,
                size = 52.dp,
              )
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = "My Card",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Bold,
                  color = colors.textPrimary,
                )
                Text(
                  text = if (hasContactsPermission) "Real Device Contacts Connected" else "Preview Mode",
                  fontSize = 13.sp,
                  color = if (hasContactsPermission) Color(0xFF34C759) else colors.textSecondary,
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

        // Empty State: Device contacts enabled but 0 contacts on device
        if (hasContactsPermission && contacts.isEmpty() && !isLoadingContacts) {
          item(key = "empty_device_contacts") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Filled.ContactPhone,
                  contentDescription = null,
                  tint = colors.textSecondary,
                  modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "No Contacts Found On Device",
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp,
                  color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Add contacts or populate demo contacts to test real device calling and dialing.",
                  fontSize = 14.sp,
                  color = colors.textSecondary,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  ElevatedButton(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = IosBlue, contentColor = Color.White),
                  ) {
                    Text("+ Add Contact")
                  }
                  OutlinedButton(
                    onClick = { onSeedDemoContacts() },
                  ) {
                    Text("Populate Demo Contacts")
                  }
                }
              }
            }
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("contact_item_${contact.id}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ContactAvatar(
                initial = contact.initial,
                colorIndex = contact.avatarColorIndex,
                size = 46.dp,
                photoUri = contact.photoUri,
              )
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
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

              // Favorite Star Toggle
              IconButton(onClick = { onToggleFavorite(contact.id) }) {
                Icon(
                  imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                  contentDescription = "Favorite",
                  tint = if (contact.isFavorite) Color(0xFFFFCC00) else colors.textSecondary,
                  modifier = Modifier.size(20.dp),
                )
              }

              // Call Quick Button
              IconButton(onClick = { onContactClick(contact) }) {
                Icon(
                  imageVector = Icons.Filled.Call,
                  contentDescription = "Call",
                  tint = Color(0xFF34C759),
                  modifier = Modifier.size(20.dp),
                )
              }
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

      // 5. Alphabet Scrubber Bar on Right Edge
      if (contacts.isNotEmpty()) {
        Column(
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          AlphabetList.forEach { letter ->
            Text(
              text = letter.toString(),
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              color = IosBlue,
              modifier = Modifier
                .clickable {
                  val targetIndex = letterToScrollIndex[letter]
                  if (targetIndex != null) {
                    scope.launch { listState.scrollToItem(targetIndex) }
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

  // 6. Add Contact Modal Dialog
  if (showAddDialog) {
    AddContactDialog(
      onDismiss = { showAddDialog = false },
      onSaveContact = { name, phone, type ->
        onSaveNewContact(name, phone, type)
        showAddDialog = false
      },
    )
  }
}

private val AlphabetList = ('A'..'Z').toList()
