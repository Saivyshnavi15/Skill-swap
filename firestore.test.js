const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_uid";
const ADMIN_EMAIL = "ksaivyshnavi406@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const serverTs = () => new Date();

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("profiles").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("user_skills").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("exchange_requests").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("notifications").where("userId", "==", ALICE_UID).get());
});

test("UserPrivate PII isolation: owner can write & read, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      appLanguage: "en",
      preferredCommLanguage: "English",
      privacyShowLocation: true,
      privacyAllowInvites: true,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update test (ghost field)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: serverTs(),
    })
  );
});

test("PublicProfile: owner can create/update, active list query succeeds, cross-user update fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sai",
      bio: "Java and Spring mentor",
      location: "Hyderabad",
      spokenLanguages: ["English", "Telugu"],
      preferredLanguage: "English",
      availability: "Weekdays 6-9 PM",
      experienceYears: 4,
      teachSkillNames: ["Java", "Spring Boot"],
      learnSkillNames: ["Photoshop"],
      ratingAvg: 4.8,
      reviewCount: 4,
      reputationPoints: 45,
      badges: ["Skill Sharer"],
      isActive: true,
      isOnline: true,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Query aligned with rule: isActive == true
  await assertSucceeds(bobDb.collection("profiles").where("isActive", "==", true).get());
  // Unfiltered list query fails
  await assertFails(bobDb.collection("profiles").get());
  // Cross-user update fails
  await assertFails(bobDb.collection("profiles").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: serverTs() }));
});

test("Email spoofing test: unverified admin email cannot deactivate another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      isActive: true,
      reputationPoints: 10,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofedAdminDb.collection("profiles").doc(ALICE_UID).update({
      isActive: false,
      updatedAt: serverTs(),
    })
  );

  const verifiedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    verifiedAdminDb.collection("profiles").doc(ALICE_UID).update({
      isActive: false,
      updatedAt: serverTs(),
    })
  );
});

test("UserSkill and LearningGoal: owner-scoped CRUD & query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("user_skills").doc("skill_1").set({
      userId: ALICE_UID,
      name: "Java",
      category: "TECHNICAL",
      proficiency: "EXPERT",
      experienceYears: 5,
      description: "Core Java & concurrency",
      isCustom: false,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("user_skills").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("user_skills").doc("skill_1").get());
  await assertFails(bobDb.collection("user_skills").where("userId", "==", ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("learning_skills").doc("goal_1").set({
      userId: ALICE_UID,
      skillName: "Photoshop",
      currentLevel: "BEGINNER",
      targetLevel: "ADVANCED",
      priority: "HIGH",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
  await assertFails(bobDb.collection("learning_skills").doc("goal_1").get());
});

test("ExchangeRequest: participant isolation and terminal state locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("exchange_requests").doc("req_1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      receiverId: BOB_UID,
      receiverName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      offeredSkills: ["Java"],
      requestedSkills: ["Photoshop"],
      message: "Let's swap skills!",
      status: "PENDING",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    bobDb.collection("exchange_requests").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("exchange_requests").doc("req_1").update({
      status: "REJECTED",
      updatedAt: serverTs(),
    })
  );
  // Terminal state lock: cannot update after REJECTED
  await assertFails(
    aliceDb.collection("exchange_requests").doc("req_1").update({
      status: "ACCEPTED",
      updatedAt: serverTs(),
    })
  );
});

test("Conversations, Messages, Sessions, Projects, Reviews, Notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const deterministicConvId = `${ALICE_UID}_${BOB_UID}`;
  // Non-existent deterministic conversation doc check: participant succeeds, outsider fails
  await assertSucceeds(aliceDb.collection("conversations").doc(deterministicConvId).get());
  await assertFails(charlieDb.collection("conversations").doc(deterministicConvId).get());

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: ["Alice", "Bob"],
      lastMessage: "Hi Bob!",
      lastSenderId: ALICE_UID,
      unreadCount: 1,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").set({
      conversationId: "conv_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID],
      text: "Hi Bob!",
      isRead: false,
      createdAt: serverTs(),
    })
  );

  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").collection("messages")
      .where("participantIds", "array-contains", BOB_UID).get()
  );

  await assertSucceeds(
    aliceDb.collection("sessions").doc("sess_1").set({
      hostId: ALICE_UID,
      hostName: "Alice",
      guestId: BOB_UID,
      guestName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      skillName: "Java",
      dateLabel: "2026-11-15",
      startTime: "18:00",
      endTime: "19:00",
      description: "Streams & Lambdas",
      roomCode: "room_abc",
      status: "SCHEDULED",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("projects").doc("proj_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      title: "Open Mentor Portal",
      description: "Community web app",
      requiredSkills: ["Java", "Photoshop"],
      memberIds: [ALICE_UID],
      memberNames: ["Alice"],
      deadline: "2026-12-20",
      status: "OPEN",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Bob joins OPEN project
  await assertSucceeds(
    bobDb.collection("projects").doc("proj_1").update({
      memberIds: [ALICE_UID, BOB_UID],
      memberNames: ["Alice", "Bob"],
      updatedAt: serverTs(),
    })
  );

  // Review is immutable & cannot review self
  await assertFails(
    aliceDb.collection("reviews").doc("rev_self").set({
      reviewerId: ALICE_UID,
      reviewerName: "Alice",
      reviewedUserId: ALICE_UID,
      reviewedUserName: "Alice",
      rating: 5,
      comment: "Great",
      createdAt: serverTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_1").set({
      reviewerId: ALICE_UID,
      reviewerName: "Alice",
      reviewedUserId: BOB_UID,
      reviewedUserName: "Bob",
      sessionId: "sess_1",
      rating: 5,
      comment: "Helpful session!",
      createdAt: serverTs(),
    })
  );
  await assertFails(aliceDb.collection("reviews").doc("rev_1").delete());

  await assertSucceeds(
    aliceDb.collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      type: "BADGE_EARNED",
      title: "Badge Earned",
      body: "You unlocked Beginner Helper!",
      isRead: false,
      relatedId: "badge_1",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
  await assertFails(bobDb.collection("notifications").doc("notif_1").get());
});
