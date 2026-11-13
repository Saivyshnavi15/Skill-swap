package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun observeSessionState_tracksSignInAndSignOut() = runBlocking {
    val authService = AuthService(auth)

    auth.signOut()
    val initial = withTimeout(TIMEOUT_MS) {
      authService.observeSessionState().first()
    }
    assertEquals(AuthSessionState.Unauthenticated, initial)

    val uid = signInTestUser("auth_service_user@test.com")
    val authenticated = withTimeout(TIMEOUT_MS) {
      authService.observeSessionState().first { it is AuthSessionState.Authenticated }
    } as AuthSessionState.Authenticated
    assertEquals(uid, authenticated.uid)
    assertEquals("auth_service_user@test.com", authenticated.email)

    authService.signOut()
    val signedOut = withTimeout(TIMEOUT_MS) {
      authService.observeSessionState().first { it is AuthSessionState.Unauthenticated }
    }
    assertEquals(AuthSessionState.Unauthenticated, signedOut)
    assertNull(authService.currentUser)
  }

  @Test
  fun signInWithGoogleIdToken_blankToken_returnsError() = runBlocking {
    val authService = AuthService(auth)
    val outcome = authService.signInWithGoogleIdToken("   ", AuthFlowMode.SIGN_UP)
    assertTrue(outcome is AuthOutcome.Error)
  }

  private companion object {
    const val TIMEOUT_MS = 5000L
  }
}
