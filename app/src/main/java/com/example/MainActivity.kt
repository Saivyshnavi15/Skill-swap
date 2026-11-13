package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AuthService
import com.example.data.SkillSwapRepository
import com.example.data.UserPreferencesDataStore
import com.example.domain.TranslationService
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.components.BottomNavTab
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearningGoalsScreen
import com.example.ui.screens.MatchDetailsScreen
import com.example.ui.screens.MatchesScreen
import com.example.ui.screens.MySkillsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.ReputationBadgesReviewsScreen
import com.example.ui.screens.RequestsScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.screens.SettingsLanguageAdminScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.theme.SkillSwapTheme
import com.example.ui.viewmodel.SkillSwapViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SkillSwapTheme {
        SkillSwapRoot()
      }
    }
  }
}

@Composable
fun SkillSwapRoot(auth: FirebaseAuth = Firebase.auth) {
  val authService = remember(auth) { AuthService(auth) }
  val currentUser by authService.observeAuthState()
    .collectAsStateWithLifecycle(initialValue = authService.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(
      authService = authService,
      onAuthSuccess = {}
    )
  } else {
    key(user.uid) {
      AuthenticatedSkillSwapShell(
        authService = authService,
        currentUserId = user.uid,
        currentUserEmail = user.email,
        currentUserDisplayName = user.displayName
      )
    }
  }
}

