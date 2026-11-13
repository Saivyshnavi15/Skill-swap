package com.example.data

import com.example.model.CollabProject
import com.example.model.ProjectStatus
import com.example.model.PublicProfile
import com.example.model.SkillCategory
import com.example.model.UserReview

data class CatalogSkillTemplate(
  val name: String,
  val category: SkillCategory
)

object SeedCommunityCatalog {

  val catalogSkills: List<CatalogSkillTemplate> = listOf(
    CatalogSkillTemplate("Java", SkillCategory.TECHNICAL),
    CatalogSkillTemplate("Spring Boot", SkillCategory.TECHNICAL),
    CatalogSkillTemplate("SQL", SkillCategory.TECHNICAL),
    CatalogSkillTemplate("Kotlin", SkillCategory.TECHNICAL),
    CatalogSkillTemplate("Jetpack Compose", SkillCategory.TECHNICAL),
    CatalogSkillTemplate("Python", SkillCategory.TECHNICAL),
    CatalogSkillTemplate("React Native", SkillCategory.TECHNICAL),
    CatalogSkillTemplate("Photoshop", SkillCategory.CREATIVE),
    CatalogSkillTemplate("Video Editing", SkillCategory.CREATIVE),
    CatalogSkillTemplate("Canva", SkillCategory.CREATIVE),
    CatalogSkillTemplate("UI/UX Design", SkillCategory.CREATIVE),
    CatalogSkillTemplate("Illustration", SkillCategory.CREATIVE),
    CatalogSkillTemplate("Photography", SkillCategory.CREATIVE),
    CatalogSkillTemplate("Public Speaking", SkillCategory.PROFESSIONAL),
    CatalogSkillTemplate("Product Strategy", SkillCategory.PROFESSIONAL),
    CatalogSkillTemplate("Technical Writing", SkillCategory.PROFESSIONAL),
    CatalogSkillTemplate("Project Management", SkillCategory.PROFESSIONAL),
    CatalogSkillTemplate("Guitar", SkillCategory.HOBBY),
    CatalogSkillTemplate("Classical Vocal", SkillCategory.HOBBY),
    CatalogSkillTemplate("Chess Strategy", SkillCategory.HOBBY),
    CatalogSkillTemplate("Cooking", SkillCategory.PRACTICAL),
    CatalogSkillTemplate("Financial Literacy", SkillCategory.PRACTICAL),
    CatalogSkillTemplate("Spoken Telugu", SkillCategory.PRACTICAL),
    CatalogSkillTemplate("Spoken Hindi", SkillCategory.PRACTICAL)
  )

