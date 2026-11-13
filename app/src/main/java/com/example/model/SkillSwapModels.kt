package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class SkillCategory(val labelKey: String) {
  TECHNICAL("cat_technical"),
  CREATIVE("cat_creative"),
  PROFESSIONAL("cat_professional"),
  HOBBY("cat_hobby"),
  PRACTICAL("cat_practical")
}

enum class Proficiency(val level: Int, val displayLabel: String) {
  BEGINNER(1, "Beginner"),
  INTERMEDIATE(2, "Intermediate"),
  ADVANCED(3, "Advanced"),
  EXPERT(4, "Expert")
}

enum class GoalPriority(val displayLabel: String) {
  LOW("Low"),
  MEDIUM("Medium"),
  HIGH("High")
}

enum class ExchangeStatus {
  PENDING,
  ACCEPTED,
  REJECTED,
  CANCELLED,
  COMPLETED
}

enum class SessionStatus {
  SCHEDULED,
  STARTED,
  COMPLETED,
  CANCELLED
}

enum class ProjectStatus {
  OPEN,
  IN_PROGRESS,
  COMPLETED
}

enum class NotificationType {
  EXCHANGE_REQUEST,
  REQUEST_ACCEPTED,
  REQUEST_REJECTED,
  NEW_MESSAGE,
  SESSION_REMINDER,
  PROJECT_INVITATION,
  NEW_REVIEW,
  BADGE_EARNED
}

enum class AppLanguage(val code: String, val nativeName: String, val englishName: String) {
  ENGLISH("en", "English", "English"),
  TELUGU("te", "తెలుగు", "Telugu"),
  HINDI("hi", "हिन्दी", "Hindi"),
  TAMIL("ta", "தமிழ்", "Tamil"),
  KANNADA("kn", "ಕನ್ನಡ", "Kannada"),
  MALAYALAM("ml", "മലയാളം", "Malayalam");

  companion object {
    fun fromCode(code: String): AppLanguage =
      entries.firstOrNull { it.code == code } ?: ENGLISH
  }
}

@IgnoreExtraProperties
data class UserPrivate(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String? = null,
  val appLanguage: String = "en",
  val preferredCommLanguage: String = "English",
  val privacyShowLocation: Boolean = true,
  val privacyAllowInvites: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val bio: String = "",
  val location: String = "",
  val photoUri: String = "",
  val spokenLanguages: List<String> = listOf("English"),
  val preferredLanguage: String = "English",
  val availability: String = "Weekdays 6-9 PM",
  val experienceYears: Long = 1L,
  val teachSkillNames: List<String> = emptyList(),
  val learnSkillNames: List<String> = emptyList(),
  val ratingAvg: Double = 0.0,
  val reviewCount: Long = 0L,
  val reputationPoints: Long = 0L,
  val badges: List<String> = emptyList(),
  @get:PropertyName("isActive") @PropertyName("isActive") val isActive: Boolean = true,
  @get:PropertyName("isOnline") @PropertyName("isOnline") val isOnline: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val profileCompletionPercent: Int
    get() {
      var score = 20
      if (displayName.isNotBlank()) score += 10
      if (bio.isNotBlank()) score += 15
      if (location.isNotBlank()) score += 10
      if (spokenLanguages.isNotEmpty()) score += 10
      if (availability.isNotBlank()) score += 10
      if (teachSkillNames.isNotEmpty()) score += 15
      if (learnSkillNames.isNotEmpty()) score += 10
      return score.coerceIn(0, 100)
    }
}

@IgnoreExtraProperties
data class UserSkill(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val category: String = SkillCategory.TECHNICAL.name,
  val proficiency: String = Proficiency.INTERMEDIATE.name,
  val experienceYears: Long = 1L,
  val description: String = "",
  @get:PropertyName("isCustom") @PropertyName("isCustom") val isCustom: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LearningGoal(
  @DocumentId val id: String = "",
  val userId: String = "",
  val skillName: String = "",
  val currentLevel: String = Proficiency.BEGINNER.name,
  val targetLevel: String = Proficiency.ADVANCED.name,
  val priority: String = GoalPriority.HIGH.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SkillMatchResult(
  val peerProfile: PublicProfile,
  val matchPercentage: Int,
  val commonLanguages: List<String>,
  val skillsYouCanTeach: List<String>,
  val skillsYouCanLearn: List<String>,
  val availabilityOverlap: String,
  val reason: String,
  val scoreBreakdown: Map<String, Int>
)

@IgnoreExtraProperties
data class ExchangeRequest(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val receiverId: String = "",
  val receiverName: String = "",
  val participantIds: List<String> = emptyList(),
  val offeredSkills: List<String> = emptyList(),
  val requestedSkills: List<String> = emptyList(),
  val message: String = "",
  val status: String = ExchangeStatus.PENDING.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
  @DocumentId val id: String = "",
  val participantIds: List<String> = emptyList(),
  val participantNames: List<String> = emptyList(),
  val lastMessage: String = "",
  val lastSenderId: String = "",
  val unreadCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val conversationId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val participantIds: List<String> = emptyList(),
  val text: String = "",
  @get:PropertyName("isRead") @PropertyName("isRead") val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LearningSession(
  @DocumentId val id: String = "",
  val hostId: String = "",
  val hostName: String = "",
  val guestId: String = "",
  val guestName: String = "",
  val participantIds: List<String> = emptyList(),
  val skillName: String = "",
  val dateLabel: String = "",
  val startTime: String = "",
  val endTime: String = "",
  val description: String = "",
  val roomCode: String = "",
  val status: String = SessionStatus.SCHEDULED.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CollabProject(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val ownerName: String = "",
  val title: String = "",
  val description: String = "",
  val requiredSkills: List<String> = emptyList(),
  val memberIds: List<String> = emptyList(),
  val memberNames: List<String> = emptyList(),
  val deadline: String = "",
  val status: String = ProjectStatus.OPEN.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserReview(
  @DocumentId val id: String = "",
  val reviewerId: String = "",
  val reviewerName: String = "",
  val reviewedUserId: String = "",
  val reviewedUserName: String = "",
  val sessionId: String = "",
  val rating: Long = 5L,
  val comment: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val type: String = NotificationType.EXCHANGE_REQUEST.name,
  val title: String = "",
  val body: String = "",
  @get:PropertyName("isRead") @PropertyName("isRead") val isRead: Boolean = false,
  val relatedId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BadgeDefinition(
  val id: String,
  val title: String,
  val description: String,
  val requiredPoints: Int
)

object BadgeCatalog {
  val allBadges = listOf(
    BadgeDefinition("beginner_helper", "Beginner Helper", "Joined SkillSwap and shared your first skill", 5),
    BadgeDefinition("skill_sharer", "Skill Sharer", "Completed a peer skill exchange (+10 pts)", 15),
    BadgeDefinition("active_learner", "Active Learner", "Set learning goals & scheduled a session", 25),
    BadgeDefinition("community_contributor", "Community Contributor", "Collaborated on community projects", 40),
    BadgeDefinition("expert_mentor", "Expert Mentor", "Earned 5-star reviews from learners", 60),
    BadgeDefinition("collaboration_star", "Collaboration Star", "Top-rated community builder (100+ pts)", 100)
  )

  fun badgesForPoints(points: Long): List<String> =
    allBadges.filter { points >= it.requiredPoints }.map { it.title }
}
