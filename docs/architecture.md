# SkillSwap Android Architecture

## Layers

- **UI Layer (`com.example.ui.*`)**: Jetpack Compose + Material Design 3 (`SkillSwapTheme`, `Plus Jakarta Sans` + `Inter` bundled fonts), adaptive `NavigationBar` (compact) / `NavigationRail` (expanded), Navigation Compose routes, and `SkillSwapViewModel` (`StateFlow` + `collectAsStateWithLifecycle`).
- **Domain Layer (`com.example.domain.*`)**:
  - `MatchingService` / `MatchingServiceImpl`: Transparent weighted scoring across skills, languages, availability, and rating.
  - `TranslationService`: Dictionaries for `en`, `te`, `hi`, `ta`, `kn`, `ml`.
  - `VideoSessionService` / `LocalCameraWebRtcSessionService`: CameraX local hardware preview + WebRTC signaling state contract.
  - `FutureAiServices.kt`: Architectural interfaces for future AI expansion.
- **Data Layer (`com.example.data.*`)**:
  - `SkillSwapRepository`: Real-time Firestore `snapshots()` flows, `FieldValue.serverTimestamp()`, `ServerTimestampBehavior.ESTIMATE`, and structured `handleFirestoreError()` JSON logging.
  - `UserPreferencesDataStore`: Local persistence for language and onboarding state.
  - `SeedCommunityCatalog`: 10 community mentor profiles, 24 skills, sample projects, and reviews.
