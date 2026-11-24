package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiProviderSettings
import com.example.model.AiProviderType
import com.example.model.AndroidBuildConfig
import com.example.model.AppProject
import kotlin.math.roundToInt

@Composable
fun SettingsAdminHelpScreen(
    initialTab: Int = 0, // 0 = Settings, 1 = Help
    activeProject: AppProject?,
    providerSettings: AiProviderSettings,
    localCachedCount: Int,
    buildJobsCount: Int,
    onSaveProviderSettings: (AiProviderSettings) -> Unit,
    onUpdateAndroidBuildConfig: (AndroidBuildConfig, String, String) -> Unit,
    onTestWorkerConnection: () -> Unit
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Settings & Admin") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Help & Guide") })
        }

        if (activeTab == 0) {
            SettingsAndAdminTab(
                activeProject = activeProject,
                providerSettings = providerSettings,
                localCachedCount = localCachedCount,
                buildJobsCount = buildJobsCount,
                onSaveProviderSettings = onSaveProviderSettings,
                onUpdateAndroidBuildConfig = onUpdateAndroidBuildConfig,
                onTestWorkerConnection = onTestWorkerConnection
            )
        } else {
            HelpGuideTab()
        }
    }
}

@Composable
private fun SettingsAndAdminTab(
    activeProject: AppProject?,
    providerSettings: AiProviderSettings,
    localCachedCount: Int,
    buildJobsCount: Int,
    onSaveProviderSettings: (AiProviderSettings) -> Unit,
    onUpdateAndroidBuildConfig: (AndroidBuildConfig, String, String) -> Unit,
    onTestWorkerConnection: () -> Unit
) {
    var provider by remember(providerSettings.provider) { mutableStateOf(providerSettings.provider) }
    var modelName by remember(providerSettings.modelName) { mutableStateOf(providerSettings.modelName) }
    var customEndpoint by remember(providerSettings.customEndpointUrl) { mutableStateOf(providerSettings.customEndpointUrl) }
    var customKey by remember(providerSettings.customApiKey) { mutableStateOf(providerSettings.customApiKey) }
    var workerUrl by remember(providerSettings.buildWorkerUrl) { mutableStateOf(providerSettings.buildWorkerUrl) }
    var workerToken by remember(providerSettings.buildWorkerToken) { mutableStateOf(providerSettings.buildWorkerToken) }
    var autosave by remember(providerSettings.autosaveEnabled) { mutableStateOf(providerSettings.autosaveEnabled) }
    var fontSize by remember(providerSettings.editorFontSizeSp) { mutableIntStateOf(providerSettings.editorFontSizeSp) }

    var appName by remember(activeProject?.id, activeProject?.name) { mutableStateOf(activeProject?.name.orEmpty()) }
    var packageName by remember(activeProject?.id, activeProject?.packageName) { mutableStateOf(activeProject?.packageName.orEmpty()) }
    var minSdk by remember(activeProject?.id) { mutableStateOf("${activeProject?.backend?.androidConfig?.minSdk ?: 24}") }
    var targetSdk by remember(activeProject?.id) { mutableStateOf("${activeProject?.backend?.androidConfig?.targetSdk ?: 36}") }
    var versionName by remember(activeProject?.id) { mutableStateOf(activeProject?.backend?.androidConfig?.versionName ?: "1.0.0") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Provider & Model Routing
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Psychology, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Provider-Independent AI Engine", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AiProviderType.entries.forEach { p ->
                            FilterChip(
                                selected = provider == p,
                                onClick = {
                                    provider = p
                                    modelName = p.defaultModel
                                },
                                label = { Text(p.displayName) }
                            )
                        }
                    }

                    OutlinedTextField(value = modelName, onValueChange = { modelName = it }, label = { Text("Model ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    if (provider != AiProviderType.GEMINI_FIREBASE) {
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = { customEndpoint = it },
                            label = { Text("Chat Completions Endpoint URL") },
                            placeholder = { Text("https://api.openai.com/v1/chat/completions") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customKey,
                            onValueChange = { customKey = it },
                            label = { Text("User API Key (Stored locally in DataStore)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("gemini-3.8-flash", "gemini-3.5-flash-lite").forEach { gm ->
                                FilterChip(
                                    selected = modelName == gm,
                                    onClick = { modelName = gm },
                                    label = { Text(gm, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Text(
                            text = "Uses Firebase AI Logic with App Check protection & automatic high-demand retry/failover across Gemini models.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Remote Build Worker & Editor Preferences
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Build Worker & Editor Config", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = workerUrl,
                        onValueChange = { workerUrl = it },
                        label = { Text("BCF Build Worker Base URL") },
                        placeholder = { Text("https://builder.yourdomain.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workerToken,
                        onValueChange = { workerToken = it },
                        label = { Text("Worker Bearer Token (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-cache edits in Room", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autosave, onCheckedChange = { autosave = it })
                    }
                    Text("Code Editor Font Size: ${fontSize}sp", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { fontSize = it.roundToInt() },
                        valueRange = 10f..18f
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveProviderSettings(
                                    AiProviderSettings(
                                        provider = provider,
                                        modelName = modelName.trim().ifBlank { provider.defaultModel },
                                        customEndpointUrl = customEndpoint.trim(),
                                        customApiKey = customKey.trim(),
                                        buildWorkerUrl = workerUrl.trim(),
                                        buildWorkerToken = workerToken.trim(),
                                        autosaveEnabled = autosave,
                                        editorFontSizeSp = fontSize
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Preferences")
                        }
                        if (workerUrl.isNotBlank()) {
                            OutlinedButton(onClick = onTestWorkerConnection, shape = RoundedCornerShape(10.dp)) {
                                Text("Test Worker")
                            }
                        }
                    }
                }
            }
        }

        // Active Android Project Config
        if (activeProject != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Android Project Settings (${activeProject.name})", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("App Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package Name / Application ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = minSdk, onValueChange = { minSdk = it }, label = { Text("minSdk") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = targetSdk, onValueChange = { targetSdk = it }, label = { Text("targetSdk") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = versionName, onValueChange = { versionName = it }, label = { Text("Version") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                val nextCfg = activeProject.backend.androidConfig.copy(
                                    minSdk = minSdk.toIntOrNull() ?: 24,
                                    targetSdk = targetSdk.toIntOrNull() ?: 36,
                                    versionName = versionName.ifBlank { "1.0.0" }
                                )
                                onUpdateAndroidBuildConfig(nextCfg, appName, packageName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Apply Project Config")
                        }
                    }
                }
            }
        }

        // Admin & Workspace Security Panel
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("Admin, Quotas & Security Status", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("• Offline Room Projects Cached: $localCachedCount", style = MaterialTheme.typography.bodySmall)
                    Text("• Submitted Remote Build Jobs: $buildJobsCount", style = MaterialTheme.typography.bodySmall)
                    Text("• Auth Gate: Google Sign-In (Credential Manager) + Firebase App Check", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    Text("• Secret Hygiene: No Admin SDK or signing keys bundled in Android client", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HelpGuideTab() {
    val sections = remember {
        listOf(
            "1. AI Studio & Project Agent (13 Modes)" to "Pick a generation mode such as Quick Generate, Full Project, Plan First, Add Feature, Fix Existing Project, or Generate Tests. Modes targeting an active project inspect your existing files and produce reviewable file patches before overwriting.",
            "2. Visual Designer (Sketchware + Compose)" to "Add from 39 widgets & containers, adjust corner radius/padding/elevation/actions, use Undo/Redo, and tap Sync Code to generate Jetpack Compose source inside `app/src/main/java/.../ui/`.",
            "3. Visual Logic Blocks" to "Compose Events, Logic, Variables, Math, Text, UI, Android, HTTP, and Firebase blocks per screen, then click Compile to Kotlin to create executable ViewModel/Controller logic.",
            "4. Backend, Database & REST API Studio" to "Design REST routes, generate Node.js + TypeScript Express + Dockerfile files, design SQL/Firestore schemas, generate SQL migrations, and run live OkHttp API tests.",
            "5. Exporting Android Studio ZIP & Running Build Worker" to "Use Build Center -> Android Studio ZIP to export a complete Gradle project via Android SAF. To compile remote APKs/AABs, deploy the included `/backend-server/` Node.js + Docker Gradle worker and enter its URL in Settings."
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                Text("BCF Builder AI Documentation", style = MaterialTheme.typography.titleLarge)
            }
        }
        items(sections, key = { it.first }) { (title, body) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
