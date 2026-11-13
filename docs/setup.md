# SkillSwap Setup & Developer Guide

## 1. Android Build & Run

1. Open the project in Android Studio or run via Gradle:
   ```bash
   gradle :app:assembleDebug
   ```
2. Run local unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

## 2. Firebase Configuration

- **Provisioned Project**: Configured automatically via `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`.
- **Database ID**: Always resolved from `@string/firestore_database_id`.
- **Authentication**: Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption`).

## 3. Security Rules Testing (Local Emulator)

```bash
# Start emulators (Firestore on 8085, Auth on 9099)
firebase emulators:start --only firestore,auth --project demo-no-project

# Run JS rules tests
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

## 4. Native WebRTC Peer Signaling (Optional Extension)

Local camera video (`CameraX` `PreviewView`) and microphone/camera permission flows (`CAMERA`, `RECORD_AUDIO`) are implemented in `VideoSessionService` / `LocalCameraWebRtcSessionService`. To enable live multi-device peer-to-peer video streaming:

1. Add a WebRTC Android artifact (such as `io.getstream:stream-webrtc-android`) in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
2. Add a `sessions/{sessionId}/signaling` Firestore subcollection for SDP offers, answers, and ICE candidates.
3. Implement `PeerConnectionFactory` inside `VideoSessionService`.
