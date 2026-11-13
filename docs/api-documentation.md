# SkillSwap Data & Service Contracts

## Firestore Collections (Named DB: `@string/firestore_database_id`)

| Collection | Access Scope | Description |
|---|---|---|
| `/users/{userId}` | Owner / Admin | Private preferences (`appLanguage`, `preferredCommLanguage`, privacy toggles) and PII (`email`). |
| `/profiles/{profileId}` | Signed-in read (`isActive == true`), Owner write | Public discoverable skill exchange profile (no PII). |
| `/user_skills/{skillId}` | Owner (`userId == auth.uid`) | Teaching skills with category, proficiency, experience years, and description. |
| `/learning_skills/{goalId}` | Owner (`userId == auth.uid`) | Desired skills with current level, target level, and priority. |
| `/exchange_requests/{requestId}` | Participants (`participantIds`) | Peer-to-peer swap requests (`PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `COMPLETED`). |
| `/conversations/{conversationId}` | Participants (`participantIds`) | Real-time conversation threads with nested `/messages/{messageId}` subcollection. |
| `/sessions/{sessionId}` | Participants (`participantIds`) | Scheduled 1:1 learning and CameraX/WebRTC video sessions. |
| `/projects/{projectId}` | Public `OPEN` / Members / Owner | Collaborative community projects (`OPEN`, `IN_PROGRESS`, `COMPLETED`). |
| `/reviews/{reviewId}` | Reviewer / Reviewed / Admin | Immutable peer ratings (1–5 stars) and reviews. |
| `/notifications/{notificationId}` | Owner (`userId == auth.uid`) | Activity notifications with read/unread state. |
