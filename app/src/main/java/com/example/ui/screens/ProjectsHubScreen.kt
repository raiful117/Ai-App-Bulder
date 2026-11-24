package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.local.ProjectSnapshotEntity
import com.example.model.AppProject
import com.example.model.ProjectGenerators
import com.example.model.StarterBlueprints
import com.example.viewmodel.StudioTab
import com.example.viewmodel.UiState

@Composable
fun ProjectsHubScreen(
    currentUserId: String,
    projectsState: UiState<List<AppProject>>,
    activeProject: AppProject?,
    snapshots: List<ProjectSnapshotEntity>,
    onOpenAiForge: () -> Unit,
    onCreateBlank: (String) -> Unit,
    onCreateTemplate: (AppProject) -> Unit,
    onDuplicateProject: (AppProject) -> Unit,
    onImportJson: (String) -> Unit,
    onCreateSnapshot: (String) -> Unit,
    onRestoreSnapshot: (ProjectSnapshotEntity) -> Unit,
    onOpenProject: (AppProject, StudioTab) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    val context = LocalContext.current
    var showNewDialog by remember { mutableStateOf(false) }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var newAppName by remember { mutableStateOf("") }
    var snapshotLabel by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    val templates = remember(currentUserId) { StarterBlueprints.templates(currentUserId) }

    val jsonImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (raw.isNotBlank()) onImportJson(raw)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("My Projects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Cloud Firestore + Room offline cache & version snapshots",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { jsonImportLauncher.launch(arrayOf("application/json", "text/plain")) }, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import")
                    }
                    Button(
                        onClick = { showNewDialog = true },
                        modifier = Modifier.testTag("hub_blank_canvas_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Search projects by name or package...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onOpenAiForge,
                    modifier = Modifier.testTag("hub_new_ai_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (statusNotice != null) {
            item {
                Text(statusNotice!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
        }

        // Active Project Version History & Rollback
        if (activeProject != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Text("Version Snapshots • ${activeProject.name}", style = MaterialTheme.typography.titleSmall)
                            }
                            OutlinedButton(onClick = { showSnapshotDialog = true }, shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Filled.Save, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Checkpoint", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (snapshots.isEmpty()) {
                            Text(
                                "No checkpoints yet. Save a snapshot before major refactors.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                snapshots.take(6).forEach { snap ->
                                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(snap.label, style = MaterialTheme.typography.labelSmall)
                                            IconButton(onClick = { onRestoreSnapshot(snap) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Filled.Restore, "Rollback to snapshot", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        when (projectsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Text(projectsState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is UiState.Success -> {
                val projects = projectsState.data.filter {
                    searchQuery.isBlank() ||
                        it.name.contains(searchQuery, true) ||
                        it.packageName.contains(searchQuery, true)
                }
                if (projects.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.Widgets, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Text("No matching projects", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Tap 'New', 'Import', or clone one of the 15 starter blueprints below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            selected = project.id == activeProject?.id,
                            onOpenWorkspace = { onOpenProject(project, StudioTab.WORKSPACE) },
                            onOpenVisual = { onOpenProject(project, StudioTab.VISUAL_BUILDER) },
                            onOpenBlocks = { onOpenProject(project, StudioTab.BLOCKS) },
                            onOpenPreview = { onOpenProject(project, StudioTab.PREVIEW_REVIEW) },
                            onOpenCode = { onOpenProject(project, StudioTab.CODE_TREE) },
                            onDuplicate = { onDuplicateProject(project) },
                            onCopyJson = {
                                val json = ProjectGenerators.exportProjectJson(project)
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("${project.name}.json", json))
                                statusNotice = "Copied '${project.name}' JSON backup to clipboard."
                            },
                            onDelete = { onDeleteProject(project.id) }
                        )
                    }
                }
            }
        }

        // Starter Blueprints Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Starter Blueprints (${templates.size})", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(templates, key = { it.name }) { tpl ->
                        TemplateBlueprintCard(template = tpl, onClone = { onCreateTemplate(tpl) })
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("Create Blank Studio App") },
            text = {
                OutlinedTextField(
                    value = newAppName,
                    onValueChange = { newAppName = it },
                    label = { Text("App Name") },
                    placeholder = { Text("e.g. TaskFlow Pro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("blank_app_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateBlank(newAppName)
                        newAppName = ""
                        showNewDialog = false
                    },
                    modifier = Modifier.testTag("confirm_blank_app_button")
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            title = { Text("Save Version Snapshot") },
            text = {
                OutlinedTextField(
                    value = snapshotLabel,
                    onValueChange = { snapshotLabel = it },
                    label = { Text("Checkpoint Label") },
                    placeholder = { Text("Before Auth & Checkout refactor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCreateSnapshot(snapshotLabel)
                    snapshotLabel = ""
                    showSnapshotDialog = false
                }) { Text("Save Snapshot") }
            },
            dismissButton = { TextButton(onClick = { showSnapshotDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProjectCard(
    project: AppProject,
    selected: Boolean,
    onOpenWorkspace: () -> Unit,
    onOpenVisual: () -> Unit,
    onOpenBlocks: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenCode: () -> Unit,
    onDuplicate: () -> Unit,
    onCopyJson: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onOpenWorkspace,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(project.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (project.tagline.isNotBlank()) {
                        Text(project.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row {
                    IconButton(onClick = onCopyJson) { Icon(Icons.Filled.Code, "Copy JSON backup", modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDuplicate) { Icon(Icons.Filled.ContentCopy, "Duplicate project", modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "Delete project", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${project.screens.size} Screens")
                StatChip("${project.files.size} Files")
                StatChip(project.architecture.displayName)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onOpenWorkspace, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("IDE")
                }
                OutlinedButton(onClick = onOpenVisual, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Widgets, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("UI")
                }
                OutlinedButton(onClick = onOpenBlocks, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Extension, "Blocks", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = onOpenPreview, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.PlayArrow, "Preview", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun TemplateBlueprintCard(template: AppProject, onClone: () -> Unit) {
    val accent = Color(template.themePreset.primaryHex)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = Modifier.width(238.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(accent))
                Text(template.architecture.displayName, style = MaterialTheme.typography.labelMedium, color = accent)
            }
            Text(template.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(template.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onClone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF041C26))
            ) {
                Text("Use Template", fontWeight = FontWeight.Bold)
            }
        }
    }
}
