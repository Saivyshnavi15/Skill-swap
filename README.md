# SkillSwap

**A Multilingual Skill Exchange, Collaboration, and Community Platform**

SkillSwap is an Android application built with Kotlin, Jetpack Compose, Material Design 3, and Firebase (Cloud Firestore + Google Sign-In Authentication). Users can teach skills they know, learn skills they want, discover compatible peers through an explainable matching engine, exchange requests, chat in real time, schedule learning and CameraX/WebRTC video sessions, collaborate on projects, earn reputation points & badges, and switch UI languages across **English, Telugu, Hindi, Tamil, Kannada, and Malayalam**.

There is **no money system** — zero payments, wallets, or paid courses.

## Core Features

- **Google Sign-In & Onboarding**: Interactive onboarding carousel and Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption`).
- **Home Dashboard**: Personalized welcome, profile completion meter, quick actions, skills & learning goals summary, top recommended match spotlight, upcoming sessions, pending requests, reputation points, and badges.
- **Skill & Learning Goal Management**: Add, edit, and delete teaching skills across 5 categories (`TECHNICAL`, `CREATIVE`, `PROFESSIONAL`, `HOBBY`, `PRACTICAL`) with 4 proficiency levels (`BEGINNER`–`EXPERT`) and custom skill support, plus learning goals with current/target levels and priority.
- **Explainable Smart Matching**: `MatchingServiceImpl` ranks peers transparently across mutual skill swap overlap (60 pts), common spoken languages (20 pts), availability overlap (10 pts), and trust/experience (10 pts), showing exact reasons and breakdowns.
- **Explore & Filters**: Real-time search across people and skills with Category, Language, Rating (`4.7★+`), and Availability filters.
- **Exchange Requests & Real-Time Chat**: Send, accept, reject, cancel, and complete peer exchange requests, with live Firestore-synced conversations and messages.
- **Sessions & Video Room Architecture**: Schedule 1:1 sessions, track Upcoming/Past sessions, and join live video rooms powered by `VideoSessionService` (real CameraX preview + runtime `CAMERA`/`RECORD_AUDIO` permissions + mute/camera/flip controls, structured for native WebRTC ICE/SDP peer transport).
- **Project Collaboration**: Create, join, leave, and complete community projects with required skills and member lists.
- **Reputation, Badges & Reviews**: Earn points for completing exchanges (+10), helping peers (+5), and completing projects (+10), unlocking 6 badges (`Beginner Helper` through `Collaboration Star`).
- **Multilingual Support**: Instant switching across English (`en`), Telugu (`te`), Hindi (`hi`), Tamil (`ta`), Kannada (`kn`), and Malayalam (`ml`) via `TranslationService` and DataStore.
- **Future AI Interfaces**: Modular `AIRecommendationService`, `SkillGapService`, `AITranslationService`, and `AIProfileService` interfaces ready for future integration.

## Setup & Verification

1. **Firebase Backend**: Configured via `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` using the named Firestore database ID (`R.string.firestore_database_id`).
2. **Security Rules**: Hardened zero-trust rules in `firestore.rules` with schema validation (`firebase-blueprint.json`), PII isolation (`users/{uid}` vs `profiles/{uid}`), and participant-scoped queries.
3. **WebRTC Peer Signaling Note**: Local camera hardware preview and audio permissions are active via CameraX (`VideoSessionService`). Connecting remote video streams across devices requires adding a native WebRTC Android library (e.g. `io.getstream:stream-webrtc-android`) and wiring SDP/ICE candidate exchange over Firestore signaling documents.
