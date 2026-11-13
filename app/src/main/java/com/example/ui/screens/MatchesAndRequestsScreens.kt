package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExchangeRequest
import com.example.model.ExchangeStatus
import com.example.model.PublicProfile
import com.example.model.SkillMatchResult
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InteractiveStarRating
import com.example.ui.components.MatchPercentagePill
import com.example.ui.components.ReadOnlyRatingBadge
import com.example.ui.components.SkillChipGroup
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatarBadge
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError

@Composable
fun MatchesScreen(
  matches: List<SkillMatchResult>,
  onOpenMatchDetails: (String) -> Unit
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Smart Matches", style = MaterialTheme.typography.headlineMedium)
      Text(
        text = "Transparent, explainable compatibility ranked by mutual skills, languages, and schedule.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    if (matches.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Default.SwapHoriz,
          title = "No matching users found",
          subtitle = "Try adding more teaching skills or learning interests to unlock personalized matches."
        )
      }
    } else {
      items(matches, key = { it.peerProfile.userId }) { match ->
        Card(
          onClick = { onOpenMatchDetails(match.peerProfile.userId) },
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("match_card_${match.peerProfile.userId}")
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
                  name = match.peerProfile.displayName,
                  photoUri = match.peerProfile.photoUri,
                  isOnline = match.peerProfile.isOnline
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(match.peerProfile.displayName, style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = "${match.availabilityOverlap} • ${match.peerProfile.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              MatchPercentagePill(match.matchPercentage)
            }
            Text(match.reason, style = MaterialTheme.typography.bodyMedium)
            if (match.skillsYouCanLearn.isNotEmpty()) {
              Text("They can teach you:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              SkillChipGroup(match.skillsYouCanLearn)
            }
            if (match.skillsYouCanTeach.isNotEmpty()) {
              Text("You can teach them:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              SkillChipGroup(
                skills = match.skillsYouCanTeach,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
  match: SkillMatchResult,
  myProfile: PublicProfile,
  onBack: () -> Unit,
  onSendRequest: (offered: List<String>, requested: List<String>, message: String) -> Unit,
  onStartChat: () -> Unit,
  onScheduleSession: (skillName: String, dateLabel: String, startTime: String, endTime: String, description: String) -> Unit,
  onSubmitReview: (rating: Long, comment: String) -> Unit
) {
  val peer = match.peerProfile
  var showRequestDialog by remember { mutableStateOf(false) }
  var showScheduleDialog by remember { mutableStateOf(false) }
  var showReviewDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(peer.displayName) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("match_details_back")) {
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
        Card(shape = MaterialTheme.shapes.extraLarge) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatarBadge(peer.displayName, peer.photoUri, peer.isOnline, size = 64.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                  Text(peer.displayName, style = MaterialTheme.typography.headlineSmall)
                  Text(peer.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  ReadOnlyRatingBadge(peer.ratingAvg, peer.reviewCount)
                }
              }
              MatchPercentagePill(match.matchPercentage)
            }
            Text(peer.bio, style = MaterialTheme.typography.bodyMedium)
            Text(
              text = "Languages: ${peer.spokenLanguages.joinToString(", ")} • Availability: ${peer.availability}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Transparent Match Explanation Breakdown
      item {
        Card(shape = MaterialTheme.shapes.large) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Why You Match (${match.matchPercentage}%)", style = MaterialTheme.typography.titleMedium)
            Text(match.reason, style = MaterialTheme.typography.bodyMedium)
            match.scoreBreakdown.forEach { (factor, pts) ->
              val maxPts = when (factor) {
                "Skills Exchange" -> 60f
                "Languages" -> 20f
                else -> 10f
              }
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                  Text(factor, style = MaterialTheme.typography.labelMedium)
                  Text("$pts / ${maxPts.toInt()} pts", style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(
                  progress = { (pts / maxPts).coerceIn(0f, 1f) },
                  modifier = Modifier.fillMaxWidth().height(6.dp)
                )
              }
            }
          }
        }
      }

      item {
        Card(shape = MaterialTheme.shapes.large) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Skills ${peer.displayName} Can Teach", style = MaterialTheme.typography.titleSmall)
            SkillChipGroup(peer.teachSkillNames)
            Text("Skills ${peer.displayName} Wants to Learn", style = MaterialTheme.typography.titleSmall)
            SkillChipGroup(
              skills = peer.learnSkillNames,
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text("Reputation & Badges (${peer.reputationPoints} pts)", style = MaterialTheme.typography.titleSmall)
            SkillChipGroup(
              skills = peer.badges,
              containerColor = MaterialTheme.colorScheme.tertiaryContainer,
              contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
          }
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = { showRequestDialog = true },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("send_exchange_request_button")
          ) {
            Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Exchange Request")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = onStartChat,
              modifier = Modifier
                .weight(1f)
                .testTag("match_chat_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Chat")
            }
            OutlinedButton(
              onClick = { showScheduleDialog = true },
              modifier = Modifier
                .weight(1f)
                .testTag("match_schedule_button")
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Schedule")
            }
          }
          OutlinedButton(
            onClick = { showReviewDialog = true },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("match_review_button")
          ) {
            Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rate & Review ${peer.displayName}")
          }
        }
      }
    }
  }

  if (showRequestDialog) {
    var msg by remember {
      mutableStateOf("Hi ${peer.displayName}! I'd love to exchange skills with you.")
    }
    val offered = myProfile.teachSkillNames.ifEmpty { listOf("Knowledge Sharing") }
    val requested = peer.teachSkillNames.ifEmpty { listOf("Mentorship") }
    AlertDialog(
      onDismissRequest = { showRequestDialog = false },
      title = { Text("Send Exchange Request") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Offering: ${offered.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
          Text("Requesting: ${requested.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = msg,
            onValueChange = { msg = it },
            label = { Text("Personal message") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSendRequest(offered, requested, msg)
            showRequestDialog = false
          },
          modifier = Modifier.testTag("confirm_send_request")
        ) { Text("Send") }
      },
      dismissButton = {
        TextButton(onClick = { showRequestDialog = false }) { Text("Cancel") }
      }
    )
  }

  if (showScheduleDialog) {
    var skillName by remember { mutableStateOf(peer.teachSkillNames.firstOrNull() ?: "Skill Exchange") }
    var dateLabel by remember { mutableStateOf("2026-11-18") }
    var startTime by remember { mutableStateOf("18:30") }
    var endTime by remember { mutableStateOf("19:30") }
    var notes by remember { mutableStateOf("Hands-on 1:1 skill exchange session") }
    AlertDialog(
      onDismissRequest = { showScheduleDialog = false },
      title = { Text("Schedule Session") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = skillName, onValueChange = { skillName = it }, label = { Text("Skill") })
          OutlinedTextField(value = dateLabel, onValueChange = { dateLabel = it }, label = { Text("Date") })
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End") }, modifier = Modifier.weight(1f))
          }
          OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Agenda") })
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onScheduleSession(skillName, dateLabel, startTime, endTime, notes)
            showScheduleDialog = false
          },
          modifier = Modifier.testTag("confirm_schedule_session")
        ) { Text("Schedule") }
      },
      dismissButton = {
        TextButton(onClick = { showScheduleDialog = false }) { Text("Cancel") }
      }
    )
  }

  if (showReviewDialog) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showReviewDialog = false },
      title = { Text("Review ${peer.displayName}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          InteractiveStarRating(rating = rating, onRatingSelected = { rating = it })
          OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Share your feedback") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          enabled = comment.isNotBlank(),
          onClick = {
            onSubmitReview(rating.toLong(), comment)
            showReviewDialog = false
          },
          modifier = Modifier.testTag("confirm_submit_review")
        ) { Text("Submit") }
      },
      dismissButton = {
        TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
fun RequestsScreen(
  currentUserId: String,
  requests: List<ExchangeRequest>,
  onUpdateStatus: (String, ExchangeStatus) -> Unit,
  onOpenExplore: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Received", "Sent", "Completed")

  val filtered = remember(requests, selectedTab, currentUserId) {
    when (selectedTab) {
      0 -> requests.filter { it.receiverId == currentUserId && it.status != ExchangeStatus.COMPLETED.name }
      1 -> requests.filter { it.senderId == currentUserId && it.status != ExchangeStatus.COMPLETED.name }
      else -> requests.filter { it.status == ExchangeStatus.COMPLETED.name }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text("Exchange Requests", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(12.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      tabs.forEachIndexed { index, label ->
        SegmentedButton(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
          modifier = Modifier.testTag("requests_tab_$index")
        ) {
          Text(label)
        }
      }
    }
    Spacer(modifier = Modifier.height(14.dp))

    if (filtered.isEmpty()) {
      EmptyStateCard(
        icon = Icons.Default.Handshake,
        title = "No exchange requests yet",
        subtitle = "Discover compatible peers in Explore or Matches and send a skill swap request.",
        actionLabel = "Explore Peers",
        onAction = onOpenExplore,
        testTag = "requests_empty_explore"
      )
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filtered, key = { it.id }) { req ->
          val isReceiver = req.receiverId == currentUserId
          Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = if (isReceiver) "From ${req.senderName}" else "To ${req.receiverName}",
                  style = MaterialTheme.typography.titleMedium
                )
                StatusPill(
                  label = req.status,
                  containerColor = when (req.status) {
                    ExchangeStatus.ACCEPTED.name, ExchangeStatus.COMPLETED.name -> EmeraldSuccess.copy(alpha = 0.16f)
                    ExchangeStatus.REJECTED.name, ExchangeStatus.CANCELLED.name -> RoseError.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.primaryContainer
                  },
                  contentColor = when (req.status) {
                    ExchangeStatus.ACCEPTED.name, ExchangeStatus.COMPLETED.name -> EmeraldSuccess
                    ExchangeStatus.REJECTED.name, ExchangeStatus.CANCELLED.name -> RoseError
                    else -> MaterialTheme.colorScheme.primary
                  }
                )
              }
              Text("Offered: ${req.offeredSkills.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
              Text("Requested: ${req.requestedSkills.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
              if (req.message.isNotBlank()) {
                Text(req.message, style = MaterialTheme.typography.bodyMedium)
              }

              if (req.status == ExchangeStatus.PENDING.name) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (isReceiver) {
                    Button(onClick = { onUpdateStatus(req.id, ExchangeStatus.ACCEPTED) }, modifier = Modifier.weight(1f)) {
                      Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Accept")
                    }
                    OutlinedButton(onClick = { onUpdateStatus(req.id, ExchangeStatus.REJECTED) }, modifier = Modifier.weight(1f)) {
                      Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Reject")
                    }
                  } else {
                    OutlinedButton(onClick = { onUpdateStatus(req.id, ExchangeStatus.CANCELLED) }, modifier = Modifier.fillMaxWidth()) {
                      Text("Cancel Request")
                    }
                  }
                }
              } else if (req.status == ExchangeStatus.ACCEPTED.name) {
                Button(
                  onClick = { onUpdateStatus(req.id, ExchangeStatus.COMPLETED) },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text("Mark Exchange Completed (+10 Reputation)")
                }
              }
            }
          }
        }
      }
    }
  }
}
