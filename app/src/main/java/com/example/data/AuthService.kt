package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class AuthFlowMode(val label: String) {
  SIGN_IN("Sign-In"),
  SIGN_UP("Sign-Up")
}

sealed interface AuthSessionState {
  data object Unauthenticated : AuthSessionState
  data class Authenticated(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
  ) : AuthSessionState
}

sealed interface AuthOutcome {
  data class Success(
    val user: FirebaseUser,
    val isNewUser: Boolean
  ) : AuthOutcome

  data object Cancelled : AuthOutcome

  data class Error(
    val message: String,
    val cause: Throwable? = null
  ) : AuthOutcome
}

/**
 * Firebase Authentication service layer backed by Jetpack Credential Manager (Google Sign-In)
 * and FirebaseAuth. Handles interactive login/sign-up, silent credential restoration,
 * reactive session state streams, and non-cancellable sign-out.
 */
class AuthService(
  private val auth: FirebaseAuth = Firebase.auth
) {

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  fun observeSessionState(): Flow<AuthSessionState> = observeAuthState().map { user ->
    if (user == null) {
      AuthSessionState.Unauthenticated
    } else {
      AuthSessionState.Authenticated(
        uid = user.uid,
        email = user.email,
        displayName = user.displayName,
        photoUrl = user.photoUrl?.toString()
      )
    }
  }

  /**
   * Attempts silent sign-in on startup using previously authorized Google credentials.
   * Uses a dedicated single-option GetCredentialRequest with GetGoogleIdOption.
   */
  suspend fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context.applicationContext)
  ): AuthOutcome {
    val existing = auth.currentUser
    if (existing != null) {
      return AuthOutcome.Success(user = existing, isNewUser = false)
    }

    val clientId = resolveWebClientId(context)
      ?: return AuthOutcome.Cancelled

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    return try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        signInWithGoogleIdToken(googleIdToken, AuthFlowMode.SIGN_IN)
      } else {
        AuthOutcome.Cancelled
      }
    } catch (e: NoCredentialException) {
      Log.d(TAG, "No authorized Google credential found for silent sign-in: ${e.message}")
      AuthOutcome.Cancelled
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Silent Google Sign-In cancelled: ${e.message}", e)
      AuthOutcome.Cancelled
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Silent Google Sign-In skipped: ${e.message}", e)
      AuthOutcome.Cancelled
    }
  }

  /**
   * Interactive Google Sign-In / Sign-Up flow using Jetpack Credential Manager.
   * Supplies a single GetSignInWithGoogleOption in GetCredentialRequest so the bottom
   * sheet remains responsive across Android 14-16 devices.
   */
  suspend fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context.applicationContext),
    mode: AuthFlowMode = AuthFlowMode.SIGN_IN
  ): AuthOutcome {
    val clientId = resolveWebClientId(context)
      ?: return AuthOutcome.Error(
        "Google Sign-In configuration missing: default_web_client_id not found"
      )

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    val targetContext: Context = (context as? Activity) ?: context

    return try {
      val result = credentialManager.getCredential(targetContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        signInWithGoogleIdToken(googleIdToken, mode)
      } else {
        AuthOutcome.Error("Unexpected credential type returned by Credential Manager")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google ${mode.label} flow cancelled or dismissed: ${e.message}", e)
      AuthOutcome.Cancelled
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Google ${mode.label} failed", e)
      AuthOutcome.Error(
        message = e.localizedMessage ?: "Google ${mode.label} failed",
        cause = e
      )
    }
  }

  /**
   * Interactive Google Sign-Up workflow for new SkillSwap members.
   */
  suspend fun signUpWithGoogle(
    context: Context,
    credentialManager: CredentialManager = CredentialManager.create(context.applicationContext)
  ): AuthOutcome = signInWithGoogle(
    context = context,
    credentialManager = credentialManager,
    mode = AuthFlowMode.SIGN_UP
  )

  /**
   * Exchanges a Google ID token for a FirebaseUser session.
   */
  suspend fun signInWithGoogleIdToken(
    idToken: String,
    mode: AuthFlowMode = AuthFlowMode.SIGN_IN
  ): AuthOutcome {
    if (idToken.isBlank()) {
      return AuthOutcome.Error("Google ID token cannot be blank")
    }
    return try {
      val authCredential = GoogleAuthProvider.getCredential(idToken, null)
      val firebaseResult = auth.signInWithCredential(authCredential).await()
      val user = firebaseResult.user
      if (user != null) {
        val isNewUser = firebaseResult.additionalUserInfo?.isNewUser == true
        AuthOutcome.Success(user = user, isNewUser = isNewUser)
      } else {
        AuthOutcome.Error("Firebase ${mode.label} completed without an active user")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase credential exchange failed during ${mode.label}", e)
      AuthOutcome.Error(
        message = e.localizedMessage ?: "Authentication failed",
        cause = e
      )
    }
  }

  /**
   * Clears Credential Manager state and signs out of FirebaseAuth inside NonCancellable
   * so unmounting the calling Composable does not interrupt credential cleanup.
   */
  suspend fun signOut(credentialManager: CredentialManager? = null) {
    withContext(NonCancellable) {
      try {
        credentialManager?.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        auth.signOut()
      }
    }
  }

  private fun resolveWebClientId(context: Context): String? = try {
    context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
  } catch (e: Exception) {
    Log.w(TAG, "Missing default_web_client_id string resource", e)
    null
  }

  private companion object {
    const val TAG = "AuthService"
  }
}
