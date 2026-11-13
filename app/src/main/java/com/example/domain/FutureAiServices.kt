package com.example.domain

import com.example.model.PublicProfile

/**
 * Future AI Architecture Interfaces (Ready for integration when AI services are enabled).
 * Current matching uses deterministic transparent scoring in MatchingServiceImpl.
 */
interface AIRecommendationService {
  suspend fun recommendSkillPaths(profile: PublicProfile): List<String>
}

interface SkillGapService {
  suspend fun analyzeSkillGaps(currentSkills: List<String>, targetRole: String): List<String>
}

interface AITranslationService {
  suspend fun translateMessage(text: String, targetLanguageCode: String): String
}

interface AIProfileService {
  suspend fun suggestBioImprovements(profile: PublicProfile): String
}
