package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.AppNotification
import com.example.model.BadgeCatalog
import com.example.model.CollabProject
import com.example.model.ProjectStatus
import com.example.model.PublicProfile
import com.example.model.UserPrivate
import com.example.model.UserReview
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReadOnlyRatingBadge
import com.example.ui.components.SkillChipGroup
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberStar
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
  currentUserId: String,
  projects: List<CollabProject>,
  onBack: () -> Unit,
  onCreateProject: (title: String, desc: String, skills: List<String>, deadline: String) -> Unit,
  onJoinProject: (CollabProject) -> Unit,
  onLeaveProject: (CollabProject) -> Unit,
  onCompleteProject: (String) -> Unit
) {
  var showCreateDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Project Collaboration") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("projects_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateDialog = true },
        modifier = Modifier.testTag("fab_create_project")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Create Project")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      item {
        Card(shape = MaterialTheme.shapes.extraLarge) {
          Image(
            painter = painterResource(id = R.drawable.img_community_collab),
            contentDescription = "Collaborative community studio",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
          )
        }
      }

      if (projects.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Default.FolderShared,
            title = "No community projects yet",
            subtitle = "Create a project to collaborate across coding, design, and content skills.",
            actionLabel = "Create Project",
            onAction = { showCreateDialog = true }
          )
        }
      } else {
        items(projects, key = { it.id }) { proj ->
          val isMember = currentUserId in proj.memberIds
          val isOwner = proj.ownerId == currentUserId
          Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(proj.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusPill(
                  label = proj.status,
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
              Text("Owner: ${proj.ownerName} • Deadline: ${proj.deadline.ifBlank { "Flexible" }}", style = MaterialTheme.typography.bodySmall)
              Text(proj.description, style = MaterialTheme.typography.bodyMedium)
              Text("Required Skills", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              SkillChipGroup(proj.requiredSkills)
              Text(
                text = "Members (${proj.memberNames.size}): ${proj.memberNames.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              if (proj.status != ProjectStatus.COMPLETED.name) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (!isMember) {
                    Button(
                      onClick = { onJoinProject(proj) },
                      modifier = Modifier.weight(1f).testTag("join_project_${proj.id}")
                    ) { Text("Join Project") }
                  } else if (!isOwner) {
                    OutlinedButton(
                      onClick = { onLeaveProject(proj) },
                      modifier = Modifier.weight(1f)
                    ) { Text("Leave Project") }
                  }
                  if (isOwner) {
                    Button(
                      onClick = { onCompleteProject(proj.id) },
                      modifier = Modifier.weight(1f)
                    ) { Text("Complete (+10 pts)") }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("Java, UI/UX Design") }
    var deadline by remember { mutableStateOf("2026-12-25") }

    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("Create Collaborative Project") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title") }, modifier = Modifier.testTag("project_title_input"))
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
          OutlinedTextField(value = skills, onValueChange = { skills = it }, label = { Text("Required Skills (comma-separated)") })
          OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline") })
        }
      },
      confirmButton = {
        Button(
          enabled = title.trim().length >= 2 && desc.isNotBlank(),
          onClick = {
            val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            onCreateProject(title, desc, skillList, deadline)
            showCreateDialog = false
          },
          modifier = Modifier.testTag("confirm_create_project")
        ) { Text("Create") }
      },
      dismissButton = {
        TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReputationBadgesReviewsScreen(
  profile: PublicProfile,
  receivedReviews: List<UserReview>,
  authoredReviews: List<UserReview>,
  onBack: () -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Reputation, Badges & Reviews") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("reputation_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = MaterialTheme.shapes.extraLarge,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Community Reputation", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "${profile.reputationPoints} Points",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Complete exchange +10 • Help user +5 • 5-star review +5 • Complete project +10 • Cancellation -5",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }

      item {
        Text("Badges", style = MaterialTheme.typography.titleLarge)
      }

      items(BadgeCatalog.allBadges, key = { it.id }) { badge ->
        val unlocked = profile.reputationPoints >= badge.requiredPoints || badge.title in profile.badges
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
          ) {
            Icon(
              imageVector = if (unlocked) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
              contentDescription = null,
              tint = if (unlocked) AmberStar else MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(badge.title, style = MaterialTheme.typography.titleSmall)
              Text(badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(
              label = if (unlocked) "Unlocked" else "${badge.requiredPoints} pts",
              containerColor = if (unlocked) EmeraldSuccess.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (unlocked) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text("Reviews (${receivedReviews.size + authoredReviews.size})", style = MaterialTheme.typography.titleLarge)
      }

      val allReviews = (receivedReviews + authoredReviews).distinctBy { it.id }
      if (allReviews.isEmpty()) {
        item {
          Text(
            "No reviews yet. Complete a learning session or open any match profile to share feedback!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        items(allReviews, key = { it.id }) { rev ->
          Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${rev.reviewerName} → ${rev.reviewedUserName}", style = MaterialTheme.typography.titleSmall)
                ReadOnlyRatingBadge(rev.rating.toDouble())
              }
              Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
  notifications: List<AppNotification>,
  onBack: () -> Unit,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: () -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Notifications") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (notifications.any { !it.isRead }) {
            IconButton(onClick = onMarkAllRead, modifier = Modifier.testTag("mark_all_notifications_read")) {
              Icon(Icons.Default.DoneAll, contentDescription = "Mark all read")
            }
          }
        }
      )
    }
  ) { innerPadding ->
    if (notifications.isEmpty()) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        EmptyStateCard(
          icon = Icons.Default.Notifications,
          title = "All caught up!",
          subtitle = "Exchange requests, session reminders, project invites, and earned badges will appear here."
        )
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(innerPadding)
      ) {
        items(notifications, key = { it.id }) { n ->
          Card(
            onClick = { if (!n.isRead) onMarkRead(n.id) },
            colors = CardDefaults.cardColors(
              containerColor = if (n.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(n.title, style = MaterialTheme.typography.titleSmall)
                if (!n.isRead) {
                  StatusPill(
                    label = "NEW",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  )
                }
              }
              Text(n.body, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLanguageAdminScreen(
  appLanguage: AppLanguage,
  privateUser: UserPrivate?,
  isAdminUser: Boolean,
  communityProfiles: List<PublicProfile>,
  onBack: () -> Unit,
  onSavePreferences: (appLang: AppLanguage, commLang: String, showLocation: Boolean, allowInvites: Boolean) -> Unit,
  onAdminToggleActive: (String, Boolean) -> Unit,
  onSignOut: () -> Unit
) {
  var selectedAppLang by remember(appLanguage) { mutableStateOf(appLanguage) }
  var selectedCommLang by remember(privateUser) { mutableStateOf(privateUser?.preferredCommLanguage ?: "English") }
  var showLocation by remember(privateUser) { mutableStateOf(privateUser?.privacyShowLocation ?: true) }
  var allowInvites by remember(privateUser) { mutableStateOf(privateUser?.privacyAllowInvites ?: true) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings & Multilingual") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      item {
        Card(shape = MaterialTheme.shapes.large) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Application Language", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.chunked(3).forEach { rowLangs ->
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowLangs.forEach { lang ->
                      FilterChip(
                        selected = selectedAppLang == lang,
                        onClick = { selectedAppLang = lang },
                        label = { Text("${lang.nativeName} (${lang.englishName})") },
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                      )
                    }
                  }
                }
              }
            }
            Text("Preferred Communication Language", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("English", "Telugu", "Hindi").forEach { cLang ->
                FilterChip(
                  selected = selectedCommLang == cLang,
                  onClick = { selectedCommLang = cLang },
                  label = { Text(cLang) }
                )
              }
            }
          }
        }
      }

      item {
        Card(shape = MaterialTheme.shapes.large) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Privacy & Discovery", style = MaterialTheme.typography.titleMedium)
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Show city/region on public profile", style = MaterialTheme.typography.bodyMedium)
              Switch(checked = showLocation, onCheckedChange = { showLocation = it })
            }
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Allow project collaboration invites", style = MaterialTheme.typography.bodyMedium)
              Switch(checked = allowInvites, onCheckedChange = { allowInvites = it })
            }
            Button(
              onClick = { onSavePreferences(selectedAppLang, selectedCommLang, showLocation, allowInvites) },
              modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
            ) {
              Text("Save Language & Privacy Settings")
            }
          }
        }
      }

      item {
        Card(shape = MaterialTheme.shapes.large) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Matching & Future AI Architecture", style = MaterialTheme.typography.titleSmall)
              Text(
                "Smart matches currently use transparent deterministic scoring. Modular interfaces (AIRecommendationService, SkillGapService, AITranslationService, AIProfileService) are ready for future AI integration.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      if (isAdminUser) {
        item {
          Text("Admin Moderation Console (${communityProfiles.size} profiles)", style = MaterialTheme.typography.titleLarge)
        }
        items(communityProfiles, key = { it.userId }) { peer ->
          Card(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(peer.displayName, style = MaterialTheme.typography.titleSmall)
                Text("${peer.reputationPoints} pts • ${peer.teachSkillNames.take(2).joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
              }
              if (!peer.userId.startsWith("seed_")) {
                Switch(
                  checked = peer.isActive,
                  onCheckedChange = { active -> onAdminToggleActive(peer.userId, active) }
                )
              } else {
                StatusPill("Community Seed", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
              }
            }
          }
        }
      }

      item {
        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sign_out_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out")
        }
      }
    }
  }
}
