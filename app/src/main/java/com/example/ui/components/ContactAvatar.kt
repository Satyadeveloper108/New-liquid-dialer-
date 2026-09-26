package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

private val AvatarGradients = listOf(
  listOf(Color(0xFF8E8E93), Color(0xFF636366)),
  listOf(Color(0xFF007AFF), Color(0xFF0051A8)),
  listOf(Color(0xFF34C759), Color(0xFF248A3D)),
  listOf(Color(0xFFFF9500), Color(0xFFC97500)),
  listOf(Color(0xFFAF52DE), Color(0xFF7B33A0)),
  listOf(Color(0xFF5856D6), Color(0xFF3C3A9E)),
  listOf(Color(0xFFFF2D55), Color(0xFFC0183A)),
  listOf(Color(0xFF5AC8FA), Color(0xFF3291B8)),
)

// Precomputed static brushes to eliminate allocation during scrolling
private val AvatarGradientBrushes = AvatarGradients.map { Brush.verticalGradient(it) }

@Composable
fun ContactAvatar(
  initial: Char,
  colorIndex: Int = 0,
  size: Dp = 44.dp,
  photoUri: String? = null,
  modifier: Modifier = Modifier,
) {
  if (!photoUri.isNullOrBlank()) {
    val context = LocalContext.current
    val imageRequest = remember(photoUri) {
      ImageRequest.Builder(context)
        .data(photoUri)
        .crossfade(false)
        .build()
    }
    AsyncImage(
      model = imageRequest,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = modifier
        .size(size)
        .clip(CircleShape),
    )
  } else {
    val brush = AvatarGradientBrushes[colorIndex.coerceIn(0, AvatarGradientBrushes.lastIndex)]
    val fontSize = (size.value * 0.44f).sp

    Box(
      modifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(brush),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initial.toString(),
        color = Color.White,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
