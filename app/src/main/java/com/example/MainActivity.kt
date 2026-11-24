package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiStudioEngine
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.isFirebaseAvailable
import com.example.data.ProjectRepository
import com.example.firebase.configureAppCheck
import com.example.local.LocalStudioDatabase
import com.example.local.StudioPreferences
import com.example.network.HttpWorkspaceClient
import com.example.ui.ForgeStudioApp
import com.example.ui.theme.ForgeStudioTheme
import com.example.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ForgeStudioTheme(darkTheme = true) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth? = null) {
    val context = LocalContext.current
    val resolvedAuth = remember(context, auth) {
        auth ?: if (isFirebaseAvailable(context)) {
            runCatching { Firebase.auth }.getOrNull()
        } else null
    }

    if (resolvedAuth == null) {
        val localUserId = "local-studio-user"
        key(localUserId) {
            val viewModel: StudioViewModel = viewModel(
                key = localUserId,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        StudioViewModel(
                            repository = ProjectRepository(db = null, auth = null),
                            aiEngine = AiStudioEngine(),
                            localDb = LocalStudioDatabase.getInstance(app),
                            preferences = StudioPreferences(app),
                            httpClient = HttpWorkspaceClient(),
                            currentUserId = localUserId,
                            cloudSyncEnabled = false
                        )
                    }
                }
            )
            ForgeStudioApp(
                viewModel = viewModel,
                currentUserId = localUserId,
                userEmail = "Local Offline Studio Mode",
                isCloudAuthActive = false
            )
        }
        return
    }

    val currentUser by resolvedAuth.authStateFlow().collectAsStateWithLifecycle(initialValue = resolvedAuth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: StudioViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        StudioViewModel(
                            repository = ProjectRepository(db, resolvedAuth),
                            aiEngine = AiStudioEngine(),
                            localDb = LocalStudioDatabase.getInstance(app),
                            preferences = StudioPreferences(app),
                            httpClient = HttpWorkspaceClient(),
                            currentUserId = user.uid,
                            cloudSyncEnabled = true
                        )
                    }
                }
            )
            ForgeStudioApp(
                viewModel = viewModel,
                currentUserId = user.uid,
                userEmail = user.email,
                isCloudAuthActive = true
            )
        }
    }
}
