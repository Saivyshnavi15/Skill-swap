package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SeedCommunityCatalog
import com.example.data.SkillSwapRepository
import com.example.data.UserPreferencesDataStore
import com.example.domain.MatchingService
import com.example.domain.MatchingServiceImpl
import com.example.domain.TranslationService
import com.example.model.AppLanguage
import com.example.model.AppNotification
import com.example.model.ChatMessage
import com.example.model.CollabProject
import com.example.model.Conversation
import com.example.model.ExchangeRequest
import com.example.model.ExchangeStatus
import com.example.model.LearningGoal
import com.example.model.LearningSession
import com.example.model.PublicProfile
import com.example.model.SessionStatus
import com.example.model.SkillCategory
import com.example.model.SkillMatchResult
import com.example.model.UiState
import com.example.model.UserPrivate
import com.example.model.UserReview
import com.example.model.UserSkill
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExploreFilterState(
  val query: String = "",
  val category: SkillCategory? = null,
  val language: String = "All",
  val proficiency: String = "All",
  val minRating: Double = 0.0,
  val availability: String = "All"
)

class SkillSwapViewModel(
  private val repository: SkillSwapRepository,
  private val prefs: UserPreferencesDataStore,
  private val currentUserId: String,
  private val currentUserEmail: String?,
  private val currentUserDisplayName: String?,
  private val matchingService: MatchingService = MatchingServiceImpl()
) : ViewModel() {

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _exploreFilter = MutableStateFlow(ExploreFilterState())
  val exploreFilter: StateFlow<ExploreFilterState> = _exploreFilter.asStateFlow()

  val isAdminUser: Boolean =
    currentUserEmail?.equals("ksaivyshnavi406@gmail.com", ignoreCase = true) == true

  val appLanguage: StateFlow<AppLanguage> = prefs.appLanguage
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppLanguage.ENGLISH)

  val onboardingCompleted: StateFlow<Boolean> = prefs.onboardingCompleted
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

  val myPrivateUser: StateFlow<UserPrivate?> = repository.observeMyPrivateUser()
    .catch { e -> logFlowError("myPrivateUser", e); emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  val myProfileState: StateFlow<UiState<PublicProfile>> = repository.observeMyProfile()
    .map<PublicProfile?, UiState<PublicProfile>> { profile ->
      if (profile != null) {
        UiState.Success(profile)
      } else {
        UiState.Success(
          PublicProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = currentUserDisplayName ?: "SkillSwap Learner"
          )
        )
      }
    }
    .catch { e ->
      val root = unwrapFirestoreError(e)
      if (root == null && e is CancellationException) throw e
      Log.w(TAG, "Error observing profile", root ?: e)
      emit(UiState.Error((root ?: e).message ?: "Failed to load profile"))
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val mySkills: StateFlow<List<UserSkill>> = repository.observeMySkills()
    .catch { e -> logFlowError("mySkills", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val myLearningGoals: StateFlow<List<LearningGoal>> = repository.observeMyLearningGoals()
    .catch { e -> logFlowError("myLearningGoals", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val communityProfiles: StateFlow<List<PublicProfile>> = repository.observeCommunityProfiles()
    .map { firestoreProfiles ->
      val combined = (firestoreProfiles + SeedCommunityCatalog.communityProfiles)
        .distinctBy { it.userId }
        .filter { it.isActive }
      combined
    }
    .catch { e ->
      logFlowError("communityProfiles", e)
      emit(SeedCommunityCatalog.communityProfiles)
    }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
      SeedCommunityCatalog.communityProfiles
    )

  val matches: StateFlow<List<SkillMatchResult>> =
    combine(myProfileState, communityProfiles) { profileState, peers ->
      val me = (profileState as? UiState.Success)?.data
        ?: PublicProfile(id = currentUserId, userId = currentUserId, displayName = currentUserDisplayName ?: "Learner")
      matchingService.rankMatches(me, peers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val filteredExploreMatches: StateFlow<List<SkillMatchResult>> =
    combine(matches, _exploreFilter) { allMatches, filter ->
      allMatches.filter { match ->
        val p = match.peerProfile
        val matchesQuery = filter.query.isBlank() ||
          p.displayName.contains(filter.query, ignoreCase = true) ||
          p.bio.contains(filter.query, ignoreCase = true) ||
          p.location.contains(filter.query, ignoreCase = true) ||
          p.teachSkillNames.any { it.contains(filter.query, ignoreCase = true) } ||
          p.learnSkillNames.any { it.contains(filter.query, ignoreCase = true) }

        val matchesCategory = filter.category == null || p.teachSkillNames.any { skillName ->
          val template = SeedCommunityCatalog.catalogSkills.firstOrNull {
            it.name.equals(skillName, ignoreCase = true)
          }
          template?.category == filter.category
        }

        val matchesLanguage = filter.language == "All" ||
          p.spokenLanguages.any { it.equals(filter.language, ignoreCase = true) }

        val matchesRating = p.ratingAvg >= filter.minRating

        val matchesAvail = filter.availability == "All" ||
          p.availability.contains(filter.availability, ignoreCase = true)

        matchesQuery && matchesCategory && matchesLanguage && matchesRating && matchesAvail
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val exchangeRequests: StateFlow<List<ExchangeRequest>> = repository.observeMyExchangeRequests()
    .catch { e -> logFlowError("exchangeRequests", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val conversations: StateFlow<List<Conversation>> = repository.observeMyConversations()
    .catch { e -> logFlowError("conversations", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val sessions: StateFlow<List<LearningSession>> = repository.observeMySessions()
    .catch { e -> logFlowError("sessions", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val projects: StateFlow<List<CollabProject>> =
    combine(repository.observeOpenProjects(), repository.observeMyProjects()) { openList, myList ->
      (myList + openList + SeedCommunityCatalog.communityProjects).distinctBy { it.id }
    }
      .catch { e ->
        logFlowError("projects", e)
        emit(SeedCommunityCatalog.communityProjects)
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        SeedCommunityCatalog.communityProjects
      )

  val myReceivedReviews: StateFlow<List<UserReview>> = repository.observeMyReviews()
    .catch { e -> logFlowError("myReceivedReviews", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val myAuthoredReviews: StateFlow<List<UserReview>> = repository.observeReviewsAuthoredByMe()
    .catch { e -> logFlowError("myAuthoredReviews", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val notifications: StateFlow<List<AppNotification>> = repository.observeMyNotifications()
    .catch { e -> logFlowError("notifications", e); emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  init {
    viewModelScope.launch {
      repository.ensureUserInitialized(
        displayName = currentUserDisplayName ?: "SkillSwap Learner",
        email = currentUserEmail
      ).onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to initialize profile"
      }
    }
  }

  fun t(key: String): String = TranslationService.t(appLanguage.value, key)

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun updateExploreFilter(transform: (ExploreFilterState) -> ExploreFilterState) {
    _exploreFilter.value = transform(_exploreFilter.value)
  }

  fun setOnboardingCompleted(done: Boolean) {
    viewModelScope.launch { prefs.setOnboardingCompleted(done) }
  }

  fun setLanguages(
    appLang: AppLanguage,
    preferredCommLanguage: String,
    privacyShowLocation: Boolean = myPrivateUser.value?.privacyShowLocation ?: true,
    privacyAllowInvites: Boolean = myPrivateUser.value?.privacyAllowInvites ?: true
  ) {
    viewModelScope.launch {
      prefs.setAppLanguage(appLang)
      prefs.setCommLanguage(preferredCommLanguage)
      repository.updateUserSettings(
        appLanguage = appLang.code,
        preferredCommLanguage = preferredCommLanguage,
        privacyShowLocation = privacyShowLocation,
        privacyAllowInvites = privacyAllowInvites
      ).onSuccess {
        _feedbackMessage.value = "Language & privacy preferences saved"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Could not save settings"
      }
    }
  }

  fun updateProfile(
    displayName: String,
    bio: String,
    location: String,
    photoUri: String,
    spokenLanguages: List<String>,
    preferredLanguage: String,
    availability: String,
    experienceYears: Long
  ) {
    viewModelScope.launch {
      repository.updateMyProfile(
        displayName = displayName,
        bio = bio,
        location = location,
        photoUri = photoUri,
        spokenLanguages = spokenLanguages,
        preferredLanguage = preferredLanguage,
        availability = availability,
        experienceYears = experienceYears
      ).onSuccess {
        _feedbackMessage.value = "Profile updated"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to update profile"
      }
    }
  }

  fun addSkill(
    name: String,
    category: SkillCategory,
    proficiency: String,
    experienceYears: Long,
    description: String,
    isCustom: Boolean
  ) {
    viewModelScope.launch {
      repository.addSkill(
        name = name,
        category = category.name,
        proficiency = proficiency,
        experienceYears = experienceYears,
        description = description,
        isCustom = isCustom
      ).onSuccess {
        _feedbackMessage.value = "Added $name to your teaching skills"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to add skill"
      }
    }
  }

  fun updateSkill(
    skillId: String,
    name: String,
    category: SkillCategory,
    proficiency: String,
    experienceYears: Long,
    description: String
  ) {
    viewModelScope.launch {
      repository.updateSkill(
        skillId = skillId,
        name = name,
        category = category.name,
        proficiency = proficiency,
        experienceYears = experienceYears,
        description = description
      ).onSuccess {
        _feedbackMessage.value = "Updated $name"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to update skill"
      }
    }
  }

  fun deleteSkill(skillId: String) {
    viewModelScope.launch {
      repository.deleteSkill(skillId)
        .onSuccess { _feedbackMessage.value = "Skill removed" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to delete skill" }
    }
  }

  fun addLearningGoal(
    skillName: String,
    currentLevel: String,
    targetLevel: String,
    priority: String
  ) {
    viewModelScope.launch {
      repository.addLearningGoal(skillName, currentLevel, targetLevel, priority)
        .onSuccess { _feedbackMessage.value = "Added learning goal: $skillName" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to add goal" }
    }
  }

  fun deleteLearningGoal(goalId: String) {
    viewModelScope.launch {
      repository.deleteLearningGoal(goalId)
        .onSuccess { _feedbackMessage.value = "Learning goal removed" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to delete goal" }
    }
  }

  fun sendExchangeRequest(
    peer: PublicProfile,
    offeredSkills: List<String>,
    requestedSkills: List<String>,
    message: String
  ) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      repository.sendExchangeRequest(
        senderName = myName,
        receiverId = peer.userId,
        receiverName = peer.displayName,
        offeredSkills = offeredSkills.ifEmpty { listOf("Knowledge Sharing") },
        requestedSkills = requestedSkills.ifEmpty { peer.teachSkillNames.take(1).ifEmpty { listOf("Mentorship") } },
        message = message
      ).onSuccess {
        _feedbackMessage.value = "Exchange request sent to ${peer.displayName}!"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to send request"
      }
    }
  }

  fun updateExchangeStatus(requestId: String, status: ExchangeStatus) {
    viewModelScope.launch {
      repository.updateExchangeStatus(requestId, status)
        .onSuccess { _feedbackMessage.value = "Request marked ${status.name.lowercase()}" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update request" }
    }
  }

  fun openOrStartChat(peer: PublicProfile, onReady: (String) -> Unit) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      try {
        val convId = repository.ensureConversation(
          myName = myName,
          peerId = peer.userId,
          peerName = peer.displayName,
          initialText = "Hi ${peer.displayName}! Excited to connect on SkillSwap."
        )
        onReady(convId)
      } catch (e: Exception) {
        _feedbackMessage.value = e.localizedMessage ?: "Could not open chat"
      }
    }
  }

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
    repository.observeMessages(conversationId)

  fun sendMessage(conversation: Conversation, text: String) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      repository.sendMessage(
        conversationId = conversation.id,
        senderName = myName,
        participantIds = conversation.participantIds,
        text = text
      ).onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to send message"
      }
    }
  }

  fun scheduleSession(
    peer: PublicProfile,
    skillName: String,
    dateLabel: String,
    startTime: String,
    endTime: String,
    description: String
  ) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      repository.scheduleSession(
        hostName = myName,
        guestId = peer.userId,
        guestName = peer.displayName,
        skillName = skillName,
        dateLabel = dateLabel,
        startTime = startTime,
        endTime = endTime,
        description = description
      ).onSuccess {
        _feedbackMessage.value = "Session scheduled with ${peer.displayName}"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to schedule session"
      }
    }
  }

  fun updateSessionStatus(sessionId: String, status: SessionStatus) {
    viewModelScope.launch {
      repository.updateSessionStatus(sessionId, status)
        .onSuccess { _feedbackMessage.value = "Session updated: ${status.name.lowercase()}" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to update session" }
    }
  }

  fun createProject(
    title: String,
    description: String,
    requiredSkills: List<String>,
    deadline: String
  ) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      repository.createProject(myName, title, description, requiredSkills, deadline)
        .onSuccess { _feedbackMessage.value = "Project '$title' created!" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to create project" }
    }
  }

  fun joinProject(project: CollabProject) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      repository.joinProject(project, myName)
        .onSuccess { _feedbackMessage.value = "Joined '${project.title}'!" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to join project" }
    }
  }

  fun leaveProject(project: CollabProject) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      repository.leaveProject(project, myName)
        .onSuccess { _feedbackMessage.value = "Left '${project.title}'" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to leave project" }
    }
  }

  fun completeProject(projectId: String) {
    viewModelScope.launch {
      repository.completeProject(projectId)
        .onSuccess { _feedbackMessage.value = "Project completed! +10 Reputation pts" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to complete project" }
    }
  }

  fun submitReview(
    reviewedUserId: String,
    reviewedUserName: String,
    sessionId: String,
    rating: Long,
    comment: String
  ) {
    val myName = (myProfileState.value as? UiState.Success)?.data?.displayName ?: currentUserDisplayName ?: "Learner"
    viewModelScope.launch {
      repository.submitReview(myName, reviewedUserId, reviewedUserName, sessionId, rating, comment)
        .onSuccess { _feedbackMessage.value = "Review submitted! +5 Reputation pts" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to submit review" }
    }
  }

  fun markNotificationRead(id: String) {
    viewModelScope.launch { repository.markNotificationRead(id) }
  }

  fun markAllNotificationsRead() {
    viewModelScope.launch { repository.markAllNotificationsRead(notifications.value) }
  }

  fun adminToggleProfileActive(profileId: String, isActive: Boolean) {
    viewModelScope.launch {
      repository.setProfileActiveStatus(profileId, isActive)
        .onSuccess { _feedbackMessage.value = "Updated user active status" }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Admin action failed" }
    }
  }

  fun currentUid(): String = currentUserId

  private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
    generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()

  private fun logFlowError(tag: String, error: Throwable) {
    val root = unwrapFirestoreError(error)
    if (root == null && error is CancellationException) throw error
    Log.w(TAG, "Stream error in $tag", root ?: error)
  }

  private companion object {
    const val TAG = "SkillSwapVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
