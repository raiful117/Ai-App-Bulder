package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiProviderSettings
import com.example.model.AppProject
import com.example.model.AssetCategory
import com.example.model.BuildJobRecord
import com.example.model.BuildTarget
import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticSeverity
import com.example.model.ProjectGenerators
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.OutputStream

@Composable
fun BuildCenterScreen(
    project: AppProject?,
    diagnostics: List<DiagnosticIssue>,
    providerSettings: AiProviderSettings,
    buildWorkerStatus: String,
    buildJobs: List<BuildJobRecord>,
    onRunDiagnostics: () -> Unit,
    onAiFixIssues: () -> Unit,
    onExportZip: (OutputStream, String, (Int) -> Unit) -> Unit,
    onTestWorkerConnection: () -> Unit,
    onTriggerRemoteBuild: (BuildTarget) -> Unit,
    onRefreshBuildJob: (BuildJobRecord) -> Unit,
    onDownloadBuildArtifact: (BuildJobRecord, OutputStream, (Result<Long>) -> Unit) -> Unit = { _, _, _ -> },
    onAddAsset: (String, AssetCategory, String, String) -> Unit,
    onDeleteAsset: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select or create a project to export & build.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var activeTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Export & APK") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Diagnostics (${diagnostics.size})") })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Assets (${project.backend.assets.size})") })
        }

        when (activeTab) {
            0 -> BuildAndExportTab(
                project = project,
                providerSettings = providerSettings,
                buildWorkerStatus = buildWorkerStatus,
                buildJobs = buildJobs,
                onExportZip = onExportZip,
                onTestWorkerConnection = onTestWorkerConnection,
                onTriggerRemoteBuild = onTriggerRemoteBuild,
                onRefreshBuildJob = onRefreshBuildJob,
                onDownloadBuildArtifact = onDownloadBuildArtifact,
                onOpenSettings = onOpenSettings
            )
            1 -> DiagnosticsTab(
                diagnostics = diagnostics,
                onRunDiagnostics = onRunDiagnostics,
                onAiFixIssues = onAiFixIssues
            )
            else -> AssetsManagerTab(
                project = project,
                onAddAsset = onAddAsset,
                onDeleteAsset = onDeleteAsset
            )
        }
    }
}