  val communityProfiles: List<PublicProfile> = listOf(
    PublicProfile(
      id = "seed_rahul",
      userId = "seed_rahul",
      displayName = "Rahul Verma",
      bio = "Motion designer & Photoshop mentor eager to learn backend engineering with Java and Spring Boot.",
      location = "Bengaluru, IN",
      spokenLanguages = listOf("English", "Hindi", "Telugu"),
      preferredLanguage = "English",
      availability = "Weekdays 6-9 PM",
      experienceYears = 5L,
      teachSkillNames = listOf("Photoshop", "Video Editing", "Canva"),
      learnSkillNames = listOf("Java", "Spring Boot", "SQL"),
      ratingAvg = 4.9,
      reviewCount = 14L,
      reputationPoints = 125L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Expert Mentor", "Collaboration Star"),
      isActive = true,
      isOnline = true
    ),
    PublicProfile(
      id = "seed_ananya",
      userId = "seed_ananya",
      displayName = "Ananya Reddy",
      bio = "Product designer crafting accessible design systems. Looking to exchange UI/UX mentoring for Kotlin.",
      location = "Hyderabad, IN",
      spokenLanguages = listOf("English", "Telugu", "Hindi"),
      preferredLanguage = "Telugu",
      availability = "Weekends & Weekdays 7-9 PM",
      experienceYears = 4L,
      teachSkillNames = listOf("UI/UX Design", "Illustration", "Canva"),
      learnSkillNames = listOf("Kotlin", "Jetpack Compose", "Public Speaking"),
      ratingAvg = 4.8,
      reviewCount = 9L,
      reputationPoints = 85L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Community Contributor"),
      isActive = true,
      isOnline = true
    ),
    PublicProfile(
      id = "seed_karthik",
      userId = "seed_karthik",
      displayName = "Karthik Nair",
      bio = "Android engineer & acoustic guitarist. Happy to teach Jetpack Compose and Kotlin in exchange for Photography.",
      location = "Kochi, IN",
      spokenLanguages = listOf("English", "Malayalam", "Tamil"),
      preferredLanguage = "English",
      availability = "Weekends 10 AM - 2 PM",
      experienceYears = 6L,
      teachSkillNames = listOf("Kotlin", "Jetpack Compose", "Guitar"),
      learnSkillNames = listOf("Photography", "Video Editing"),
      ratingAvg = 4.9,
      reviewCount = 11L,
      reputationPoints = 110L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Expert Mentor", "Collaboration Star"),
      isActive = true,
      isOnline = false
    ),
    PublicProfile(
      id = "seed_meera",
      userId = "seed_meera",
      displayName = "Meera Iyer",
      bio = "Tech communicator & Toastmasters coach helping engineers master clear storytelling and documentation.",
      location = "Chennai, IN",
      spokenLanguages = listOf("English", "Tamil", "Hindi"),
      preferredLanguage = "Tamil",
      availability = "Weekdays 6-9 PM",
      experienceYears = 7L,
      teachSkillNames = listOf("Public Speaking", "Technical Writing", "Product Strategy"),
      learnSkillNames = listOf("Python", "SQL", "Photoshop"),
      ratingAvg = 4.7,
      reviewCount = 8L,
      reputationPoints = 70L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Expert Mentor"),
      isActive = true,
      isOnline = true
    ),
    PublicProfile(
      id = "seed_vikram",
      userId = "seed_vikram",
      displayName = "Vikram Gowda",
      bio = "Data analyst fluent in Python and SQL. Excited to trade database tuning for visual storytelling.",
      location = "Mysuru, IN",
      spokenLanguages = listOf("English", "Kannada", "Hindi"),
      preferredLanguage = "Kannada",
      availability = "Weekdays Evening",
      experienceYears = 4L,
      teachSkillNames = listOf("Python", "SQL", "Chess Strategy"),
      learnSkillNames = listOf("Video Editing", "UI/UX Design"),
      ratingAvg = 4.6,
      reviewCount = 6L,
      reputationPoints = 55L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Community Contributor"),
      isActive = true,
      isOnline = true
    ),
    PublicProfile(
      id = "seed_priya",
      userId = "seed_priya",
      displayName = "Priya Sharma",
      bio = "Documentary filmmaker & Premiere Pro editor sharing color grading and storytelling workflows.",
      location = "Pune, IN",
      spokenLanguages = listOf("English", "Hindi"),
      preferredLanguage = "Hindi",
      availability = "Weekends 4-8 PM",
      experienceYears = 5L,
      teachSkillNames = listOf("Video Editing", "Photography", "Photoshop"),
      learnSkillNames = listOf("Java", "React Native", "Financial Literacy"),
      ratingAvg = 4.9,
      reviewCount = 12L,
      reputationPoints = 95L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Expert Mentor"),
      isActive = true,
      isOnline = false
    ),
    PublicProfile(
      id = "seed_arjun",
      userId = "seed_arjun",
      displayName = "Arjun Rao",
      bio = "Backend architect teaching clean microservices, Java, and Spring Boot.",
      location = "Visakhapatnam, IN",
      spokenLanguages = listOf("English", "Telugu"),
      preferredLanguage = "Telugu",
      availability = "Weekdays 7-9 PM",
      experienceYears = 8L,
      teachSkillNames = listOf("Java", "Spring Boot", "Project Management"),
      learnSkillNames = listOf("Guitar", "UI/UX Design"),
      ratingAvg = 4.8,
      reviewCount = 10L,
      reputationPoints = 105L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Expert Mentor", "Collaboration Star"),
      isActive = true,
      isOnline = true
    ),
    PublicProfile(
      id = "seed_divya",
      userId = "seed_divya",
      displayName = "Divya Menon",
      bio = "Carnatic vocalist and South Indian home chef exchanging music & cooking for mobile dev skills.",
      location = "Thiruvananthapuram, IN",
      spokenLanguages = listOf("English", "Malayalam", "Tamil"),
      preferredLanguage = "Malayalam",
      availability = "Weekends Morning",
      experienceYears = 6L,
      teachSkillNames = listOf("Classical Vocal", "Cooking", "Photography"),
      learnSkillNames = listOf("Canva", "Jetpack Compose"),
      ratingAvg = 4.7,
      reviewCount = 7L,
      reputationPoints = 60L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Expert Mentor"),
      isActive = true,
      isOnline = true
    ),
    PublicProfile(
      id = "seed_rohan",
      userId = "seed_rohan",
      displayName = "Rohan Kulkarni",
      bio = "Cross-platform mobile engineer and personal finance educator.",
      location = "Bengaluru, IN",
      spokenLanguages = listOf("English", "Hindi", "Kannada"),
      preferredLanguage = "English",
      availability = "Weekdays 6-9 PM",
      experienceYears = 4L,
      teachSkillNames = listOf("React Native", "Financial Literacy", "SQL"),
      learnSkillNames = listOf("Photoshop", "Public Speaking"),
      ratingAvg = 4.5,
      reviewCount = 5L,
      reputationPoints = 45L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Community Contributor"),
      isActive = true,
      isOnline = false
    ),
    PublicProfile(
      id = "seed_sneha",
      userId = "seed_sneha",
      displayName = "Sneha Deshmukh",
      bio = "Digital illustrator & brand storyteller open to exchanging visual arts for Python automation.",
      location = "Hyderabad, IN",
      spokenLanguages = listOf("English", "Telugu", "Hindi"),
      preferredLanguage = "English",
      availability = "Weekdays Evening",
      experienceYears = 3L,
      teachSkillNames = listOf("Illustration", "Photoshop", "Canva"),
      learnSkillNames = listOf("Python", "Java", "Spring Boot"),
      ratingAvg = 4.8,
      reviewCount = 6L,
      reputationPoints = 50L,
      badges = listOf("Beginner Helper", "Skill Sharer", "Community Contributor"),
      isActive = true,
      isOnline = true
    )
  )

