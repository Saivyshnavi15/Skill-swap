package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.LocalCameraWebRtcSessionService
import com.example.domain.VideoSessionService
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.LearningSession
import com.example.model.SessionStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatarBadge
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
  currentUserId: String,
  conversations: List<Conversation>,
  onBack: () -> Unit,
  onOpenChat: (String) -> Unit,
  onOpenExplore: () -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Conversations") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("chat_list_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    if (conversations.isEmpty()) {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        EmptyStateCard(
          icon = Icons.AutoMirrored.Filled.Chat,
          title = "No conversations yet",
          subtitle = "Send an exchange request or open Chat on any mentor profile to start messaging.",
          actionLabel = "Browse Mentors",
          onAction = onOpenExplore,
          testTag = "chat_empty_explore"
        )
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(innerPadding)
      ) {
        items(conversations, key = { it.id }) { conv ->
          val peerIndex = conv.participantIds.indexOfFirst { it != currentUserId }
          val peerName = conv.participantNames.getOrElse(if (peerIndex >= 0) peerIndex else 0) { "Peer" }
          Card(
            onClick = { onOpenChat(conv.id) },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("conversation_card_${conv.id}")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(14.dp).fillMaxWidth()
            ) {
              UserAvatarBadge(name = peerName, isOnline = true, size = 46.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(peerName, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = conv.lastMessage,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
              if (conv.unreadCount > 0 && conv.lastSenderId != currentUserId) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                  Text(
                    text = conv.unreadCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  currentUserId: String,
  conversation: Conversation,
  messages: List<ChatMessage>,
  onBack: () -> Unit,
  onSendMessage: (String) -> Unit
) {
  val peerIndex = conversation.participantIds.indexOfFirst { it != currentUserId }
  val peerName = conversation.participantNames.getOrElse(if (peerIndex >= 0) peerIndex else 0) { "Peer" }
  var draft by remember { mutableStateOf("") }
  val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatarBadge(name = peerName, isOnline = true, size = 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(peerName, style = MaterialTheme.typography.titleMedium)
              Text("Online • Real-time sync", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    bottomBar = {
      Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text("Type a message...") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("chat_message_input")
          )
          Spacer(modifier = Modifier.width(8.dp))
          FilledIconButton(
            enabled = draft.isNotBlank(),
            onClick = {
              onSendMessage(draft)
              draft = ""
            },
            modifier = Modifier.size(48.dp).testTag("chat_send_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderId == currentUserId
        Row(
          horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
          modifier = Modifier.fillMaxWidth()
        ) {
          Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
              topStart = 18.dp,
              topEnd = 18.dp,
              bottomStart = if (isMine) 18.dp else 4.dp,
              bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
              Text(
                text = msg.text,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
              )
              Spacer(modifier = Modifier.height(3.dp))
              val timeStr = msg.createdAt?.toDate()?.let { timeFormat.format(it) } ?: "Sending"
              Text(
                text = if (isMine && msg.isRead) "$timeStr • Read" else timeStr,
                color = (if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall
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
fun SessionsScreen(
  currentUserId: String,
  sessions: List<LearningSession>,
  onBack: () -> Unit,
  onJoinVideo: (LearningSession) -> Unit,
  onUpdateStatus: (String, SessionStatus) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Upcoming", "Past")

  val filtered = remember(sessions, selectedTab) {
    if (selectedTab == 0) {
      sessions.filter { it.status == SessionStatus.SCHEDULED.name || it.status == SessionStatus.STARTED.name }
    } else {
      sessions.filter { it.status == SessionStatus.COMPLETED.name || it.status == SessionStatus.CANCELLED.name }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Learning Sessions") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("sessions_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp)
    ) {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, label ->
          SegmentedButton(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
          ) { Text(label) }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))

      if (filtered.isEmpty()) {
        EmptyStateCard(
          icon = Icons.Default.CalendarMonth,
          title = if (selectedTab == 0) "No upcoming sessions" else "No past sessions yet",
          subtitle = "Open any Match Detail page and tap Schedule to book a live skill exchange session."
        )
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          items(filtered, key = { it.id }) { sess ->
            val peerName = if (sess.hostId == currentUserId) sess.guestName else sess.hostName
            Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(sess.skillName, style = MaterialTheme.typography.titleMedium)
                  StatusPill(
                    label = sess.status,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                }
                Text("With: $peerName", style = MaterialTheme.typography.bodyMedium)
                Text("${sess.dateLabel} • ${sess.startTime} – ${sess.endTime}", style = MaterialTheme.typography.bodySmall)
                if (sess.description.isNotBlank()) {
                  Text(sess.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (sess.status == SessionStatus.SCHEDULED.name || sess.status == SessionStatus.STARTED.name) {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                      onClick = { onJoinVideo(sess) },
                      modifier = Modifier.weight(1f).testTag("join_video_${sess.id}")
                    ) {
                      Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Join Video")
                    }
                    OutlinedButton(
                      onClick = { onUpdateStatus(sess.id, SessionStatus.COMPLETED) },
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Complete")
                    }
                  }
                  OutlinedButton(
                    onClick = { onUpdateStatus(sess.id, SessionStatus.CANCELLED) },
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Cancel Session")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(
  session: LearningSession,
  currentUserId: String,
  onLeaveSession: () -> Unit,
  videoService: VideoSessionService = remember { LocalCameraWebRtcSessionService() }
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val callState by videoService.state.collectAsStateWithLifecycle()
  val peerName = if (session.hostId == currentUserId) session.guestName else session.hostName

  var hasPermissions by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    hasPermissions = grants[Manifest.permission.CAMERA] == true &&
      grants[Manifest.permission.RECORD_AUDIO] == true
  }

  val previewView = remember { PreviewView(context) }

  LaunchedEffect(session.id) {
    videoService.joinSession(session.id, session.roomCode, peerName)
    if (!hasPermissions) {
      permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }
  }

  LaunchedEffect(hasPermissions, callState.isCameraEnabled, callState.isFrontCamera) {
    if (hasPermissions && callState.isCameraEnabled) {
      videoService.bindLocalCamera(context, lifecycleOwner, previewView)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("${session.skillName} • Room ${callState.roomCode}", style = MaterialTheme.typography.titleMedium)
            Text("Peer: $peerName", style = MaterialTheme.typography.labelSmall)
          }
        },
        navigationIcon = {
          IconButton(onClick = {
            videoService.leaveSession()
            onLeaveSession()
          }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = callState.signalingState,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(12.dp)
        )
      }

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(MaterialTheme.shapes.extraLarge)
          .background(Color(0xFF0F172A))
      ) {
        if (hasPermissions && callState.isCameraEnabled) {
          AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UserAvatarBadge(name = peerName, isOnline = true, size = 72.dp)
            Text(
              text = if (!hasPermissions) "Camera & microphone permission required for live video" else "Camera is turned off",
              color = Color.White,
              style = MaterialTheme.typography.bodyMedium
            )
            if (!hasPermissions) {
              Button(
                onClick = {
                  permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                },
                modifier = Modifier.testTag("grant_camera_audio_button")
              ) {
                Text("Allow Camera & Mic")
              }
            }
          }
        }

        Surface(
          color = Color.Black.copy(alpha = 0.55f),
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(14.dp)
        ) {
          Text(
            text = "Session ID: ${session.id.take(10)}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        FilledIconButton(
          onClick = { videoService.toggleMicrophone() },
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (callState.isMicEnabled) MaterialTheme.colorScheme.surfaceVariant else RoseError
          ),
          modifier = Modifier.size(56.dp).testTag("video_toggle_mic")
        ) {
          Icon(
            imageVector = if (callState.isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (callState.isMicEnabled) "Mute mic" else "Unmute mic"
          )
        }

        FilledIconButton(
          onClick = { videoService.toggleCamera() },
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (callState.isCameraEnabled) MaterialTheme.colorScheme.surfaceVariant else RoseError
          ),
          modifier = Modifier.size(56.dp).testTag("video_toggle_camera")
        ) {
          Icon(
            imageVector = if (callState.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
            contentDescription = if (callState.isCameraEnabled) "Turn off camera" else "Turn on camera"
          )
        }

        FilledIconButton(
          onClick = {
            if (hasPermissions) {
              videoService.flipCamera(context, lifecycleOwner, previewView)
            }
          },
          modifier = Modifier.size(56.dp).testTag("video_flip_camera")
        ) {
          Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera")
        }

        Button(
          onClick = {
            videoService.leaveSession()
            onLeaveSession()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RoseError),
          modifier = Modifier.height(56.dp).testTag("video_leave_button")
        ) {
          Icon(Icons.Default.CallEnd, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Leave", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