@Composable
fun AuthenticatedSkillSwapShell(
  authService: AuthService,
  currentUserId: String,
  currentUserEmail: String?,
  currentUserDisplayName: String?
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: SkillSwapViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) as Application
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        SkillSwapViewModel(
          repository = SkillSwapRepository(db, Firebase.auth),
          prefs = UserPreferencesDataStore(app),
          currentUserId = currentUserId,
          currentUserEmail = currentUserEmail,
          currentUserDisplayName = currentUserDisplayName
        )
      }
    }
  )

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavTab.HOME.route

  val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
  val profileState by viewModel.myProfileState.collectAsStateWithLifecycle()
  val myPrivateUser by viewModel.myPrivateUser.collectAsStateWithLifecycle()
  val mySkills by viewModel.mySkills.collectAsStateWithLifecycle()
  val myGoals by viewModel.myLearningGoals.collectAsStateWithLifecycle()
  val matches by viewModel.matches.collectAsStateWithLifecycle()
  val exploreMatches by viewModel.filteredExploreMatches.collectAsStateWithLifecycle()
  val exploreFilter by viewModel.exploreFilter.collectAsStateWithLifecycle()
  val requests by viewModel.exchangeRequests.collectAsStateWithLifecycle()
  val conversations by viewModel.conversations.collectAsStateWithLifecycle()
  val sessions by viewModel.sessions.collectAsStateWithLifecycle()
  val projects by viewModel.projects.collectAsStateWithLifecycle()
  val receivedReviews by viewModel.myReceivedReviews.collectAsStateWithLifecycle()
  val authoredReviews by viewModel.myAuthoredReviews.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val communityProfiles by viewModel.communityProfiles.collectAsStateWithLifecycle()
  val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedback) {
    if (feedback != null) {
      snackbarHostState.showSnackbar(feedback!!)
      viewModel.clearFeedback()
    }
  }

  val myProfile = (profileState as? UiState.Success)?.data
    ?: PublicProfile(
      id = currentUserId,
      userId = currentUserId,
      displayName = currentUserDisplayName ?: "SkillSwap Learner"
    )

  val t: (String) -> String = { key -> TranslationService.t(appLanguage, key) }

  val topLevelRoutes = remember { BottomNavTab.entries.map { it.route }.toSet() }
  val showBottomOrRailNav = currentRoute in topLevelRoutes
  val isExpandedWidth = LocalConfiguration.current.screenWidthDp >= 840

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      if (showBottomOrRailNav && !isExpandedWidth) {
        NavigationBar {
          BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
              selected = selected,
              onClick = {
                navController.navigate(tab.route) {
                  popUpTo(BottomNavTab.HOME.route) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = t(tab.labelKey)
                )
              },
              label = { Text(t(tab.labelKey)) },
              modifier = Modifier.testTag("bottom_nav_${tab.route}")
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (showBottomOrRailNav && isExpandedWidth) {
        NavigationRail {
          BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationRailItem(
              selected = selected,
              onClick = {
                navController.navigate(tab.route) {
                  popUpTo(BottomNavTab.HOME.route) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = t(tab.labelKey)
                )
              },
              label = { Text(t(tab.labelKey)) }
            )
          }
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxSize()) {
        NavHost(
          navController = navController,
          startDestination = BottomNavTab.HOME.route
        ) {
          composable(BottomNavTab.HOME.route) {
            HomeScreen(
              profile = myProfile,
              mySkills = mySkills,
              myGoals = myGoals,
              matches = matches,
              requests = requests,
              sessions = sessions,
              notifications = notifications,
              appLanguage = appLanguage,
              t = t,
              onOpenNotifications = { navController.navigate("notifications") },
              onOpenChatList = { navController.navigate("chat_list") },
              onOpenLanguageSettings = { navController.navigate("settings") },
              onOpenAddSkill = { navController.navigate("my_skills") },
              onOpenLearningGoals = { navController.navigate("learning_goals") },
              onFindMatch = { navController.navigate(BottomNavTab.MATCHES.route) },
              onOpenMatchDetails = { uid -> navController.navigate("match_details/$uid") },
              onOpenProjects = { navController.navigate("projects") },
              onOpenRequests = { navController.navigate(BottomNavTab.REQUESTS.route) },
              onOpenSessions = { navController.navigate("sessions") },
              onOpenReputation = { navController.navigate("reputation") }
            )
          }

          composable(BottomNavTab.EXPLORE.route) {
            ExploreScreen(
              filter = exploreFilter,
              matches = exploreMatches,
              t = t,
              onUpdateFilter = viewModel::updateExploreFilter,
              onViewProfile = { uid -> navController.navigate("match_details/$uid") }
            )
          }

          composable(BottomNavTab.MATCHES.route) {
            MatchesScreen(
              matches = matches,
              onOpenMatchDetails = { uid -> navController.navigate("match_details/$uid") }
            )
          }

          composable(BottomNavTab.REQUESTS.route) {
            RequestsScreen(
              currentUserId = currentUserId,
              requests = requests,
              onUpdateStatus = viewModel::updateExchangeStatus,
              onOpenExplore = { navController.navigate(BottomNavTab.EXPLORE.route) }
            )
          }

          composable(BottomNavTab.PROFILE.route) {
            ProfileScreen(
              profile = myProfile,
              mySkills = mySkills,
              myGoals = myGoals,
              isAdminUser = viewModel.isAdminUser,
              onSaveProfile = viewModel::updateProfile,
              onOpenMySkills = { navController.navigate("my_skills") },
              onOpenLearningGoals = { navController.navigate("learning_goals") },
              onOpenSessions = { navController.navigate("sessions") },
              onOpenProjects = { navController.navigate("projects") },
              onOpenReputation = { navController.navigate("reputation") },
              onOpenSettings = { navController.navigate("settings") },
              onOpenAdmin = { navController.navigate("settings") }
            )
          }

          composable("my_skills") {
            MySkillsScreen(
              skills = mySkills,
              onBack = { navController.popBackStack() },
              onAddSkill = viewModel::addSkill,
              onUpdateSkill = viewModel::updateSkill,
              onDeleteSkill = viewModel::deleteSkill
            )
          }

          composable("learning_goals") {
            LearningGoalsScreen(
              goals = myGoals,
              onBack = { navController.popBackStack() },
              onAddGoal = viewModel::addLearningGoal,
              onDeleteGoal = viewModel::deleteLearningGoal
            )
          }

          composable(
            route = "match_details/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
          ) { backStackEntry ->
            val targetId = backStackEntry.arguments?.getString("userId").orEmpty()
            val match = matches.firstOrNull { it.peerProfile.userId == targetId }
            if (match != null) {
              MatchDetailsScreen(
                match = match,
                myProfile = myProfile,
                onBack = { navController.popBackStack() },
                onSendRequest = { offered, requested, msg ->
                  viewModel.sendExchangeRequest(match.peerProfile, offered, requested, msg)
                },
                onStartChat = {
                  viewModel.openOrStartChat(match.peerProfile) { convId ->
                    navController.navigate("chat/$convId")
                  }
                },
                onScheduleSession = { skill, date, start, end, desc ->
                  viewModel.scheduleSession(match.peerProfile, skill, date, start, end, desc)
                },
                onSubmitReview = { rating, comment ->
                  viewModel.submitReview(
                    reviewedUserId = match.peerProfile.userId,
                    reviewedUserName = match.peerProfile.displayName,
                    sessionId = "direct_exchange",
                    rating = rating,
                    comment = comment
                  )
                }
              )
            }
          }

          composable("chat_list") {
            ChatListScreen(
              currentUserId = currentUserId,
              conversations = conversations,
              onBack = { navController.popBackStack() },
              onOpenChat = { convId -> navController.navigate("chat/$convId") },
              onOpenExplore = { navController.navigate(BottomNavTab.EXPLORE.route) }
            )
          }

          composable(
            route = "chat/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
          ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getString("conversationId").orEmpty()
            val conv = conversations.firstOrNull { it.id == convId }
            val msgs by viewModel.observeMessages(convId).collectAsStateWithLifecycle(initialValue = emptyList())
            if (conv != null) {
              ChatScreen(
                currentUserId = currentUserId,
                conversation = conv,
                messages = msgs,
                onBack = { navController.popBackStack() },
                onSendMessage = { text -> viewModel.sendMessage(conv, text) }
              )
            }
          }

          composable("sessions") {
            SessionsScreen(
              currentUserId = currentUserId,
              sessions = sessions,
              onBack = { navController.popBackStack() },
              onJoinVideo = { sess -> navController.navigate("video/${sess.id}") },
              onUpdateStatus = viewModel::updateSessionStatus
            )
          }

          composable(
            route = "video/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
          ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            val sess = sessions.firstOrNull { it.id == sessionId }
            if (sess != null) {
              VideoCallScreen(
                session = sess,
                currentUserId = currentUserId,
                onLeaveSession = { navController.popBackStack() }
              )
            }
          }

          composable("projects") {
            ProjectsScreen(
              currentUserId = currentUserId,
              projects = projects,
              onBack = { navController.popBackStack() },
              onCreateProject = viewModel::createProject,
              onJoinProject = viewModel::joinProject,
              onLeaveProject = viewModel::leaveProject,
              onCompleteProject = viewModel::completeProject
            )
          }

          composable("reputation") {
            ReputationBadgesReviewsScreen(
              profile = myProfile,
              receivedReviews = receivedReviews,
              authoredReviews = authoredReviews,
              onBack = { navController.popBackStack() }
            )
          }

          composable("notifications") {
            NotificationsScreen(
              notifications = notifications,
              onBack = { navController.popBackStack() },
              onMarkRead = viewModel::markNotificationRead,
              onMarkAllRead = viewModel::markAllNotificationsRead
            )
          }

          composable("settings") {
            SettingsLanguageAdminScreen(
              appLanguage = appLanguage,
              privateUser = myPrivateUser,
              isAdminUser = viewModel.isAdminUser,
              communityProfiles = communityProfiles,
              onBack = { navController.popBackStack() },
              onSavePreferences = { lang, commLang, showLoc, allowInv ->
                viewModel.setLanguages(lang, commLang, showLoc, allowInv)
              },
              onAdminToggleActive = viewModel::adminToggleProfileActive,
              onSignOut = {
                scope.launch {
                  authService.signOut(credentialManager = credentialManager)
                }
              }
            )
          }
        }
      }
    }
  }
}
