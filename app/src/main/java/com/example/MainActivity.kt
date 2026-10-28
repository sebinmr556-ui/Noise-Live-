package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.AppRepository
import com.example.ui.components.LoginRequiredDialog
import com.example.ui.navigation.MainDestination
import com.example.ui.screens.*
import com.example.ui.theme.DarkAccent
import com.example.ui.theme.MyApplicationTheme
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val repository = remember { AppRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by repository.currentUser.collectAsState()
    val isUserAuthenticated by repository.isUserAuthenticated.collectAsState()
    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var isMessagesOpen by remember { mutableStateOf(false) }
    var showGoogleSignInPage by remember(isUserAuthenticated) { mutableStateOf(!isUserAuthenticated) }

    // Authentication Gating & Dialog State
    val authPromptAction by repository.authPromptRequest.collectAsState()
    var isAuthLoading by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    val launchGoogleSignIn: () -> Unit = {
        isAuthLoading = true
        authErrorMessage = null

        coroutineScope.launch {
            try {
                val serverClientId = context.getString(R.string.default_web_client_id)
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val activity = context as? android.app.Activity
                    ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
                    ?: context as android.app.Activity

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    repository.signInWithGoogleIdToken(idToken) { success, err ->
                        isAuthLoading = false
                        if (success) {
                            repository.clearAuthPrompt()
                            showGoogleSignInPage = false
                        } else {
                            authErrorMessage = err ?: "Google authentication failed"
                        }
                    }
                } else {
                    isAuthLoading = false
                    authErrorMessage = "Received unsupported credential type"
                }
            } catch (e: GetCredentialCancellationException) {
                isAuthLoading = false
                android.util.Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                authErrorMessage = "Sign-in was cancelled. Please choose your Google account to continue."
            } catch (e: Exception) {
                isAuthLoading = false
                android.util.Log.e("Auth", "Google Sign-In failed", e)
                val msg = e.localizedMessage ?: "Sign-in encountered an error"
                authErrorMessage = if (msg.contains("NoCredentialException") || msg.contains("no credentials", ignoreCase = true)) {
                    "No Google accounts found on this device. Please add a Google account in Android Settings or use Explore Mode."
                } else {
                    msg
                }
            }
        }
    }

    // Handle system back navigation
    BackHandler(enabled = showGoogleSignInPage || isMessagesOpen || currentDestination != MainDestination.HOME) {
        when {
            showGoogleSignInPage -> showGoogleSignInPage = false
            isMessagesOpen -> isMessagesOpen = false
            currentDestination != MainDestination.HOME -> currentDestination = MainDestination.HOME
        }
    }

    if (showGoogleSignInPage) {
        GoogleSignInScreen(
            currentUser = currentUser,
            isLoading = isAuthLoading,
            errorMessage = authErrorMessage,
            onContinueWithGoogle = launchGoogleSignIn,
            onContinueSavedAccount = {
                authErrorMessage = null
                showGoogleSignInPage = false
            },
            onBrowsePublicFeed = {
                authErrorMessage = null
                showGoogleSignInPage = false
            }
        )
        return
    }

    val showBottomBar = !isMessagesOpen && currentDestination != MainDestination.POST

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("main_bottom_nav_bar")
                    ) {
                        MainDestination.entries.forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = Color.White.copy(alpha = 0.65f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.testTag("nav_${destination.route}")
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            if (isMessagesOpen) {
                DirectMessagesScreen(
                    repository = repository,
                    onBack = { isMessagesOpen = false }
                )
            } else {
                when (currentDestination) {
                    MainDestination.HOME -> {
                        HomeScreen(
                            repository = repository,
                            onNavigateToMessages = { isMessagesOpen = true },
                            onNavigateToCreate = { currentDestination = MainDestination.POST },
                            onNavigateToActivity = { currentDestination = MainDestination.ACTIVITY },
                            onNavigateToProfile = { currentDestination = MainDestination.PROFILE },
                            onOpenGoogleSignIn = {
                                authErrorMessage = null
                                showGoogleSignInPage = true
                            }
                        )
                    }
                    MainDestination.SEARCH -> {
                        ExploreScreen(repository = repository)
                    }
                    MainDestination.POST -> {
                        CreatePostScreen(
                            repository = repository,
                            onPostCreated = { currentDestination = MainDestination.HOME }
                        )
                    }
                    MainDestination.ACTIVITY -> {
                        ActivityScreen(repository = repository)
                    }
                    MainDestination.PROFILE -> {
                        ProfileScreen(
                            repository = repository,
                            onOpenGoogleSignIn = {
                                authErrorMessage = null
                                showGoogleSignInPage = true
                            }
                        )
                    }
                }
            }

            // Instagram-style Auth Gating Dialog
            if (authPromptAction != null) {
                LoginRequiredDialog(
                    actionTitle = "Sign in to ${authPromptAction ?: "continue"}",
                    actionDescription = "Sign in with Google to like, comment, publish posts, or speak in voice chatrooms.",
                    isLoading = isAuthLoading,
                    errorMessage = authErrorMessage,
                    onDismiss = {
                        repository.clearAuthPrompt()
                        authErrorMessage = null
                    },
                    onContinueWithGoogle = launchGoogleSignIn
                )
            }
        }
    }
}