@Composable
private fun BuildAndExportTab(
    project: AppProject,
    providerSettings: AiProviderSettings,
    buildWorkerStatus: String,
    buildJobs: List<BuildJobRecord>,
    onExportZip: (OutputStream, String, (Int) -> Unit) -> Unit,
    onTestWorkerConnection: () -> Unit,
    onTriggerRemoteBuild: (BuildTarget) -> Unit,
    onRefreshBuildJob: (BuildJobRecord) -> Unit,
    onDownloadBuildArtifact: (BuildJobRecord, OutputStream, (Result<Long>) -> Unit) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var pendingExportMode by remember { mutableStateOf("ANDROID_STUDIO") }
    var lastExportNotice by remember { mutableStateOf<String?>(null) }
    var pendingArtifactJob by remember { mutableStateOf<BuildJobRecord?>(null) }

    val safZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                onExportZip(stream, pendingExportMode) { count ->
                    lastExportNotice = "Wrote $count files ($pendingExportMode) to selected archive."
                }
            }
        }
    }

    val safApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        val job = pendingArtifactJob
        if (uri != null && job != null) {
            val stream = context.contentResolver.openOutputStream(uri)
            if (stream != null) {
                onDownloadBuildArtifact(job, stream) { result ->
                    result.onSuccess { bytes ->
                        lastExportNotice = "Saved compiled ${job.target.label} (${bytes / 1024} KB) to device."
                    }.onFailure { err ->
                        lastExportNotice = "APK download failed: ${err.localizedMessage}"
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Real ZIP Export (Android SAF)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Archive, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Real Project ZIP Exporter (SAF)", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Packages real Kotlin source, build.gradle.kts, AndroidManifest.xml, Node.js backend, and SQL migrations into a portable .zip archive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val safeSlug = project.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").ifBlank { "bcf_project" }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                pendingExportMode = "ANDROID_STUDIO"
                                safZipLauncher.launch("${safeSlug}_android_studio.zip")
                            },
                            modifier = Modifier.weight(1f).testTag("export_android_zip_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Android Studio ZIP")
                        }
                        OutlinedButton(
                            onClick = {
                                pendingExportMode = "FULL_STACK"
                                safZipLauncher.launch("${safeSlug}_fullstack.zip")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Full-Stack ZIP")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                pendingExportMode = "SOURCE_ONLY"
                                safZipLauncher.launch("${safeSlug}_source.zip")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Source Code Only") }
                        OutlinedButton(
                            onClick = {
                                pendingExportMode = "BACKEND"
                                safZipLauncher.launch("${safeSlug}_backend.zip")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Backend Code") }
                    }
                    OutlinedButton(
                        onClick = {
                            val json = ProjectGenerators.exportProjectJson(project)
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("${project.name}.bcf.json", json))
                            lastExportNotice = "Copied portable project JSON (${json.length} chars) to clipboard."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Portable Project JSON Backup")
                    }
                    if (lastExportNotice != null) {
                        Text(lastExportNotice!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        // Honest Remote Gradle Build Worker Section
        item {
            val workerConfigured = providerSettings.buildWorkerUrl.isNotBlank()
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (workerConfigured) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Build, null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Remote Gradle APK/AAB Worker", style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (workerConfigured) buildWorkerStatus else "Not Configured",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (buildWorkerStatus.startsWith("Connected")) MaterialTheme.colorScheme.tertiary else Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!workerConfigured) {
                        Text(
                            text = "No remote Gradle Build Worker URL is configured. Real APK/AAB compilation requires connecting the Docker Gradle Worker from `/backend-server/` (never simulated on-device).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Filled.CloudQueue, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Configure Build Worker URL in Settings")
                        }
                    } else {
                        Text(
                            text = "Worker: ${providerSettings.buildWorkerUrl}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onTestWorkerConnection, shape = RoundedCornerShape(10.dp)) {
                                Text("Ping Health")
                            }
                            Button(onClick = { onTriggerRemoteBuild(BuildTarget.DEBUG_APK) }, shape = RoundedCornerShape(10.dp)) {
                                Text("Build Debug APK")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onTriggerRemoteBuild(BuildTarget.RELEASE_APK) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                Text("Release APK")
                            }
                            OutlinedButton(onClick = { onTriggerRemoteBuild(BuildTarget.APP_BUNDLE) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                Text("Release AAB")
                            }
                        }
                    }
                }
            }
        }

        if (buildJobs.isNotEmpty()) {
            item { Text("Build Queue & Logs (${buildJobs.size})", style = MaterialTheme.typography.titleMedium) }
            items(buildJobs, key = { it.buildId }) { job ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${job.target.label} • ${job.status}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onRefreshBuildJob(job) }) {
                                Icon(Icons.Filled.Refresh, "Refresh build status")
                            }
                        }
                        Text("Job ID: ${job.buildId} • ${job.createdAtLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(job.logs, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall)
                        if (job.artifactUrl.isNotBlank()) {
                            Text("Artifact: ${job.artifactUrl}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            if (job.status.equals("SUCCEEDED", ignoreCase = true)) {
                                val slug = project.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").ifBlank { "app" }
                                val ext = if (job.target == BuildTarget.APP_BUNDLE) "aab" else "apk"
                                Button(
                                    onClick = {
                                        pendingArtifactJob = job
                                        safApkLauncher.launch("${slug}_${job.buildId.takeLast(5)}.$ext")
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("download_apk_button_${job.buildId}"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save .$ext to Device (SAF)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsTab(
    diagnostics: List<DiagnosticIssue>,
    onRunDiagnostics: () -> Unit,
    onAiFixIssues: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRunDiagnostics, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.BugReport, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Run Static Checks")
                }
                OutlinedButton(onClick = onAiFixIssues, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Fix Mode")
                }
            }
        }

        if (diagnostics.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                        Column {
                            Text("Zero Static Issues Detected", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                            Text("Package name, Manifest, screens & source files look structurally valid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            items(diagnostics, key = { it.id }) { issue ->
                val accent = when (issue.severity) {
                    DiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
                    DiagnosticSeverity.WARNING -> Color(0xFFF59E0B)
                    DiagnosticSeverity.INFO -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${issue.severity.name} • ${issue.filePath}", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
                        Text(issue.message, style = MaterialTheme.typography.bodyMedium)
                        Text("Fix: ${issue.fixHint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetsManagerTab(
    project: AppProject,
    onAddAsset: (String, AssetCategory, String, String) -> Unit,
    onDeleteAsset: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(AssetCategory.IMAGE) }

    // Zero-permission Photo Picker per Google Play policy
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/png"
            onAddAsset("asset_${System.currentTimeMillis() % 10000}", AssetCategory.IMAGE, uri.toString(), mime)
        }
    }

    // Zero-permission SAF Document picker for fonts/SVG/Lottie/JSON
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            onAddAsset("res_${System.currentTimeMillis() % 10000}", selectedCategory, uri.toString(), mime)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resource & Assets Manager", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Uses zero-permission Android Photo Picker & Storage Access Framework for images, SVG vectors, custom TTF fonts, and Lottie JSON.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pick Image")
                        }
                        OutlinedButton(
                            onClick = { docPicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.InsertDriveFile, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import Asset")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(AssetCategory.ICON, AssetCategory.FONT, AssetCategory.LOTTIE).forEach { cat ->
                            FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat.label) })
                        }
                    }
                }
            }
        }

        items(project.backend.assets, key = { it.assetId }) { asset ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(asset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${asset.category.label} • ${asset.mimeType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(asset.uriOrPath.take(64), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDeleteAsset(asset.assetId) }) {
                        Icon(Icons.Filled.DeleteOutline, "Delete asset", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
