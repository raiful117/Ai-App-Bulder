package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppProject
import com.example.model.DatabaseEngine
import com.example.model.DeploymentRecord
import com.example.model.FirebaseSetupConfig
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.UiState

@Composable
fun FirebaseDatabaseScreen(
    project: AppProject?,
    initialTab: Int = 0, // 0 = Firebase Studio, 1 = Multi-Engine Database Designer
    deploymentsState: UiState<List<DeploymentRecord>>,
    aiState: AiTaskState,
    onUpdateFirebaseConfig: (FirebaseSetupConfig) -> Unit,
    onAddCollection: (String, String, String) -> Unit,
    onRemoveCollection: (String) -> Unit,
    onAddDatabaseTable: (DatabaseEngine, String, String, String, String) -> Unit,
    onGenerateMigration: () -> Unit,
    onDeployProject: (String, String, String) -> Unit,
    onRollbackDeployment: (String) -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select or create a project to configure Firebase & Database.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Firebase Studio") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Database Studio") })
        }

        if (activeTab == 0) {
            FirebaseStudioTab(
                project = project,
                deploymentsState = deploymentsState,
                aiState = aiState,
                onUpdateFirebaseConfig = onUpdateFirebaseConfig,
                onAddCollection = onAddCollection,
                onRemoveCollection = onRemoveCollection,
                onDeployProject = onDeployProject,
                onRollbackDeployment = onRollbackDeployment
            )
        } else {
            DatabaseDesignerTab(
                project = project,
                onAddDatabaseTable = onAddDatabaseTable,
                onGenerateMigration = onGenerateMigration
            )
        }
    }
}

