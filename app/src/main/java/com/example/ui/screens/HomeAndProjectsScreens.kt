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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiProviderSettings
import com.example.model.AppProject
import com.example.model.StarterBlueprints
import com.example.viewmodel.StudioTab
import com.example.viewmodel.UiState

@Composable
fun HomeDashboardScreen(
    activeProject: AppProject?,
    projectsState: UiState<List<AppProject>>,
    localCachedCount: Int,
    providerSettings: AiProviderSettings,
    buildWorkerStatus: String,
    onNavigateTab: (StudioTab) -> Unit,
    onOpenProject: (AppProject, StudioTab) -> Unit
) {
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero IDE Banner
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = stringResource(id = R.string.cd_studio_hero),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${providerSettings.provider.displayName} • ${providerSettings.modelName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "BCF Builder AI IDE",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Visual Designer, Logic Blocks, Full-Stack Kotlin & Node.js Generator, Firebase & Build Worker.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onNavigateTab(StudioTab.AI_ARCHITECT) },
                                modifier = Modifier.testTag("home_ai_studio_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("AI App Generator")
                            }
                            OutlinedButton(
                                onClick = { onNavigateTab(StudioTab.TEMPLATES) },
                                modifier = Modifier.testTag("home_templates_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.FolderCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("15 Templates")
                            }
                        }
                    }
                }
            }
        }

        // Workspace Telemetry Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile("Cloud Projects", "${projects.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricTile("Room Cache", "$localCachedCount", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                MetricTile(
                    "Build Worker",
                    if (providerSettings.buildWorkerUrl.isBlank()) "Unconfigured" else buildWorkerStatus.take(12),
                    MaterialTheme.colorScheme.tertiary,
                    Modifier.weight(1f)
                )
            }
        }

        // Active Project Quick Bar
        if (activeProject != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ACTIVE WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(activeProject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${activeProject.packageName} • ${activeProject.screens.size} screens • ${activeProject.files.size} files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = { onNavigateTab(StudioTab.WORKSPACE) }, shape = RoundedCornerShape(10.dp)) {
                                Text("Open IDE")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickModuleButton("Project IDE", Icons.Filled.Code) { onNavigateTab(StudioTab.WORKSPACE) }
                            QuickModuleButton("Designer", Icons.Filled.Widgets) { onNavigateTab(StudioTab.VISUAL_BUILDER) }
                            QuickModuleButton("Blocks", Icons.Filled.Extension) { onNavigateTab(StudioTab.BLOCKS) }
                            QuickModuleButton("Preview", Icons.Filled.PlayArrow) { onNavigateTab(StudioTab.PREVIEW_REVIEW) }
                            QuickModuleButton("Build & ZIP", Icons.Filled.Build) { onNavigateTab(StudioTab.BUILD_CENTER) }
                        }
                    }
                }
            }
        }

        // Full-Stack Platform Modules Grid
        item {
            Text("Platform Studios & Tools", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioTile(
                        title = "AI Studio & Agent",
                        subtitle = "13 modes + targeted patches",
                        icon = Icons.Filled.AutoAwesome,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.AI_ARCHITECT) }
                    )
                    StudioTile(
                        title = "Visual Designer",
                        subtitle = "39 Sketchware/M3 widgets",
                        icon = Icons.Filled.Widgets,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.VISUAL_BUILDER) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioTile(
                        title = "Logic Blocks",
                        subtitle = "Event & API blocks -> Kotlin",
                        icon = Icons.Filled.Extension,
                        accent = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.BLOCKS) }
                    )
                    StudioTile(
                        title = "Code & Gradle",
                        subtitle = "Multi-file tree + Search",
                        icon = Icons.Filled.Code,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.CODE_TREE) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioTile(
                        title = "Backend & API",
                        subtitle = "Node.js TS + OkHttp runner",
                        icon = Icons.Filled.Api,
                        accent = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.BACKEND) }
                    )
                    StudioTile(
                        title = "Firebase & DB",
                        subtitle = "Auth, Rules, SQL & NoSQL",
                        icon = Icons.Filled.LocalFireDepartment,
                        accent = Color(0xFFF97316),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.FIREBASE) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioTile(
                        title = "Database Designer",
                        subtitle = "Firestore, Postgres, Room",
                        icon = Icons.Filled.Storage,
                        accent = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.DATABASE) }
                    )
                    StudioTile(
                        title = "Build Center",
                        subtitle = "Diagnostics, ZIP & APK Worker",
                        icon = Icons.Filled.Build,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(StudioTab.BUILD_CENTER) }
                    )
                }
            }
        }

        // Recent Projects
        if (projects.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Projects", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { onNavigateTab(StudioTab.PROJECTS) }) {
                        Text("View All")
                    }
                }
            }
            items(projects.take(3), key = { it.id }) { proj ->
                Surface(
                    onClick = { onOpenProject(proj, StudioTab.WORKSPACE) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(proj.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${proj.screens.size} screens • ${proj.files.size} files • ${proj.architecture.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.CloudQueue, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun TemplatesGalleryScreen(
    currentUserId: String,
    onCreateTemplate: (AppProject) -> Unit,
    onCreateBlank: (String) -> Unit
) {
    val allTemplates = remember(currentUserId) { StarterBlueprints.templates(currentUserId) }
    var searchQuery by remember { mutableStateOf("") }
    var filterTag by remember { mutableStateOf("All") }
    val categories = remember { listOf("All", "Firebase", "E-Commerce", "Social & Media", "Offline / Admin") }

    val filtered = remember(allTemplates, searchQuery, filterTag) {
        allTemplates.filter { tpl ->
            val matchesSearch = searchQuery.isBlank() ||
                tpl.name.contains(searchQuery, ignoreCase = true) ||
                tpl.tagline.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (filterTag) {
                "Firebase" -> tpl.name.contains("Firebase", true) || tpl.name.contains("Auth", true) || tpl.name.contains("Full-Stack", true)
                "E-Commerce" -> tpl.name.contains("Commerce", true) || tpl.name.contains("REST", true) || tpl.name.contains("Dashboard", true)
                "Social & Media" -> tpl.name.contains("Social", true) || tpl.name.contains("Chat", true) || tpl.name.contains("Music", true) || tpl.name.contains("Video", true) || tpl.name.contains("News", true)
                "Offline / Admin" -> tpl.name.contains("Offline", true) || tpl.name.contains("Admin", true) || tpl.name.contains("Blank", true) || tpl.name.contains("Material", true)
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Starter App Templates (${allTemplates.size})", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Production Android + Backend starter blueprints ready to customize visually or with AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { onCreateBlank("New BCF App") }, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Blank")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Search 15 starter blueprints...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = filterTag == cat,
                        onClick = { filterTag = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        items(filtered, key = { it.name }) { tpl ->
            val accent = Color(tpl.themePreset.primaryHex)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
                            Text(tpl.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(tpl.architecture.displayName, style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                    Text(
                        text = tpl.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tpl.screens.size} screens • ${tpl.files.size} files • ${tpl.backend.collections.size} collections • ${tpl.backend.endpoints.size} endpoints",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onCreateTemplate(tpl) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF04141E))
                    ) {
                        Text("Create from Template", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickModuleButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StudioTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}
