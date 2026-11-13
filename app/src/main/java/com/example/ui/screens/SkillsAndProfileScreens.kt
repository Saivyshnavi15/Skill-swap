package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SeedCommunityCatalog
import com.example.model.GoalPriority
import com.example.model.LearningGoal
import com.example.model.Proficiency
import com.example.model.PublicProfile
import com.example.model.SkillCategory
import com.example.model.UserSkill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ReadOnlyRatingBadge
import com.example.ui.components.SkillChipGroup
import com.example.ui.components.UserAvatarBadge

@Composable
fun ProfileScreen(
  profile: PublicProfile,
  mySkills: List<UserSkill>,
  myGoals: List<LearningGoal>,
  isAdminUser: Boolean,
  onSaveProfile: (
    displayName: String,
    bio: String,
    location: String,
    photoUri: String,
    spokenLanguages: List<String>,
    preferredLanguage: String,
    availability: String,
    experienceYears: Long
  ) -> Unit,
  onOpenMySkills: () -> Unit,
  onOpenLearningGoals: () -> Unit,
  onOpenSessions: () -> Unit,
  onOpenProjects: () -> Unit,
  onOpenReputation: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenAdmin: () -> Unit
) {
  var showEditDialog by remember { mutableStateOf(false) }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      onSaveProfile(
        profile.displayName,
        profile.bio,
        profile.location,
        uri.toString(),
        profile.spokenLanguages,
        profile.preferredLanguage,
        profile.availability,
        profile.experienceYears
      )
    }
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Card(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          UserAvatarBadge(
            name = profile.displayName,
            photoUri = profile.photoUri,
            isOnline = true,
            size = 84.dp
          )
          TextButton(
            onClick = {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.testTag("pick_profile_photo")
          ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Update Photo")
          }
          Text(profile.displayName, style = MaterialTheme.typography.headlineSmall)
          Text(
            text = "${profile.location} • ${profile.experienceYears} yrs exp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          ReadOnlyRatingBadge(profile.ratingAvg, profile.reviewCount)
          Text(
            text = profile.bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Languages: ${profile.spokenLanguages.joinToString(", ")} | Availability: ${profile.availability}",
            style = MaterialTheme.typography.bodySmall
          )
          Button(
            onClick = { showEditDialog = true },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("edit_profile_button")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile & Availability")
          }
        }
      }
    }

    item {
      Card(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Teaching Skills (${mySkills.size})", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenMySkills, modifier = Modifier.testTag("profile_my_skills_button")) {
              Text("Manage")
            }
          }
          SkillChipGroup(profile.teachSkillNames)

          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Learning Goals (${myGoals.size})", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenLearningGoals, modifier = Modifier.testTag("profile_learning_goals_button")) {
              Text("Manage")
            }
          }
          SkillChipGroup(
            skills = profile.learnSkillNames,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileNavRow(Icons.Default.CalendarMonth, "Learning & Video Sessions", onOpenSessions, "profile_sessions_btn")
        ProfileNavRow(Icons.Default.FolderShared, "Collaboration Projects", onOpenProjects, "profile_projects_btn")
        ProfileNavRow(Icons.Default.EmojiEvents, "Reputation, Badges & Reviews", onOpenReputation, "profile_reputation_btn")
        ProfileNavRow(Icons.Default.Language, "Settings, Languages & Privacy", onOpenSettings, "profile_settings_btn")
        if (isAdminUser) {
          ProfileNavRow(Icons.Default.AdminPanelSettings, "Admin Moderation Console", onOpenAdmin, "profile_admin_btn")
        }
      }
    }
  }

  if (showEditDialog) {
    var name by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var location by remember { mutableStateOf(profile.location) }
    var availability by remember { mutableStateOf(profile.availability) }
    var langs by remember { mutableStateOf(profile.spokenLanguages.joinToString(", ")) }
    var expYears by remember { mutableStateOf(profile.experienceYears.toString()) }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text("Edit Profile") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
          OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })
          OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
          OutlinedTextField(value = availability, onValueChange = { availability = it }, label = { Text("Availability") })
          OutlinedTextField(value = langs, onValueChange = { langs = it }, label = { Text("Spoken Languages (comma-separated)") })
          OutlinedTextField(value = expYears, onValueChange = { expYears = it }, label = { Text("Experience (years)") })
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val parsedLangs = langs.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("English") }
            onSaveProfile(
              name,
              bio,
              location,
              profile.photoUri,
              parsedLangs,
              parsedLangs.first(),
              availability,
              expYears.toLongOrNull() ?: 1L
            )
            showEditDialog = false
          },
          modifier = Modifier.testTag("save_profile_dialog_btn")
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun ProfileNavRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  tag: String
) {
  OutlinedButton(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .testTag(tag)
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(10.dp))
    Text(label, modifier = Modifier.weight(1f))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySkillsScreen(
  skills: List<UserSkill>,
  onBack: () -> Unit,
  onAddSkill: (name: String, category: SkillCategory, proficiency: String, exp: Long, desc: String, isCustom: Boolean) -> Unit,
  onUpdateSkill: (id: String, name: String, category: SkillCategory, proficiency: String, exp: Long, desc: String) -> Unit,
  onDeleteSkill: (String) -> Unit
) {
  var showAddEditDialog by remember { mutableStateOf(false) }
  var editingSkill by remember { mutableStateOf<UserSkill?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("My Teaching Skills") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("my_skills_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { editingSkill = null; showAddEditDialog = true },
        modifier = Modifier.testTag("fab_add_skill")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Skill")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (skills.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Default.School,
            title = "You haven't added any skills yet",
            subtitle = "Add skills you can teach (Java, Photoshop, Guitar, Cooking, or any custom skill).",
            actionLabel = "Add Your First Skill",
            onAction = { editingSkill = null; showAddEditDialog = true },
            testTag = "empty_add_first_skill"
          )
        }
      } else {
        items(skills, key = { it.id }) { skill ->
          Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(skill.name, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${skill.category} • ${skill.proficiency} • ${skill.experienceYears} yrs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                  )
                }
                Row {
                  IconButton(onClick = { editingSkill = skill; showAddEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit skill")
                  }
                  IconButton(onClick = { onDeleteSkill(skill.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete skill", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
              if (skill.description.isNotBlank()) {
                Text(skill.description, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }
  }

  if (showAddEditDialog) {
    var name by remember(editingSkill) { mutableStateOf(editingSkill?.name ?: "") }
    var category by remember(editingSkill) {
      mutableStateOf(
        SkillCategory.entries.firstOrNull { it.name == editingSkill?.category } ?: SkillCategory.TECHNICAL
      )
    }
    var proficiency by remember(editingSkill) {
      mutableStateOf(editingSkill?.proficiency ?: Proficiency.INTERMEDIATE.name)
    }
    var exp by remember(editingSkill) { mutableStateOf((editingSkill?.experienceYears ?: 2L).toString()) }
    var desc by remember(editingSkill) { mutableStateOf(editingSkill?.description ?: "") }

    AlertDialog(
      onDismissRequest = { showAddEditDialog = false },
      title = { Text(if (editingSkill == null) "Add Teaching Skill" else "Edit Skill") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          // Quick suggestion chips from catalog
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            SeedCommunityCatalog.catalogSkills.take(8).forEach { tpl ->
              FilterChip(
                selected = name.equals(tpl.name, ignoreCase = true),
                onClick = { name = tpl.name; category = tpl.category },
                label = { Text(tpl.name) }
              )
            }
          }
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Skill Name (or custom skill)") },
            modifier = Modifier.testTag("skill_name_input")
          )
          Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            SkillCategory.entries.forEach { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat.name) }
              )
            }
          }
          Text("Proficiency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            Proficiency.entries.forEach { prof ->
              FilterChip(
                selected = proficiency == prof.name,
                onClick = { proficiency = prof.name },
                label = { Text(prof.displayLabel) }
              )
            }
          }
          OutlinedTextField(value = exp, onValueChange = { exp = it }, label = { Text("Experience (years)") })
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("What you can teach") })
        }
      },
      confirmButton = {
        Button(
          enabled = name.isNotBlank(),
          onClick = {
            val isCustom = SeedCommunityCatalog.catalogSkills.none { it.name.equals(name.trim(), ignoreCase = true) }
            val years = exp.toLongOrNull() ?: 1L
            if (editingSkill == null) {
              onAddSkill(name, category, proficiency, years, desc, isCustom)
            } else {
              onUpdateSkill(editingSkill!!.id, name, category, proficiency, years, desc)
            }
            showAddEditDialog = false
          },
          modifier = Modifier.testTag("confirm_save_skill")
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showAddEditDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningGoalsScreen(
  goals: List<LearningGoal>,
  onBack: () -> Unit,
  onAddGoal: (skillName: String, current: String, target: String, priority: String) -> Unit,
  onDeleteGoal: (String) -> Unit
) {
  var showAddDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Learning Goals") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("learning_goals_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        modifier = Modifier.testTag("fab_add_goal")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Learning Goal")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (goals.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Default.School,
            title = "No learning goals yet",
            subtitle = "Add skills you want to learn (e.g., Photoshop, Video Editing) to get matched with mentors.",
            actionLabel = "Add Learning Goal",
            onAction = { showAddDialog = true },
            testTag = "empty_add_first_goal"
          )
        }
      } else {
        items(goals, key = { it.id }) { goal ->
          Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(goal.skillName, style = MaterialTheme.typography.titleMedium)
                Text(
                  "Current: ${goal.currentLevel} → Target: ${goal.targetLevel}",
                  style = MaterialTheme.typography.bodySmall
                )
                Text(
                  "Priority: ${goal.priority}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
              IconButton(onClick = { onDeleteGoal(goal.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete learning goal", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    var skillName by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(Proficiency.BEGINNER.name) }
    var target by remember { mutableStateOf(Proficiency.ADVANCED.name) }
    var priority by remember { mutableStateOf(GoalPriority.HIGH.name) }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add Learning Goal") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            listOf("Photoshop", "Video Editing", "UI/UX Design", "Kotlin", "Guitar", "Public Speaking").forEach { s ->
              FilterChip(selected = skillName == s, onClick = { skillName = s }, label = { Text(s) })
            }
          }
          OutlinedTextField(
            value = skillName,
            onValueChange = { skillName = it },
            label = { Text("Skill you want to learn") },
            modifier = Modifier.testTag("goal_skill_input")
          )
          Text("Current Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Proficiency.entries.forEach { p ->
              FilterChip(selected = current == p.name, onClick = { current = p.name }, label = { Text(p.displayLabel) })
            }
          }
          Text("Target Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Proficiency.entries.forEach { p ->
              FilterChip(selected = target == p.name, onClick = { target = p.name }, label = { Text(p.displayLabel) })
            }
          }
          Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GoalPriority.entries.forEach { gp ->
              FilterChip(selected = priority == gp.name, onClick = { priority = gp.name }, label = { Text(gp.displayLabel) })
            }
          }
        }
      },
      confirmButton = {
        Button(
          enabled = skillName.isNotBlank(),
          onClick = {
            onAddGoal(skillName, current, target, priority)
            showAddDialog = false
          },
          modifier = Modifier.testTag("confirm_save_goal")
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
      }
    )
  }
}
