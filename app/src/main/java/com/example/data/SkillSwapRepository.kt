package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppNotification
import com.example.model.BadgeCatalog
import com.example.model.ChatMessage
import com.example.model.CollabProject
import com.example.model.Conversation
import com.example.model.ExchangeRequest
import com.example.model.ExchangeStatus
import com.example.model.LearningGoal
import com.example.model.LearningSession
import com.example.model.NotificationType
import com.example.model.ProjectStatus
import com.example.model.PublicProfile
import com.example.model.SessionStatus
import com.example.model.UserPrivate
import com.example.model.UserReview
import com.example.model.UserSkill
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SkillSwapRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun <T> wrapFlowError(
    upstream: Flow<T>,
    operationType: OperationType,
    path: String,
    uid: String
  ): Flow<T> = upstream.catch { error ->
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
    throw error
  }

  // --- USER PRIVATE & PUBLIC PROFILE ---

  suspend fun ensureUserInitialized(
    displayName: String,
    email: String?
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userPath = "users/$uid"
    val userRef = db.collection("users").document(uid)
    val userSnap = try {
      userRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, userPath)
      throw e
    }
    if (!userSnap.exists()) {
      val privatePayload = mapOf(
        "userId" to uid,
        "email" to email,
        "appLanguage" to "en",
        "preferredCommLanguage" to "English",
        "privacyShowLocation" to true,
        "privacyAllowInvites" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      ).filterValues { it != null }
      try {
        userRef.set(privatePayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, userPath)
        throw e
      }
    }

    val profilePath = "profiles/$uid"
    val profileRef = db.collection("profiles").document(uid)
    val profileSnap = try {
      profileRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, profilePath)
      throw e
    }
    if (!profileSnap.exists()) {
      val cleanName = displayName.ifBlank { "SkillSwap Learner" }.take(80)
      val profilePayload = mapOf(
        "userId" to uid,
        "displayName" to cleanName,
        "bio" to "Curious peer ready to teach & learn new skills on SkillSwap.",
        "location" to "India",
        "spokenLanguages" to listOf("English"),
        "preferredLanguage" to "English",
        "availability" to "Weekdays 6-9 PM",
        "experienceYears" to 1L,
        "teachSkillNames" to emptyList<String>(),
        "learnSkillNames" to emptyList<String>(),
        "ratingAvg" to 5.0,
        "reviewCount" to 0L,
        "reputationPoints" to 10L,
        "badges" to listOf("Beginner Helper"),
        "isActive" to true,
        "isOnline" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        profileRef.set(profilePayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, profilePath)
        throw e
      }
    }
  }

  fun observeMyPrivateUser(): Flow<UserPrivate?> = flow {
    val uid = requireUserId()
    val path = "users/$uid"
    emitAll(
      wrapFlowError(
        db.collection("users").document(uid).snapshots()
          .map { snap -> snap.toObject(UserPrivate::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.GET,
        path,
        uid
      )
    )
  }

  fun observeMyProfile(): Flow<PublicProfile?> = flow {
    val uid = requireUserId()
    val path = "profiles/$uid"
    emitAll(
      wrapFlowError(
        db.collection("profiles").document(uid).snapshots()
          .map { snap -> snap.toObject(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.GET,
        path,
        uid
      )
    )
  }

  fun observeCommunityProfiles(): Flow<List<PublicProfile>> = flow {
    val uid = requireUserId()
    val path = "profiles"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereEqualTo("isActive", true)
          .snapshots()
          .map { snapshot ->
            snapshot.toObjects(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun updateMyProfile(
    displayName: String,
    bio: String,
    location: String,
    photoUri: String,
    spokenLanguages: List<String>,
    preferredLanguage: String,
    availability: String,
    experienceYears: Long
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("profiles").document(uid)
    val updates = mapOf(
      "displayName" to displayName.trim().take(80).ifBlank { "Learner" },
      "bio" to bio.trim().take(500),
      "location" to location.trim().take(100),
      "photoUri" to photoUri.trim().take(500),
      "spokenLanguages" to spokenLanguages.take(10),
      "preferredLanguage" to preferredLanguage.take(30),
      "availability" to availability.trim().take(120),
      "experienceYears" to experienceYears.coerceIn(0L, 80L),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      ref.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  suspend fun updateUserSettings(
    appLanguage: String,
    preferredCommLanguage: String,
    privacyShowLocation: Boolean,
    privacyAllowInvites: Boolean
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("users").document(uid)
    val updates = mapOf(
      "appLanguage" to appLanguage,
      "preferredCommLanguage" to preferredCommLanguage.take(30),
      "privacyShowLocation" to privacyShowLocation,
      "privacyAllowInvites" to privacyAllowInvites,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      ref.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  // --- SKILLS & LEARNING GOALS ---

  fun observeMySkills(): Flow<List<UserSkill>> = flow {
    val uid = requireUserId()
    val path = "user_skills"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereEqualTo("userId", uid)
          .snapshots()
          .map { snap -> snap.toObjects(UserSkill::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun addSkill(
    name: String,
    category: String,
    proficiency: String,
    experienceYears: Long,
    description: String,
    isCustom: Boolean
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("user_skills").document()
    val cleanName = name.trim().take(60)
    val payload = mapOf(
      "userId" to uid,
      "name" to cleanName,
      "category" to category,
      "proficiency" to proficiency,
      "experienceYears" to experienceYears.coerceIn(0L, 80L),
      "description" to description.trim().take(400),
      "isCustom" to isCustom,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      syncProfileSkills(uid)
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateSkill(
    skillId: String,
    name: String,
    category: String,
    proficiency: String,
    experienceYears: Long,
    description: String
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("user_skills").document(skillId)
    val updates = mapOf(
      "name" to name.trim().take(60),
      "category" to category,
      "proficiency" to proficiency,
      "experienceYears" to experienceYears.coerceIn(0L, 80L),
      "description" to description.trim().take(400),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.update(updates).await()
      syncProfileSkills(uid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteSkill(skillId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("user_skills").document(skillId)
    try {
      docRef.delete().await()
      syncProfileSkills(uid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeMyLearningGoals(): Flow<List<LearningGoal>> = flow {
    val uid = requireUserId()
    val path = "learning_skills"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereEqualTo("userId", uid)
          .snapshots()
          .map { snap -> snap.toObjects(LearningGoal::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun addLearningGoal(
    skillName: String,
    currentLevel: String,
    targetLevel: String,
    priority: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("learning_skills").document()
    val payload = mapOf(
      "userId" to uid,
      "skillName" to skillName.trim().take(60),
      "currentLevel" to currentLevel,
      "targetLevel" to targetLevel,
      "priority" to priority,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      syncProfileLearningGoals(uid)
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteLearningGoal(goalId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("learning_skills").document(goalId)
    try {
      docRef.delete().await()
      syncProfileLearningGoals(uid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  private suspend fun syncProfileSkills(uid: String) {
    val skillsSnap = db.collection("user_skills").whereEqualTo("userId", uid).get().await()
    val names = skillsSnap.documents.mapNotNull { it.getString("name") }.distinct().take(20)
    db.collection("profiles").document(uid).update(
      mapOf(
        "teachSkillNames" to names,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
  }

  private suspend fun syncProfileLearningGoals(uid: String) {
    val goalsSnap = db.collection("learning_skills").whereEqualTo("userId", uid).get().await()
    val names = goalsSnap.documents.mapNotNull { it.getString("skillName") }.distinct().take(20)
    db.collection("profiles").document(uid).update(
      mapOf(
        "learnSkillNames" to names,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
  }

  // --- EXCHANGE REQUESTS ---

  fun observeMyExchangeRequests(): Flow<List<ExchangeRequest>> = flow {
    val uid = requireUserId()
    val path = "exchange_requests"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereArrayContains("participantIds", uid)
          .snapshots()
          .map { snap -> snap.toObjects(ExchangeRequest::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun sendExchangeRequest(
    senderName: String,
    receiverId: String,
    receiverName: String,
    offeredSkills: List<String>,
    requestedSkills: List<String>,
    message: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("exchange_requests").document()
    val payload = mapOf(
      "senderId" to uid,
      "senderName" to senderName.take(80),
      "receiverId" to receiverId,
      "receiverName" to receiverName.take(80),
      "participantIds" to listOf(uid, receiverId),
      "offeredSkills" to offeredSkills.take(10),
      "requestedSkills" to requestedSkills.take(10),
      "message" to message.trim().take(500),
      "status" to ExchangeStatus.PENDING.name,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      createNotification(
        type = NotificationType.EXCHANGE_REQUEST,
        title = "Exchange Request Sent",
        body = "Sent a skill swap request to $receiverName for ${requestedSkills.joinToString(", ")}.",
        relatedId = docRef.id
      )
      // Automatically open/ensure conversation if swapping with community peer
      ensureConversation(senderName, receiverId, receiverName, message.ifBlank { "Hi $receiverName! I'd love to exchange skills with you." })
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateExchangeStatus(requestId: String, newStatus: ExchangeStatus): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("exchange_requests").document(requestId)
    try {
      docRef.update(
        mapOf(
          "status" to newStatus.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      if (newStatus == ExchangeStatus.COMPLETED) {
        awardReputation(uid, 10L, "Completed Skill Exchange (+10 pts)")
      } else if (newStatus == ExchangeStatus.CANCELLED) {
        adjustReputation(uid, -5L)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- CONVERSATIONS & CHAT ---

  fun observeMyConversations(): Flow<List<Conversation>> = flow {
    val uid = requireUserId()
    val path = "conversations"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereArrayContains("participantIds", uid)
          .snapshots()
          .map { snap -> snap.toObjects(Conversation::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
    val uid = requireUserId()
    val path = "conversations/$conversationId/messages"
    emitAll(
      wrapFlowError(
        db.collection("conversations").document(conversationId).collection("messages")
          .whereArrayContains("participantIds", uid)
          .snapshots()
          .map { snap ->
            snap.toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
              .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
          },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun ensureConversation(
    myName: String,
    peerId: String,
    peerName: String,
    initialText: String
  ): String {
    val uid = requireUserId()
    val convId = listOf(uid, peerId).sorted().joinToString("_")
    val convRef = db.collection("conversations").document(convId)
    val existing = convRef.get().await()
    if (!existing.exists()) {
      val payload = mapOf(
        "participantIds" to listOf(uid, peerId),
        "participantNames" to listOf(myName.take(80), peerName.take(80)),
        "lastMessage" to initialText.take(1000),
        "lastSenderId" to uid,
        "unreadCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      convRef.set(payload).await()
      val msgRef = convRef.collection("messages").document()
      msgRef.set(
        mapOf(
          "conversationId" to convId,
          "senderId" to uid,
          "senderName" to myName.take(80),
          "participantIds" to listOf(uid, peerId),
          "text" to initialText.take(1000),
          "isRead" to true,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    }
    return convId
  }

  suspend fun sendMessage(
    conversationId: String,
    senderName: String,
    participantIds: List<String>,
    text: String
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanText = text.trim().take(1000)
    require(cleanText.isNotEmpty()) { "Message cannot be empty" }
    val convRef = db.collection("conversations").document(conversationId)
    val msgRef = convRef.collection("messages").document()
    try {
      msgRef.set(
        mapOf(
          "conversationId" to conversationId,
          "senderId" to uid,
          "senderName" to senderName.take(80),
          "participantIds" to participantIds,
          "text" to cleanText,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      convRef.update(
        mapOf(
          "lastMessage" to cleanText,
          "lastSenderId" to uid,
          "unreadCount" to 0L,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      throw e
    }
  }

  // --- SESSIONS ---

  fun observeMySessions(): Flow<List<LearningSession>> = flow {
    val uid = requireUserId()
    val path = "sessions"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereArrayContains("participantIds", uid)
          .snapshots()
          .map { snap -> snap.toObjects(LearningSession::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun scheduleSession(
    hostName: String,
    guestId: String,
    guestName: String,
    skillName: String,
    dateLabel: String,
    startTime: String,
    endTime: String,
    description: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("sessions").document()
    val roomCode = "swap-${docRef.id.take(8)}"
    val payload = mapOf(
      "hostId" to uid,
      "hostName" to hostName.take(80),
      "guestId" to guestId,
      "guestName" to guestName.take(80),
      "participantIds" to listOf(uid, guestId),
      "skillName" to skillName.trim().take(60),
      "dateLabel" to dateLabel.trim().take(40),
      "startTime" to startTime.trim().take(20),
      "endTime" to endTime.trim().take(20),
      "description" to description.trim().take(500),
      "roomCode" to roomCode,
      "status" to SessionStatus.SCHEDULED.name,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      createNotification(
        type = NotificationType.SESSION_REMINDER,
        title = "Session Scheduled",
        body = "$skillName session with $guestName on $dateLabel ($startTime–$endTime).",
        relatedId = docRef.id
      )
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateSessionStatus(sessionId: String, status: SessionStatus): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("sessions").document(sessionId)
    try {
      docRef.update(
        mapOf(
          "status" to status.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      if (status == SessionStatus.COMPLETED) {
        awardReputation(uid, 10L, "Completed Learning Session (+10 pts)")
      } else if (status == SessionStatus.CANCELLED) {
        adjustReputation(uid, -5L)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- COLLABORATION PROJECTS ---

  fun observeOpenProjects(): Flow<List<CollabProject>> = flow {
    val uid = requireUserId()
    val path = "projects"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereEqualTo("status", ProjectStatus.OPEN.name)
          .snapshots()
          .map { snap -> snap.toObjects(CollabProject::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  fun observeMyProjects(): Flow<List<CollabProject>> = flow {
    val uid = requireUserId()
    val path = "projects"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereArrayContains("memberIds", uid)
          .snapshots()
          .map { snap -> snap.toObjects(CollabProject::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun createProject(
    ownerName: String,
    title: String,
    description: String,
    requiredSkills: List<String>,
    deadline: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("projects").document()
    val payload = mapOf(
      "ownerId" to uid,
      "ownerName" to ownerName.take(80),
      "title" to title.trim().take(100),
      "description" to description.trim().take(1000),
      "requiredSkills" to requiredSkills.take(10),
      "memberIds" to listOf(uid),
      "memberNames" to listOf(ownerName.take(80)),
      "deadline" to deadline.trim().take(40),
      "status" to ProjectStatus.OPEN.name,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      awardReputation(uid, 5L, "Created Community Project (+5 pts)")
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun joinProject(project: CollabProject, myName: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    if (project.id.startsWith("seed_")) {
      // Fork/instantiate community project into user's live Firestore workspace
      createProject(
        ownerName = project.ownerName,
        title = project.title,
        description = project.description,
        requiredSkills = project.requiredSkills,
        deadline = project.deadline
      ).getOrThrow()
      return@runCatching
    }
    val docRef = db.collection("projects").document(project.id)
    val newMemberIds = (project.memberIds + uid).distinct().take(20)
    val newMemberNames = (project.memberNames + myName).distinct().take(20)
    try {
      docRef.update(
        mapOf(
          "memberIds" to newMemberIds,
          "memberNames" to newMemberNames,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun leaveProject(project: CollabProject, myName: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("projects").document(project.id)
    val newMemberIds = project.memberIds.filter { it != uid }
    val newMemberNames = project.memberNames.filter { it != myName }
    if (newMemberIds.isEmpty()) {
      docRef.delete().await()
      return@runCatching
    }
    try {
      docRef.update(
        mapOf(
          "memberIds" to newMemberIds,
          "memberNames" to newMemberNames,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun completeProject(projectId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("projects").document(projectId)
    try {
      docRef.update(
        mapOf(
          "status" to ProjectStatus.COMPLETED.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      awardReputation(uid, 10L, "Completed Project (+10 pts)")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- REVIEWS & REPUTATION ---

  fun observeMyReviews(): Flow<List<UserReview>> = flow {
    val uid = requireUserId()
    val path = "reviews"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereEqualTo("reviewedUserId", uid)
          .snapshots()
          .map { snap -> snap.toObjects(UserReview::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  fun observeReviewsAuthoredByMe(): Flow<List<UserReview>> = flow {
    val uid = requireUserId()
    val path = "reviews"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereEqualTo("reviewerId", uid)
          .snapshots()
          .map { snap -> snap.toObjects(UserReview::class.java, ServerTimestampBehavior.ESTIMATE) },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun submitReview(
    reviewerName: String,
    reviewedUserId: String,
    reviewedUserName: String,
    sessionId: String,
    rating: Long,
    comment: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(reviewedUserId != uid) { "Cannot review yourself" }
    val docRef = db.collection("reviews").document()
    val payload = mapOf(
      "reviewerId" to uid,
      "reviewerName" to reviewerName.take(80),
      "reviewedUserId" to reviewedUserId,
      "reviewedUserName" to reviewedUserName.take(80),
      "sessionId" to sessionId.take(128),
      "rating" to rating.coerceIn(1L, 5L),
      "comment" to comment.trim().take(600),
      "createdAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      awardReputation(uid, 5L, "Helped community with peer review (+5 pts)")
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  private suspend fun awardReputation(uid: String, delta: Long, reason: String) {
    val profRef = db.collection("profiles").document(uid)
    val snap = profRef.get().await()
    val currentPoints = snap.getLong("reputationPoints") ?: 0L
    val updatedPoints = (currentPoints + delta).coerceAtLeast(0L)
    val updatedBadges = BadgeCatalog.badgesForPoints(updatedPoints)
    val oldBadges = (snap.get("badges") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    profRef.update(
      mapOf(
        "reputationPoints" to updatedPoints,
        "badges" to updatedBadges,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
    val newlyEarned = updatedBadges - oldBadges.toSet()
    newlyEarned.forEach { badgeName ->
      createNotification(
        type = NotificationType.BADGE_EARNED,
        title = "Badge Unlocked: $badgeName",
        body = "$reason — You unlocked $badgeName!",
        relatedId = badgeName
      )
    }
  }

  private suspend fun adjustReputation(uid: String, delta: Long) {
    val profRef = db.collection("profiles").document(uid)
    val snap = profRef.get().await()
    val currentPoints = snap.getLong("reputationPoints") ?: 0L
    val updatedPoints = (currentPoints + delta).coerceAtLeast(0L)
    profRef.update(
      mapOf(
        "reputationPoints" to updatedPoints,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    ).await()
  }

  // --- NOTIFICATIONS ---

  fun observeMyNotifications(): Flow<List<AppNotification>> = flow {
    val uid = requireUserId()
    val path = "notifications"
    emitAll(
      wrapFlowError(
        db.collection(path)
          .whereEqualTo("userId", uid)
          .snapshots()
          .map { snap ->
            snap.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
          },
        OperationType.LIST,
        path,
        uid
      )
    )
  }

  suspend fun createNotification(
    type: NotificationType,
    title: String,
    body: String,
    relatedId: String = ""
  ) {
    val uid = requireUserId()
    val docRef = db.collection("notifications").document()
    val payload = mapOf(
      "userId" to uid,
      "type" to type.name,
      "title" to title.take(120),
      "body" to body.take(400),
      "isRead" to false,
      "relatedId" to relatedId.take(128),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
    }
  }

  suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
    val docRef = db.collection("notifications").document(notificationId)
    try {
      docRef.update(
        mapOf(
          "isRead" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun markAllNotificationsRead(notifications: List<AppNotification>): Result<Unit> = runCatching {
    val unread = notifications.filter { !it.isRead && it.id.isNotBlank() }
    if (unread.isEmpty()) return@runCatching
    val batch = db.batch()
    unread.forEach { n ->
      batch.update(
        db.collection("notifications").document(n.id),
        mapOf("isRead" to true, "updatedAt" to FieldValue.serverTimestamp())
      )
    }
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "notifications")
      throw e
    }
  }

  // --- ADMIN MODERATION ---

  suspend fun setProfileActiveStatus(profileId: String, isActive: Boolean): Result<Unit> = runCatching {
    val docRef = db.collection("profiles").document(profileId)
    try {
      docRef.update(
        mapOf(
          "isActive" to isActive,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }
}
