package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactDialog(
  onDismiss: () -> Unit,
  onSaveContact: (name: String, phone: String, type: String) -> Unit,
) {
  val colors = LocalIosColors.current
  var firstName by remember { mutableStateOf("") }
  var lastName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var phoneType by remember { mutableStateOf("mobile") }

  val canSave = (firstName.isNotBlank() || lastName.isNotBlank()) && phoneNumber.isNotBlank()

  BasicAlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .clip(RoundedCornerShape(20.dp)),
      color = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Navigation Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(onClick = onDismiss) {
            Text(
              text = "Cancel",
              color = IosBlue,
              fontSize = 17.sp,
            )
          }

          Text(
            text = "New Contact",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = colors.textPrimary,
          )

          TextButton(
            onClick = {
              if (canSave) {
                val fullName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")
                onSaveContact(fullName, phoneNumber.trim(), phoneType)
              }
            },
            enabled = canSave,
          ) {
            Text(
              text = "Done",
              color = if (canSave) IosBlue else colors.textSecondary.copy(alpha = 0.5f),
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar Preview Circle
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(46.dp),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Real Phone Contact",
          fontSize = 13.sp,
          color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Grouped Fields: Name
        val cardBg = if (colors.isDark) Color(0xFF2C2C2E) else Color.White
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg),
        ) {
          TextField(
            value = firstName,
            onValueChange = { firstName = it },
            placeholder = { Text("First name", color = colors.textSecondary) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
              focusedContainerColor = cardBg,
              unfocusedContainerColor = cardBg,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().testTag("add_contact_first_name"),
          )

          HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

          TextField(
            value = lastName,
            onValueChange = { lastName = it },
            placeholder = { Text("Last name", color = colors.textSecondary) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
              focusedContainerColor = cardBg,
              unfocusedContainerColor = cardBg,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().testTag("add_contact_last_name"),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grouped Fields: Phone Number & Label
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg),
        ) {
          TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            placeholder = { Text("Phone number", color = colors.textSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            colors = TextFieldDefaults.colors(
              focusedContainerColor = cardBg,
              unfocusedContainerColor = cardBg,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().testTag("add_contact_phone_number"),
          )

          HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Label",
              color = colors.textSecondary,
              fontSize = 15.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("mobile", "home", "work").forEach { type ->
                val isSelected = phoneType == type
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) IosBlue else Color.Transparent)
                    .border(
                      width = 1.dp,
                      color = if (isSelected) IosBlue else colors.separator,
                      shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { phoneType = type }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Text(
                    text = type.replaceFirstChar { it.uppercase() },
                    color = if (isSelected) Color.White else colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
