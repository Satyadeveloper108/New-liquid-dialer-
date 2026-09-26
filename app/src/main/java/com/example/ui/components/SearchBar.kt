package com.example.ui.components

import androidx.compose.foundation.border
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
  val capsuleShape = RoundedCornerShape(24.dp)
  val containerBg = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
  val borderColor = if (colors.isDark) Color(0x2EFFFFFF) else Color(0x14000000)

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
          color = Color(0xFF8E8E93),
          fontSize = 17.sp,
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = "Search",
          tint = if (colors.isDark) Color.White else Color.Black,
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
              tint = if (colors.isDark) Color.White else Color.Black,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      },
      singleLine = true,
      colors = TextFieldDefaults.colors(
        focusedContainerColor = containerBg,
        unfocusedContainerColor = containerBg,
        disabledContainerColor = containerBg,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = IosBlue,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
      ),
      shape = capsuleShape,
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp)
        .border(0.5.dp, borderColor, capsuleShape)
        .testTag(testTag),
    )
  }
}
