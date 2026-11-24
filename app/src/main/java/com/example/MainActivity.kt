package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AiStudioEngine
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
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
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
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
                            repository = ProjectRepository(db, auth),
                            aiEngine = AiStudioEngine(),
                            localDb = LocalStudioDatabase.getInstance(app),
                            preferences = StudioPreferences(app),
                            httpClient = HttpWorkspaceClient(),
                            currentUserId = user.uid
                        )
                    }
                }
            )
            ForgeStudioApp(
                viewModel = viewModel,
                currentUserId = user.uid,
                userEmail = user.email
            )
        }
    }
}