@Composable
private fun FirebaseStudioTab(
    project: AppProject,
    deploymentsState: UiState<List<DeploymentRecord>>,
    aiState: AiTaskState,
    onUpdateFirebaseConfig: (FirebaseSetupConfig) -> Unit,
    onAddCollection: (String, String, String) -> Unit,
    onRemoveCollection: (String) -> Unit,
    onDeployProject: (String, String, String) -> Unit,
    onRollbackDeployment: (String) -> Unit
) {
    val cfg = project.backend.firebaseConfig
    var fbProjectId by remember(cfg.firebaseProjectId) { mutableStateOf(cfg.firebaseProjectId) }
    var androidAppId by remember(cfg.androidAppId) { mutableStateOf(cfg.androidAppId) }
    var webApiKey by remember(cfg.webApiKey) { mutableStateOf(cfg.webApiKey) }
    var storageBucket by remember(cfg.storageBucket) { mutableStateOf(cfg.storageBucket) }

    var showAddCollection by remember { mutableStateOf(false) }
    var colName by remember { mutableStateOf("") }
    var colFields by remember { mutableStateOf("userId, title, status, createdAt") }

    var showDeployDialog by remember { mutableStateOf(false) }
    var deployEnv by remember { mutableStateOf("PRODUCTION") }
    var deployVersion by remember(project.activeVersion) { mutableStateOf("v1.0.1") }
    var deployNotes by remember { mutableStateOf("Firebase schema, security rules & Compose release") }

    val guideSteps = remember {
        listOf(
            "1. Create a Firebase project at console.firebase.google.com.",
            "2. Enable Authentication (Google Sign-In / Email-Password) and copy your Web Client ID.",
            "3. Register Android app with package `${project.packageName}` and SHA-1/SHA-256 keys.",
            "4. Download `google-services.json` and paste Project ID, App ID, API Key & Bucket below.",
            "5. Provision Firestore Database & review generated zero-trust `firestore.rules`.",
            "6. Enable App Check (Play Integrity) and optional Storage / FCM Push / Cloud Functions."
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.img_cloud_infra),
                        contentDescription = stringResource(id = R.string.cd_cloud_infra),
                        modifier = Modifier.fillMaxWidth().height(136.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(136.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))))
                    )
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Firebase Integration Center", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = if (cfg.validated) "Validated • ${cfg.firebaseProjectId}" else "Draft / Unconfigured Project Keys",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (cfg.validated) MaterialTheme.colorScheme.tertiary else Color(0xFFF59E0B)
                            )
                        }
                        Button(
                            onClick = { showDeployDialog = true },
                            enabled = aiState !is AiTaskState.Working,
                            modifier = Modifier.testTag("open_deploy_dialog_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (aiState is AiTaskState.Working) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Record Release")
                        }
                    }
                }
            }
        }

        // Firebase Configuration & Validator
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFF97316))
                        Text("Target Project Credentials", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(value = fbProjectId, onValueChange = { fbProjectId = it }, label = { Text("Firebase Project ID") }, placeholder = { Text("my-app-prod") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = androidAppId, onValueChange = { androidAppId = it }, label = { Text("mobilesdk_app_id") }, placeholder = { Text("1:123456789:android:abcdef") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = webApiKey, onValueChange = { webApiKey = it }, label = { Text("Web API Key (Public Client)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = storageBucket, onValueChange = { storageBucket = it }, label = { Text("Storage Bucket") }, placeholder = { Text("my-app-prod.firebasestorage.app") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            onUpdateFirebaseConfig(
                                cfg.copy(
                                    firebaseProjectId = fbProjectId.trim(),
                                    androidAppId = androidAppId.trim(),
                                    webApiKey = webApiKey.trim(),
                                    storageBucket = storageBucket.trim()
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Validate & Save Firebase Config")
                    }
                }
            }
        }

        // Firebase Services Matrix
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Enabled Firebase Modules", style = MaterialTheme.typography.titleMedium)
                    FbServiceToggle("Google Sign-In (Credential Manager)", cfg.useAuthGoogle) { onUpdateFirebaseConfig(cfg.copy(useAuthGoogle = it)) }
                    FbServiceToggle("Email / Password Authentication", cfg.useAuthEmail) { onUpdateFirebaseConfig(cfg.copy(useAuthEmail = it)) }
                    FbServiceToggle("Cloud Firestore Database", cfg.useFirestore) { onUpdateFirebaseConfig(cfg.copy(useFirestore = it)) }
                    FbServiceToggle("Firebase Realtime Database", cfg.useRealtimeDb) { onUpdateFirebaseConfig(cfg.copy(useRealtimeDb = it)) }
                    FbServiceToggle("Cloud Storage for Firebase", cfg.useStorage) { onUpdateFirebaseConfig(cfg.copy(useStorage = it)) }
                    FbServiceToggle("Cloud Messaging (FCM Push)", cfg.useMessaging) { onUpdateFirebaseConfig(cfg.copy(useMessaging = it)) }
                    FbServiceToggle("App Check Security", cfg.useAppCheck) { onUpdateFirebaseConfig(cfg.copy(useAppCheck = it)) }
                    FbServiceToggle("Cloud Functions / Analytics", cfg.useFunctions) { onUpdateFirebaseConfig(cfg.copy(useFunctions = it)) }
                }
            }
        }

        // Firestore Collections
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Firestore Collections & Rules", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(onClick = { showAddCollection = true }, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Collection")
                        }
                    }

                    project.backend.collections.forEach { col ->
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("/${col.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(col.fieldsSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Rule: ${col.accessRule}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                                IconButton(onClick = { onRemoveCollection(col.name) }) {
                                    Icon(Icons.Filled.DeleteOutline, "Remove collection")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Guided Setup Checklist
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Firebase Guided Setup Assistant", style = MaterialTheme.typography.titleMedium)
                    guideSteps.forEach { step ->
                        Text(step, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Deployment Releases
        when (deploymentsState) {
            is UiState.Success -> {
                val filteredDeps = deploymentsState.data.filter { it.projectId == project.id }
                if (filteredDeps.isNotEmpty()) {
                    item { Text("Recorded Releases (${filteredDeps.size})", style = MaterialTheme.typography.titleMedium) }
                    items(filteredDeps, key = { it.id }) { dep ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${dep.versionTag} • ${dep.environment} • ${dep.status}", style = MaterialTheme.typography.titleSmall)
                                    Text(dep.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (dep.status != "ROLLED_BACK") {
                                    OutlinedButton(onClick = { onRollbackDeployment(dep.id) }) { Text("Rollback") }
                                }
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    if (showAddCollection) {
        AlertDialog(
            onDismissRequest = { showAddCollection = false },
            title = { Text("Add Firestore Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = colName, onValueChange = { colName = it }, label = { Text("Collection") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = colFields, onValueChange = { colFields = it }, label = { Text("Fields") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddCollection(colName, colFields, "Owner Only (request.auth.uid)")
                    colName = ""
                    showAddCollection = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddCollection = false }) { Text("Cancel") } }
        )
    }

    if (showDeployDialog) {
        AlertDialog(
            onDismissRequest = { showDeployDialog = false },
            title = { Text("Record Cloud Release") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("STAGING", "PRODUCTION").forEach { env ->
                            FilterChip(selected = deployEnv == env, onClick = { deployEnv = env }, label = { Text(env) })
                        }
                    }
                    OutlinedTextField(value = deployVersion, onValueChange = { deployVersion = it }, label = { Text("Version Tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = deployNotes, onValueChange = { deployNotes = it }, label = { Text("Notes") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDeployProject(deployEnv, deployVersion, deployNotes)
                    showDeployDialog = false
                }) { Text("Save Release") }
            },
            dismissButton = { TextButton(onClick = { showDeployDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DatabaseDesignerTab(
    project: AppProject,
    onAddDatabaseTable: (DatabaseEngine, String, String, String, String) -> Unit,
    onGenerateMigration: () -> Unit
) {
    var selectedEngine by remember { mutableStateOf(DatabaseEngine.POSTGRESQL) }
    var tableName by remember { mutableStateOf("") }
    var columns by remember { mutableStateOf("id UUID PK, user_id TEXT NOT NULL, title TEXT, created_at TIMESTAMPTZ") }
    var relations by remember { mutableStateOf("user_id -> users.id") }
    var indexName by remember { mutableStateOf("idx_created_at") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Multi-Engine Database Schema Designer", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DatabaseEngine.entries.forEach { engine ->
                            FilterChip(
                                selected = selectedEngine == engine,
                                onClick = { selectedEngine = engine },
                                label = { Text(engine.displayName) }
                            )
                        }
                    }

                    OutlinedTextField(value = tableName, onValueChange = { tableName = it }, label = { Text("Table / Collection Name") }, placeholder = { Text("orders, invoices, sessions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = columns, onValueChange = { columns = it }, label = { Text("Columns & Types") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = relations, onValueChange = { relations = it }, label = { Text("Foreign Keys / Relations") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = indexName, onValueChange = { indexName = it }, label = { Text("Index Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onAddDatabaseTable(selectedEngine, tableName, columns, relations, indexName)
                                tableName = ""
                            },
                            enabled = tableName.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Table")
                        }
                        OutlinedButton(
                            onClick = onGenerateMigration,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SQL Migration")
                        }
                    }
                }
            }
        }

        items(project.backend.databaseTables, key = { it.tableId }) { tbl ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tbl.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(tbl.engine.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(tbl.columnsSummary, style = MaterialTheme.typography.bodySmall)
                    if (tbl.relationsSummary.isNotBlank()) {
                        Text("Relations: ${tbl.relationsSummary}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (tbl.indexSummary.isNotBlank()) {
                        Text("Index: ${tbl.indexSummary}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FbServiceToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
