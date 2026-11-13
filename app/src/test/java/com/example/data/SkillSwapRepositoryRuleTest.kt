package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.ExchangeStatus
import com.example.model.GoalPriority
import com.example.model.Proficiency
import com.example.model.SessionStatus
import com.example.model.SkillCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillSwapRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun initializeAndAddSkill_authenticatedOwner_succeedsAndEmitsRealtime() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = SkillSwapRepository(firestore, auth)

    val initResult = withTimeout(TIMEOUT_MS) { repo.ensureUserInitialized("Sai", ALICE_EMAIL) }
    assertTrue("Init failed: ${initResult.exceptionOrNull()}", initResult.isSuccess)

    val addResult = withTimeout(TIMEOUT_MS) {
      repo.addSkill(
        name = "Java",
        category = SkillCategory.TECHNICAL.name,
        proficiency = Proficiency.EXPERT.name,
        experienceYears = 4L,
        description = "Spring Boot & concurrency",
        isCustom = false
      )
    }
    assertTrue("Add skill failed: ${addResult.exceptionOrNull()}", addResult.isSuccess)
    val skillId = addResult.getOrThrow()

    val skills = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMySkills().first { list -> list.any { it.id == skillId } }
    }
    assertTrue(skills.any { it.name == "Java" })

    val goalResult = withTimeout(TIMEOUT_MS) {
      repo.addLearningGoal("Photoshop", Proficiency.BEGINNER.name, Proficiency.ADVANCED.name, GoalPriority.HIGH.name)
    }
    assertTrue("Add goal failed: ${goalResult.exceptionOrNull()}", goalResult.isSuccess)
  }

  @Test
  fun exchangeRequestSessionProjectReview_participantFlow_succeedsAndBlocksOutsiders() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = SkillSwapRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) { aliceRepo.ensureUserInitialized("Alice", ALICE_EMAIL).getOrThrow() }

    val bobUid = signInTestUser(BOB_EMAIL)
    val bobRepo = SkillSwapRepository(firestore, auth)
    withTimeout(TIMEOUT_MS) { bobRepo.ensureUserInitialized("Bob", BOB_EMAIL).getOrThrow() }

    signInTestUser(ALICE_EMAIL)
    val reqResult = withTimeout(TIMEOUT_MS) {
      aliceRepo.sendExchangeRequest(
        senderName = "Alice",
        receiverId = bobUid,
        receiverName = "Bob",
        offeredSkills = listOf("Java"),
        requestedSkills = listOf("Photoshop"),
        message = "Let's swap!"
      )
    }
    assertTrue("Send request failed: ${reqResult.exceptionOrNull()}", reqResult.isSuccess)
    val reqId = reqResult.getOrThrow()

    val sessResult = withTimeout(TIMEOUT_MS) {
      aliceRepo.scheduleSession(
        hostName = "Alice",
        guestId = bobUid,
        guestName = "Bob",
        skillName = "Java",
        dateLabel = "2026-11-20",
        startTime = "18:00",
        endTime = "19:00",
        description = "Core Java session"
      )
    }
    assertTrue("Schedule session failed: ${sessResult.exceptionOrNull()}", sessResult.isSuccess)
    val sessId = sessResult.getOrThrow()

    val projResult = withTimeout(TIMEOUT_MS) {
      aliceRepo.createProject(
        ownerName = "Alice",
        title = "SkillSwap Starter Kit",
        description = "Open community starter repository",
        requiredSkills = listOf("Java", "Photoshop"),
        deadline = "2026-12-31"
      )
    }
    assertTrue("Create project failed: ${projResult.exceptionOrNull()}", projResult.isSuccess)

    signInTestUser(BOB_EMAIL)
    val bobReqs = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeMyExchangeRequests().first { list -> list.any { it.id == reqId } }
    }
    assertEquals(1, bobReqs.count { it.id == reqId })
    assertTrue(withTimeout(TIMEOUT_MS) { bobRepo.updateExchangeStatus(reqId, ExchangeStatus.ACCEPTED) }.isSuccess)
    assertTrue(withTimeout(TIMEOUT_MS) { bobRepo.updateSessionStatus(sessId, SessionStatus.COMPLETED) }.isSuccess)

    val revResult = withTimeout(TIMEOUT_MS) {
      bobRepo.submitReview(
        reviewerName = "Bob",
        reviewedUserId = bobReqs.first { it.id == reqId }.senderId,
        reviewedUserName = "Alice",
        sessionId = sessId,
        rating = 5L,
        comment = "Awesome mentor!"
      )
    }
    assertTrue("Submit review failed: ${revResult.exceptionOrNull()}", revResult.isSuccess)

    signInTestUser(CHARLIE_EMAIL)
    val charlieRepo = SkillSwapRepository(firestore, auth)
    val charlieReqs = withTimeout(FLOW_TIMEOUT_MS) {
      charlieRepo.observeMyExchangeRequests().first()
    }
    assertTrue(charlieReqs.none { it.id == reqId })
  }

  @Test
  fun unauthenticatedObserver_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val exception = try {
      withTimeout(FLOW_TIMEOUT_MS) {
        firestore.collection("user_skills").whereEqualTo("userId", "alice").get().awaitForTest()
      }
      null
    } catch (e: Exception) {
      e
    }
    assertNotNull(exception)
    val firestoreException = exception!!.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException, got $exception", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
  }

  private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitForTest(): T {
    return com.google.android.gms.tasks.Tasks.await(this)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_skillswap@test.com"
    const val BOB_EMAIL = "bob_skillswap@test.com"
    const val CHARLIE_EMAIL = "charlie_skillswap@test.com"
    const val TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
