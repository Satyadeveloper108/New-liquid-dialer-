package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalIosColors

@Composable
fun SearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholderText: String = "Search",
  modifier: Modifier = Modifier,
  onVoiceSearchClick: (() -> Unit)? = null,
  testTag: String = "search_bar",
) {
  val colors = LocalIosColors.current

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    TextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = {
        Text(
          text = placeholderText,
          color = colors.textSecondary,
          fontSize = 16.sp,
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = "Search",
          tint = colors.textSecondary,
          modifier = Modifier.size(20.dp),
        )
      },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(
            onClick = { onQueryChange("") },
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Clear search",
              tint = colors.textSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        } else if (onVoiceSearchClick != null) {
          IconButton(
            onClick = onVoiceSearchClick,
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Mic,
              contentDescription = "Voice Search",
              tint = colors.textSecondary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      },
      singleLine = true,
      colors = TextFieldDefaults.colors(
        focusedContainerColor = colors.keypadButtonBg,
        unfocusedContainerColor = colors.keypadButtonBg,
        disabledContainerColor = colors.keypadButtonBg,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = IosBlue,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag(testTag),
    )
  }
}
