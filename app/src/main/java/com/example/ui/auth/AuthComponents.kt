package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AuthFlowMode
import com.example.data.AuthOutcome
import com.example.data.AuthService
import kotlinx.coroutines.launch

private data class OnboardingPage(
  val title: String,
  val subtitle: String,
  val badge: String
)

@Composable
fun AuthScreen(
  authService: AuthService = remember { AuthService() },
  onAuthSuccess: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var onboardingStep by remember { mutableIntStateOf(0) }

  val pages = remember {
    listOf(
      OnboardingPage(
        title = "Teach What You Know, Learn What You Love",
        subtitle = "Exchange Java, UI/UX, Video Editing, or Music directly with peers—100% free with zero money or paid courses.",
        badge = "Peer-to-Peer Skill Exchange"
      ),
      OnboardingPage(
        title = "Transparent Smart Matching",
        subtitle = "See exact match percentages and clear explanations based on mutual skills, spoken languages, and availability.",
        badge = "Explainable Matching"
      ),
      OnboardingPage(
        title = "Collaborate Across 6 Languages",
        subtitle = "Chat in real time, schedule live CameraX/WebRTC sessions, co-build community projects, and earn reputation badges.",
        badge = "English • తెలుగు • हिन्दी • தமிழ் • ಕನ್ನಡ • മലയാളം"
      )
    )
  }

  LaunchedEffect(authService) {
    val outcome = authService.attemptAutoSignIn(context, credentialManager)
    if (outcome is AuthOutcome.Success) {
      onAuthSuccess()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
          )
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp)
        .widthIn(max = 560.dp)
        .align(Alignment.TopCenter),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(44.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = "SkillSwap emblem",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "SkillSwap",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Multilingual Community Platform",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_hero_skillswap),
          contentDescription = "Diverse learners exchanging skills on SkillSwap",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      AnimatedContent(targetState = onboardingStep, label = "onboarding_step") { step ->
        val page = pages[step]
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(50)
          ) {
            Text(
              text = page.badge,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        pages.indices.forEach { idx ->
          val active = idx == onboardingStep
          Box(
            modifier = Modifier
              .height(8.dp)
              .width(if (active) 24.dp else 8.dp)
              .clip(CircleShape)
              .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
              )
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        TextButton(
          onClick = { onboardingStep = (onboardingStep + pages.size - 1) % pages.size },
          modifier = Modifier.testTag("onboarding_prev_button")
        ) {
          Text("Previous")
        }
        TextButton(
          onClick = { onboardingStep = (onboardingStep + 1) % pages.size },
          modifier = Modifier.testTag("onboarding_next_button")
        ) {
          Text("Next Highlight")
          Spacer(modifier = Modifier.width(4.dp))
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("No payments, wallets, or fees — purely knowledge exchange", style = MaterialTheme.typography.bodySmall)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("10+ active community mentors ready to match right away", style = MaterialTheme.typography.bodySmall)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Multilingual UI across 6 Indian & global languages", style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      Button(
        onClick = {
          errorMessage = null
          isLoading = true
          scope.launch {
            when (val outcome = authService.signInWithGoogle(context, credentialManager, AuthFlowMode.SIGN_IN)) {
              is AuthOutcome.Success -> {
                isLoading = false
                onAuthSuccess()
              }
              AuthOutcome.Cancelled -> {
                isLoading = false
              }
              is AuthOutcome.Error -> {
                isLoading = false
                errorMessage = outcome.message
              }
            }
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("google_sign_in_button")
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text("Signing in...", fontWeight = FontWeight.SemiBold)
        } else {
          Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButton(
        onClick = {
          errorMessage = null
          isLoading = true
          scope.launch {
            when (val outcome = authService.signUpWithGoogle(context, credentialManager)) {
              is AuthOutcome.Success -> {
                isLoading = false
                onAuthSuccess()
              }
              AuthOutcome.Cancelled -> {
                isLoading = false
              }
              is AuthOutcome.Error -> {
                isLoading = false
                errorMessage = outcome.message
              }
            }
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("create_account_google_button")
      ) {
        Text("New to SkillSwap? Register with Google")
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
