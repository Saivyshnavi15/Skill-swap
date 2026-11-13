package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.AppNotification
import com.example.model.ExchangeRequest
import com.example.model.ExchangeStatus
import com.example.model.LearningGoal
import com.example.model.LearningSession
import com.example.model.PublicProfile
import com.example.model.SessionStatus
import com.example.model.SkillCategory
import com.example.model.SkillMatchResult
import com.example.model.UserSkill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MatchPercentagePill
import com.example.ui.components.ReadOnlyRatingBadge
import com.example.ui.components.SkillChipGroup
import com.example.ui.components.UserAvatarBadge
import com.example.ui.theme.AmberStar
import com.example.ui.viewmodel.ExploreFilterState

@Composable
fun HomeScreen(
  profile: PublicProfile,
  mySkills: List<UserSkill>,
  myGoals: List<LearningGoal>,
  matches: List<SkillMatchResult>,
  requests: List<ExchangeRequest>,
  sessions: List<LearningSession>,
  notifications: List<AppNotification>,
  appLanguage: AppLanguage,
  t: (String) -> String,
  onOpenNotifications: () -> Unit,
  onOpenChatList: () -> Unit,
  onOpenLanguageSettings: () -> Unit,
  onOpenAddSkill: () -> Unit,
  onOpenLearningGoals: () -> Unit,
  onFindMatch: () -> Unit,
  onOpenMatchDetails: (String) -> Unit,
  onOpenProjects: () -> Unit,
  onOpenRequests: () -> Unit,
  onOpenSessions: () -> Unit,
  onOpenReputation: () -> Unit
) {
  val topMatch = matches.firstOrNull()
  val pendingCount = requests.count { it.status == ExchangeStatus.PENDING.name }
  val upcomingSessions = sessions.filter { it.status == SessionStatus.SCHEDULED.name || it.status == SessionStatus.STARTED.name }
  val unreadNotifs = notifications.count { !it.isRead }
  val firstName = profile.displayName.split(" ").firstOrNull().orEmpty().ifBlank { "Learner" }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // Top Editorial Header
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${t("welcome_prefix")} $firstName",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = t("no_money_badge"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onOpenLanguageSettings,
            modifier = Modifier.testTag("home_language_button")
          ) {
            Icon(Icons.Default.Language, contentDescription = "Language (${appLanguage.nativeName})")
          }
          IconButton(
            onClick = onOpenChatList,
            modifier = Modifier.testTag("home_chat_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Open conversations")
          }
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.testTag("home_notifications_button")
          ) {
            BadgedBox(
              badge = {
                if (unreadNotifs > 0) {
                  Badge { Text(unreadNotifs.toString()) }
                }
              }
            ) {
              Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
          }
        }
      }
    }

    // Profile Completion + Reputation Strip
    item {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "${t("profile_completion")}: ${profile.profileCompletionPercent}%",
              style = MaterialTheme.typography.titleMedium
            )
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              shape = RoundedCornerShape(50),
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenReputation)
                .testTag("home_reputation_pill")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AmberStar, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${profile.reputationPoints} pts • ${profile.badges.size} badges",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onTertiaryContainer
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          LinearProgressIndicator(
            progress = { profile.profileCompletionPercent / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }
      }
    }

    // Quick Actions Grid (2x2)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = t("quick_actions"),
          style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          QuickActionTile(
            icon = Icons.Default.Add,
            label = t("add_skill"),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onOpenAddSkill,
            modifier = Modifier.weight(1f).testTag("quick_add_skill")
          )
          QuickActionTile(
            icon = Icons.Default.SwapHoriz,
            label = t("find_match"),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onFindMatch,
            modifier = Modifier.weight(1f).testTag("quick_find_match")
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          QuickActionTile(
            icon = Icons.Default.Workspaces,
            label = t("create_project"),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenProjects,
            modifier = Modifier.weight(1f).testTag("quick_create_project")
          )
          QuickActionTile(
            icon = Icons.Default.Handshake,
            label = "${t("my_requests")} ($pendingCount)",
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onOpenRequests,
            modifier = Modifier.weight(1f).testTag("quick_my_requests")
          )
        }
      }
    }

    // Recommended Match Spotlight
    item {
      Text(
        text = t("recommended_match"),
        style = MaterialTheme.typography.titleMedium
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (topMatch != null) {
        ElevatedCard(
          shape = MaterialTheme.shapes.extraLarge,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_recommended_match_card")
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                  )
                )
              )
              .padding(18.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  UserAvatarBadge(
                    name = topMatch.peerProfile.displayName,
                    photoUri = topMatch.peerProfile.photoUri,
                    isOnline = topMatch.peerProfile.isOnline,
                    size = 52.dp
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = topMatch.peerProfile.displayName,
                      style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                      text = topMatch.peerProfile.location.ifBlank { "SkillSwap Peer" },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                MatchPercentagePill(percentage = topMatch.matchPercentage)
              }

              Text(
                text = topMatch.reason,
                style = MaterialTheme.typography.bodyMedium
              )

              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Knows:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                SkillChipGroup(
                  skills = topMatch.peerProfile.teachSkillNames,
                  containerColor = MaterialTheme.colorScheme.surface,
                  contentColor = MaterialTheme.colorScheme.primary
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Wants:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                SkillChipGroup(
                  skills = topMatch.peerProfile.learnSkillNames,
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
              }

              Button(
                onClick = { onOpenMatchDetails(topMatch.peerProfile.userId) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("home_view_match_button")
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(t("view_match"))
              }
            }
          }
        }
      }
    }

    // Your Skills & Learning Goals
    item {
      Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(t("your_skills"), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenAddSkill, modifier = Modifier.testTag("home_manage_skills_button")) {
              Text("Manage (${mySkills.size})")
            }
          }
          SkillChipGroup(
            skills = mySkills.map { "${it.name} (${it.proficiency.lowercase().replaceFirstChar { c -> c.uppercase() }})" },
            emptyFallback = "You haven't added any skills yet. Tap Manage to add your first skill!"
          )

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(t("learning_goals"), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenLearningGoals, modifier = Modifier.testTag("home_manage_goals_button")) {
              Text("Goals (${myGoals.size})")
            }
          }
          SkillChipGroup(
            skills = myGoals.map { "${it.skillName} → ${it.targetLevel.lowercase().replaceFirstChar { c -> c.uppercase() }}" },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            emptyFallback = "Add skills you want to learn for sharper match scores!"
          )
        }
      }
    }

    // Upcoming Sessions + Projects Summary
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Card(
          onClick = onOpenSessions,
          shape = MaterialTheme.shapes.large,
          modifier = Modifier
            .weight(1f)
            .testTag("home_sessions_card")
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(t("upcoming_sessions"), style = MaterialTheme.typography.labelLarge)
            Text(
              text = "${upcomingSessions.size} active",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
        Card(
          onClick = onOpenProjects,
          shape = MaterialTheme.shapes.large,
          modifier = Modifier
            .weight(1f)
            .testTag("home_projects_card")
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.FolderShared, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(t("projects"), style = MaterialTheme.typography.labelLarge)
            Text(
              text = "Collaborate",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionTile(
  icon: ImageVector,
  label: String,
  color: androidx.compose.ui.graphics.Color,
  contentColor: androidx.compose.ui.graphics.Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color,
    shape = MaterialTheme.shapes.medium,
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
      Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        color = contentColor,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1
      )
    }
  }
}

@Composable
fun ExploreScreen(
  filter: ExploreFilterState,
  matches: List<SkillMatchResult>,
  t: (String) -> String,
  onUpdateFilter: ((ExploreFilterState) -> ExploreFilterState) -> Unit,
  onViewProfile: (String) -> Unit
) {
  val languages = listOf("All", "English", "Telugu", "Hindi", "Tamil", "Kannada", "Malayalam")
  val availabilityOptions = listOf("All", "Weekdays", "Weekends")

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text(
        text = t("nav_explore"),
        style = MaterialTheme.typography.headlineMedium
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = filter.query,
        onValueChange = { q -> onUpdateFilter { it.copy(query = q) } },
        placeholder = { Text(t("search_hint")) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("explore_search_input")
      )
    }

    // Category filter row
    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
      ) {
        FilterChip(
          selected = filter.category == null,
          onClick = { onUpdateFilter { it.copy(category = null) } },
          label = { Text("All Categories") }
        )
        SkillCategory.entries.forEach { cat ->
          FilterChip(
            selected = filter.category == cat,
            onClick = {
              onUpdateFilter { state ->
                state.copy(category = if (state.category == cat) null else cat)
              }
            },
            label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
          )
        }
      }
    }

    // Language + Rating + Availability filter row
    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
      ) {
        languages.forEach { lang ->
          FilterChip(
            selected = filter.language == lang,
            onClick = { onUpdateFilter { it.copy(language = lang) } },
            label = { Text(lang) }
          )
        }
        FilterChip(
          selected = filter.minRating >= 4.7,
          onClick = {
            onUpdateFilter { it.copy(minRating = if (it.minRating >= 4.7) 0.0 else 4.7) }
          },
          label = { Text("4.7★+") }
        )
        availabilityOptions.drop(1).forEach { avail ->
          FilterChip(
            selected = filter.availability == avail,
            onClick = {
              onUpdateFilter { it.copy(availability = if (it.availability == avail) "All" else avail) }
            },
            label = { Text(avail) }
          )
        }
      }
    }

    if (matches.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Default.Search,
          title = "No matching users found",
          subtitle = "Try clearing a filter or adding more skills and learning goals to your profile.",
          actionLabel = "Reset Filters",
          onAction = { onUpdateFilter { ExploreFilterState() } },
          testTag = "explore_reset_filters"
        )
      }
    } else {
      items(matches, key = { it.peerProfile.userId }) { match ->
        val peer = match.peerProfile
        Card(
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("explore_user_card_${peer.userId}")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                UserAvatarBadge(
                  name = peer.displayName,
                  photoUri = peer.photoUri,
                  isOnline = peer.isOnline,
                  size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(peer.displayName, style = MaterialTheme.typography.titleMedium)
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReadOnlyRatingBadge(peer.ratingAvg, peer.reviewCount)
                    Text(
                      text = peer.spokenLanguages.joinToString(", "),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1
                    )
                  }
                }
              }
              MatchPercentagePill(match.matchPercentage)
            }

            if (peer.bio.isNotBlank()) {
              Text(peer.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("Teaches", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            SkillChipGroup(peer.teachSkillNames)

            Text("Wants to learn", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            SkillChipGroup(
              skills = peer.learnSkillNames,
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            OutlinedButton(
              onClick = { onViewProfile(peer.userId) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("view_profile_${peer.userId}")
            ) {
              Text("View Profile & Swap")
            }
          }
        }
      }
    }
  }
}