  val communityProjects: List<CollabProject> = listOf(
    CollabProject(
      id = "seed_proj_1",
      ownerId = "seed_ananya",
      ownerName = "Ananya Reddy",
      title = "Multilingual Rural Learning Kit",
      description = "Co-creating illustrated visual flashcards & audio lessons in Telugu, Hindi, and Tamil for community learners.",
      requiredSkills = listOf("Illustration", "UI/UX Design", "Kotlin", "Spoken Telugu"),
      memberIds = listOf("seed_ananya", "seed_meera"),
      memberNames = listOf("Ananya Reddy", "Meera Iyer"),
      deadline = "2026-12-15",
      status = ProjectStatus.OPEN.name
    ),
    CollabProject(
      id = "seed_proj_2",
      ownerId = "seed_rahul",
      ownerName = "Rahul Verma",
      title = "Open Design-to-Code Showcase",
      description = "Pairing Photoshop & motion designers with Java/Kotlin developers to build open-source portfolio templates.",
      requiredSkills = listOf("Photoshop", "Video Editing", "Java", "Spring Boot"),
      memberIds = listOf("seed_rahul", "seed_arjun"),
      memberNames = listOf("Rahul Verma", "Arjun Rao"),
      deadline = "2026-12-30",
      status = ProjectStatus.OPEN.name
    )
  )

  val communityReviews: List<UserReview> = listOf(
    UserReview(
      id = "seed_rev_1",
      reviewerId = "seed_arjun",
      reviewerName = "Arjun Rao",
      reviewedUserId = "seed_rahul",
      reviewedUserName = "Rahul Verma",
      sessionId = "seed_sess_1",
      rating = 5L,
      comment = "Rahul explained Photoshop layer masks and typography hierarchy with crystal clarity!"
    ),
    UserReview(
      id = "seed_rev_2",
      reviewerId = "seed_meera",
      reviewerName = "Meera Iyer",
      reviewedUserId = "seed_ananya",
      reviewedUserName = "Ananya Reddy",
      sessionId = "seed_sess_2",
      rating = 5L,
      comment = "Super patient mentor—helped me turn rough wireframes into a clean component kit."
    )
  )
}
