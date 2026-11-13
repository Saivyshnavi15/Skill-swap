package com.example.domain

import com.example.model.PublicProfile
import com.example.model.SkillMatchResult
import kotlin.math.min

interface MatchingService {
  fun computeMatch(currentUser: PublicProfile, candidate: PublicProfile): SkillMatchResult
  fun rankMatches(currentUser: PublicProfile, candidates: List<PublicProfile>): List<SkillMatchResult>
}

/**
 * Transparent, deterministic, explainable scoring engine.
 * Weighted across:
 *  - Mutual Skill Swap (they teach what you want + you teach what they want): up to 60 pts
 *  - Common Spoken Languages: up to 20 pts
 *  - Availability Overlap: up to 10 pts
 *  - Experience & Community Rating: up to 10 pts
 */
class MatchingServiceImpl : MatchingService {

  override fun computeMatch(currentUser: PublicProfile, candidate: PublicProfile): SkillMatchResult {
    val myTeach = currentUser.teachSkillNames.map { it.trim() }.filter { it.isNotEmpty() }
    val myLearn = currentUser.learnSkillNames.map { it.trim() }.filter { it.isNotEmpty() }
    val theirTeach = candidate.teachSkillNames.map { it.trim() }.filter { it.isNotEmpty() }
    val theirLearn = candidate.learnSkillNames.map { it.trim() }.filter { it.isNotEmpty() }

    val skillsYouCanTeach = myTeach.filter { skill ->
      theirLearn.any { it.equals(skill, ignoreCase = true) || it.contains(skill, ignoreCase = true) || skill.contains(it, ignoreCase = true) }
    }.distinct()

    val skillsYouCanLearn = theirTeach.filter { skill ->
      myLearn.any { it.equals(skill, ignoreCase = true) || it.contains(skill, ignoreCase = true) || skill.contains(it, ignoreCase = true) }
    }.distinct()

    val commonLanguages = currentUser.spokenLanguages.filter { lang ->
      candidate.spokenLanguages.any { it.equals(lang, ignoreCase = true) }
    }.distinct()

    // 1. Skill exchange score (up to 60)
    val teachScore = if (skillsYouCanTeach.isNotEmpty()) min(30, skillsYouCanTeach.size * 15) else 0
    val learnScore = if (skillsYouCanLearn.isNotEmpty()) min(30, skillsYouCanLearn.size * 15) else 0
    val mutualBonus = if (skillsYouCanTeach.isNotEmpty() && skillsYouCanLearn.isNotEmpty()) 10 else 0
    val skillScore = min(60, teachScore + learnScore + mutualBonus)

    // 2. Language score (up to 20)
    val languageScore = when {
      commonLanguages.size >= 2 -> 20
      commonLanguages.size == 1 -> 14
      else -> 4
    }

    // 3. Availability overlap (up to 10)
    val availOverlap = computeAvailabilityOverlap(currentUser.availability, candidate.availability)
    val availabilityScore = if (availOverlap.startsWith("Shared")) 10 else 5

    // 4. Community reputation & rating (up to 10)
    val ratingScore = ((candidate.ratingAvg / 5.0) * 6.0).toInt().coerceIn(0, 6)
    val expScore = min(4, candidate.experienceYears.toInt())
    val trustScore = min(10, ratingScore + expScore)

    val total = (skillScore + languageScore + availabilityScore + trustScore).coerceIn(15, 99)

    val reasonParts = mutableListOf<String>()
    if (skillsYouCanTeach.isNotEmpty()) {
      reasonParts.add("You can teach ${skillsYouCanTeach.joinToString(", ")}")
    }
    if (skillsYouCanLearn.isNotEmpty()) {
      reasonParts.add("${candidate.displayName} can teach ${skillsYouCanLearn.joinToString(", ")}")
    }
    if (commonLanguages.isNotEmpty()) {
      reasonParts.add("Shared languages: ${commonLanguages.joinToString(", ")}")
    }
    if (reasonParts.isEmpty()) {
      reasonParts.add("${candidate.displayName} is active in ${candidate.location.ifBlank { "the community" }} with ${candidate.teachSkillNames.take(2).joinToString(", ")}")
    }

    return SkillMatchResult(
      peerProfile = candidate,
      matchPercentage = total,
      commonLanguages = commonLanguages,
      skillsYouCanTeach = skillsYouCanTeach,
      skillsYouCanLearn = skillsYouCanLearn,
      availabilityOverlap = availOverlap,
      reason = reasonParts.joinToString(". ") + ".",
      scoreBreakdown = mapOf(
        "Skills Exchange" to skillScore,
        "Languages" to languageScore,
        "Availability" to availabilityScore,
        "Trust & Experience" to trustScore
      )
    )
  }

  override fun rankMatches(
    currentUser: PublicProfile,
    candidates: List<PublicProfile>
  ): List<SkillMatchResult> {
    return candidates
      .filter { it.userId != currentUser.userId && it.isActive }
      .map { computeMatch(currentUser, it) }
      .sortedByDescending { it.matchPercentage }
  }

  private fun computeAvailabilityOverlap(a: String, b: String): String {
    val aLow = a.lowercase()
    val bLow = b.lowercase()
    return when {
      aLow.contains("weekend") && bLow.contains("weekend") -> "Shared weekend slots"
      aLow.contains("weekday") && bLow.contains("weekday") -> "Shared weekday evenings"
      aLow.contains("evening") && bLow.contains("evening") -> "Shared evening schedule"
      aLow.contains("morning") && bLow.contains("morning") -> "Shared morning schedule"
      else -> "Flexible coordination"
    }
  }
}
