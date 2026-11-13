package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.AmberStar
import com.example.ui.theme.EmeraldSuccess

enum class BottomNavTab(
  val route: String,
  val labelKey: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  HOME("home", "nav_home", Icons.Filled.Home, Icons.Outlined.Home),
  EXPLORE("explore", "nav_explore", Icons.Filled.Explore, Icons.Outlined.Explore),
  MATCHES("matches", "nav_matches", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz),
  REQUESTS("requests", "nav_requests", Icons.Filled.Handshake, Icons.Outlined.Handshake),
  PROFILE("profile", "nav_profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun UserAvatarBadge(
  name: String,
  photoUri: String = "",
  isOnline: Boolean = false,
  size: Dp = 48.dp,
  modifier: Modifier = Modifier
) {
  val initials = name.trim()
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "SS" }

  Box(modifier = modifier.size(size)) {
    if (photoUri.isNotBlank()) {
      AsyncImage(
        model = photoUri,
        contentDescription = "$name profile photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(size)
          .clip(CircleShape)
          .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
      )
    } else {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(size)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
              )
            )
          )
      ) {
        Text(
          text = initials,
          color = Color.White,
          style = if (size >= 60.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    }
    if (isOnline) {
      Box(
        modifier = Modifier
          .size((size.value * 0.26f).dp.coerceAtLeast(10.dp))
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(EmeraldSuccess)
          .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
      )
    }
  }
}

@Composable
fun MatchPercentagePill(
  percentage: Int,
  modifier: Modifier = Modifier
) {
  val bgColor = when {
    percentage >= 80 -> MaterialTheme.colorScheme.secondaryContainer
    percentage >= 55 -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.tertiaryContainer
  }
  val textColor = when {
    percentage >= 80 -> MaterialTheme.colorScheme.onSecondaryContainer
    percentage >= 55 -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.onTertiaryContainer
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(50),
    modifier = modifier
  ) {
    Text(
      text = "$percentage% Match",
      color = textColor,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillChipGroup(
  skills: List<String>,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
  emptyFallback: String = "None added yet"
) {
  if (skills.isEmpty()) {
    Text(
      text = emptyFallback,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  } else {
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      skills.forEach { skill ->
        Surface(
          color = containerColor,
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = skill,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  testTag: String = "empty_state_action"
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(56.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = onAction,
          modifier = Modifier.testTag(testTag)
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun InteractiveStarRating(
  rating: Int,
  onRatingSelected: (Int) -> Unit
) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    (1..5).forEach { star ->
      IconButton(
        onClick = { onRatingSelected(star) },
        modifier = Modifier.testTag("star_rating_$star")
      ) {
        Icon(
          imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
          contentDescription = "$star stars",
          tint = if (star <= rating) AmberStar else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(30.dp)
        )
      }
    }
  }
}

@Composable
fun ReadOnlyRatingBadge(
  ratingAvg: Double,
  reviewCount: Long = 0L
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(AmberStar.copy(alpha = 0.16f))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Icon(
      imageVector = Icons.Filled.Star,
      contentDescription = "Rating",
      tint = AmberStar,
      modifier = Modifier.size(14.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    val formatted = String.format("%.1f", ratingAvg)
    Text(
      text = if (reviewCount > 0) "$formatted ($reviewCount)" else formatted,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun StatusPill(
  label: String,
  containerColor: Color,
  contentColor: Color,
  onClick: (() -> Unit)? = null
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(50),
    modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
  ) {
    Text(
      text = label,
      color = contentColor,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}
