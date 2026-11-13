package com.example.domain

import com.example.data.SeedCommunityCatalog
import com.example.model.AppLanguage
import com.example.model.PublicProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillSwapDomainUnitTest {

  @Test
  fun matchingService_saiAndRahul_producesHighExplainableMatch() {
    val sai = PublicProfile(
      id = "sai_uid",
      userId = "sai_uid",
      displayName = "Sai",
      spokenLanguages = listOf("English", "Telugu"),
      availability = "Weekdays 6-9 PM",
      experienceYears = 4L,
      teachSkillNames = listOf("Java", "Spring Boot", "SQL"),
      learnSkillNames = listOf("Photoshop", "Video Editing")
    )
    val rahul = SeedCommunityCatalog.communityProfiles.first { it.userId == "seed_rahul" }

    val service = MatchingServiceImpl()
    val result = service.computeMatch(sai, rahul)

    assertTrue("Expected >= 90 match, got ${result.matchPercentage}", result.matchPercentage >= 90)
    assertTrue(result.skillsYouCanTeach.containsAll(listOf("Java", "Spring Boot")))
    assertTrue(result.skillsYouCanLearn.containsAll(listOf("Photoshop", "Video Editing")))
    assertTrue(result.commonLanguages.containsAll(listOf("English", "Telugu")))
    assertTrue(result.reason.contains("Java"))
    assertTrue(result.reason.contains("Photoshop"))
  }

  @Test
  fun translationService_supportsAllSixLanguages() {
    AppLanguage.entries.forEach { lang ->
      val homeLabel = TranslationService.t(lang, "nav_home")
      assertTrue("Missing nav_home for ${lang.code}", homeLabel.isNotBlank())
    }
    assertEquals("హోమ్", TranslationService.t(AppLanguage.TELUGU, "nav_home"))
    assertEquals("होम", TranslationService.t(AppLanguage.HINDI, "nav_home"))
  }
}
