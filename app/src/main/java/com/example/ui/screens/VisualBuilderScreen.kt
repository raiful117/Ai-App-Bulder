package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppComponent
import com.example.model.AppProject
import com.example.model.AppThemePreset
import com.example.model.ColorRole
import com.example.model.ComponentAction
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.ui.components.PreviewComponentRenderer
import com.example.viewmodel.PreviewRuntimeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualBuilderScreen(
    project: AppProject?,
    activeScreenId: String?,
    selectedComponentId: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectScreen: (String) -> Unit,
    onAddScreen: (String, String, String) -> Unit,
    onDeleteScreen: (String) -> Unit,
    onSelectComponent: (String?) -> Unit,
    onAddComponent: (ComponentType) -> Unit,
    onUpdateComponent: (AppComponent) -> Unit,
    onMoveComponent: (String, Int) -> Unit,
    onDuplicateComponent: (String) -> Unit,
    onDeleteComponent: (String) -> Unit,
    onUpdateTheme: (AppThemePreset) -> Unit,
    onAiPopulateScreen: (String) -> Unit,
    onSyncCode: () -> Unit,
    onOpenBlocksForWidget: (AppComponent) -> Unit = {}
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select or create a project in Studio first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val currentScreen = project.screens.firstOrNull { it.screenId == activeScreenId } ?: project.screens.firstOrNull()
    val selectedComponent = currentScreen?.components?.firstOrNull { it.componentId == selectedComponentId }

    var showSidePalette by remember { mutableStateOf(true) }
    var leftPanelTreeMode by remember { mutableStateOf(false) }
    var selectedPaletteCat by remember { mutableStateOf<ComponentCategory?>(null) }
    var paletteSearch by remember { mutableStateOf("") }
    var showAddScreenDialog by remember { mutableStateOf(false) }
    var showAiScreenDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showGridOverlay by remember { mutableStateOf(false) }

    var newScreenTitle by remember { mutableStateOf("") }
    var newScreenArgs by remember { mutableStateOf("") }
    var newScreenDeepLink by remember { mutableStateOf("") }
    var aiScreenPrompt by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Screen Selector + Sketchware View Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    project.screens.forEach { scr ->
                        FilterChip(
                            selected = scr.screenId == currentScreen?.screenId,
                            onClick = { onSelectScreen(scr.screenId) },
                            label = { Text("${scr.title} (${scr.components.size})") }
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddScreenDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Screen", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showSidePalette = !showSidePalette },
                        modifier = Modifier.testTag("toggle_widget_palette_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ViewSidebar,
                            stringResource(id = R.string.cd_add_component),
                            tint = if (showSidePalette) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (showSidePalette) "Hide Palette" else "Widgets", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo", modifier = Modifier.size(16.dp))
                    }
                    OutlinedButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo", modifier = Modifier.size(16.dp))
                    }
                    OutlinedButton(
                        onClick = { showGridOverlay = !showGridOverlay },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.GridOn,
                            "Grid bounds",
                            tint = if (showGridOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = { showAiScreenDialog = true },
                        modifier = Modifier.testTag("ai_populate_screen_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Layout", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onSyncCode,
                        modifier = Modifier.testTag("sync_compose_code_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Sync, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync XML+Code", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { showThemeDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Palette, "Theme", modifier = Modifier.size(16.dp))
                    }
                    if (project.screens.size > 1 && currentScreen != null) {
                        OutlinedButton(
                            onClick = { onDeleteScreen(currentScreen.screenId) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.DeleteOutline, "Delete screen", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Sketchware Split Designer: Side Widget Palette (Left) + Phone Canvas (Right)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isWide = maxWidth >= 680.dp
            val paletteWidth = if (isWide) 216.dp else 134.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (showSidePalette) {
                    Surface(
                        modifier = Modifier
                            .width(paletteWidth)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = !leftPanelTreeMode,
                                    onClick = { leftPanelTreeMode = false },
                                    label = { Text("Widgets", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = leftPanelTreeMode,
                                    onClick = { leftPanelTreeMode = true },
                                    label = { Text("Tree (${currentScreen?.components?.size ?: 0})", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (leftPanelTreeMode) {
                                val treeComps = currentScreen?.components.orEmpty()
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    items(treeComps, key = { "tree_${it.componentId}" }) { comp ->
                                        val isSel = comp.componentId == selectedComponent?.componentId
                                        Surface(
                                            onClick = { onSelectComponent(if (isSel) null else comp.componentId) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                                Text(
                                                    text = comp.text.ifBlank { comp.type.displayName },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${comp.type.displayName} • #${comp.componentId.takeLast(4)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = paletteSearch,
                                    onValueChange = { paletteSearch = it },
                                    placeholder = { Text("Filter...", style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedPaletteCat == null,
                                        onClick = { selectedPaletteCat = null },
                                        label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    ComponentCategory.entries.forEach { cat ->
                                        FilterChip(
                                            selected = selectedPaletteCat == cat,
                                            onClick = { selectedPaletteCat = cat },
                                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                val filteredWidgets = remember(selectedPaletteCat, paletteSearch) {
                                    ComponentType.entries.filter { type ->
                                        val catOk = selectedPaletteCat == null || type.category == selectedPaletteCat
                                        val qOk = paletteSearch.isBlank() ||
                                            type.displayName.contains(paletteSearch, ignoreCase = true) ||
                                            type.category.label.contains(paletteSearch, ignoreCase = true)
                                        catOk && qOk
                                    }
                                }
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredWidgets, key = { it.name }) { type ->
                                        val catColor = when (type.category) {
                                            ComponentCategory.LAYOUT -> Color(0xFF38BDF8)
                                            ComponentCategory.DISPLAY -> Color(0xFF8B5CF6)
                                            ComponentCategory.INPUT -> Color(0xFF10B981)
                                            ComponentCategory.DATA -> Color(0xFFF59E0B)
                                        }
                                        Surface(
                                            onClick = { onAddComponent(type) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.45f)),
                                            modifier = Modifier.testTag("palette_widget_${type.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(22.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(catColor)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = type.displayName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = type.category.label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                                Icon(
                                                    Icons.Filled.Add,
                                                    contentDescription = "Add ${type.displayName}",
                                                    tint = catColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Screen Canvas (Right)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(if (isWide) 12.dp else 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0A0F1A),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentScreen == null || currentScreen.components.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Filled.Widgets, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                                            Text("Empty Screen Canvas", style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "Tap any widget from the left side palette to drop it onto '${currentScreen?.title ?: "Screen"}', or tap 'AI Layout'.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(currentScreen.components, key = { it.componentId }) { comp ->
                                    val isSelected = comp.componentId == selectedComponent?.componentId
                                    val widgetBlockCount = currentScreen.logicBlocks.count { it.targetWidgetId == comp.componentId }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .padding(if (isSelected) 6.dp else 0.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PreviewComponentRenderer(
                                            component = comp,
                                            themePreset = project.themePreset,
                                            darkSurface = true,
                                            interactive = false,
                                            xrayMode = isSelected || showGridOverlay,
                                            runtimeState = PreviewRuntimeState(),
                                            onComponentTap = { onSelectComponent(if (isSelected) null else comp.componentId) }
                                        )

                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${comp.type.displayName} • #${comp.componentId.takeLast(5)}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Button(
                                                            onClick = { onOpenBlocksForWidget(comp) },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFFF59E0B),
                                                                contentColor = Color(0xFF1F1300)
                                                            ),
                                                            modifier = Modifier.testTag("widget_add_blocks_button")
                                                        ) {
                                                            Icon(Icons.Filled.Extension, null, modifier = Modifier.size(14.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(
                                                                if (widgetBlockCount > 0) "Blocks ($widgetBlockCount)" else "+ Blocks",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        IconButton(onClick = { onMoveComponent(comp.componentId, -1) }) {
                                                            Icon(Icons.Filled.ArrowUpward, "Move up", modifier = Modifier.size(18.dp))
                                                        }
                                                        IconButton(onClick = { onMoveComponent(comp.componentId, 1) }) {
                                                            Icon(Icons.Filled.ArrowDownward, "Move down", modifier = Modifier.size(18.dp))
                                                        }
                                                        IconButton(onClick = { onDuplicateComponent(comp.componentId) }) {
                                                            Icon(Icons.Filled.ContentCopy, "Duplicate", modifier = Modifier.size(18.dp))
                                                        }
                                                        IconButton(onClick = { onDeleteComponent(comp.componentId) }) {
                                                            Icon(
                                                                Icons.Filled.DeleteOutline,
                                                                "Delete component",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
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
                }
            }
        }

        if (selectedComponent != null) {
            ComponentInspectorPanel(
                component = selectedComponent,
                availableRoutes = project.screens.map { it.route },
                onUpdate = onUpdateComponent,
                onOpenBlocks = { onOpenBlocksForWidget(selectedComponent) },
                onDismiss = { onSelectComponent(null) }
            )
        }
    }

    if (showAddScreenDialog) {
        AlertDialog(
            onDismissRequest = { showAddScreenDialog = false },
            title = { Text("Add Screen & Navigation Route") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newScreenTitle,
                        onValueChange = { newScreenTitle = it },
                        label = { Text("Screen Title") },
                        placeholder = { Text("Orders, Profile, Details") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newScreenArgs,
                        onValueChange = { newScreenArgs = it },
                        label = { Text("Nav Arguments (Optional)") },
                        placeholder = { Text("orderId: String") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newScreenDeepLink,
                        onValueChange = { newScreenDeepLink = it },
                        label = { Text("Deep Link URI (Optional)") },
                        placeholder = { Text("bcf://app/orders/{orderId}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddScreen(newScreenTitle, newScreenArgs, newScreenDeepLink)
                    newScreenTitle = ""; newScreenArgs = ""; newScreenDeepLink = ""
                    showAddScreenDialog = false
                }) { Text("Add Screen") }
            },
            dismissButton = { TextButton(onClick = { showAddScreenDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAiScreenDialog) {
        AlertDialog(
            onDismissRequest = { showAiScreenDialog = false },
            title = { Text("AI Populate '${currentScreen?.title ?: "Screen"}'") },
            text = {
                OutlinedTextField(
                    value = aiScreenPrompt,
                    onValueChange = { aiScreenPrompt = it },
                    label = { Text("Describe components for this screen") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAiPopulateScreen(aiScreenPrompt); aiScreenPrompt = ""; showAiScreenDialog = false },
                    enabled = aiScreenPrompt.isNotBlank()
                ) { Text("Generate Layout") }
            },
            dismissButton = { TextButton(onClick = { showAiScreenDialog = false }) { Text("Cancel") } }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Theme Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemePreset.entries.forEach { preset ->
                        Surface(
                            onClick = { onUpdateTheme(preset); showThemeDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            color = if (preset == project.themePreset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(preset.primaryHex)))
                                Text(preset.displayName, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun ComponentInspectorPanel(
    component: AppComponent,
    availableRoutes: List<String>,
    onUpdate: (AppComponent) -> Unit,
    onOpenBlocks: () -> Unit,
    onDismiss: () -> Unit
) {
    var inspectorTab by remember(component.componentId) { mutableStateOf(0) }
    var text by remember(component.componentId) { mutableStateOf(component.text) }
    var subtitle by remember(component.componentId) { mutableStateOf(component.subtitle) }
    var actionTarget by remember(component.componentId) { mutableStateOf(component.actionTarget) }
    var dataBinding by remember(component.componentId) { mutableStateOf(component.dataBinding) }
    var paddingText by remember(component.componentId) { mutableStateOf("${component.paddingDp}") }
    var radiusText by remember(component.componentId) { mutableStateOf("${component.cornerRadiusDp}") }
    var marginText by remember(component.componentId) { mutableStateOf("${component.marginDp}") }
    var elevationText by remember(component.componentId) { mutableStateOf("${component.elevationDp}") }
    var widthMode by remember(component.componentId) { mutableStateOf(component.widthMode) }
    var gravity by remember(component.componentId) { mutableStateOf(component.gravity) }
    var colorRole by remember(component.componentId) { mutableStateOf(component.colorRole) }
    var action by remember(component.componentId) { mutableStateOf(component.action) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Inspector • ${component.type.displayName}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            onUpdate(
                                component.copy(
                                    text = text,
                                    subtitle = subtitle,
                                    paddingDp = paddingText.toIntOrNull()?.coerceIn(0, 48) ?: component.paddingDp,
                                    cornerRadiusDp = radiusText.toIntOrNull()?.coerceIn(0, 40) ?: component.cornerRadiusDp,
                                    marginDp = marginText.toIntOrNull()?.coerceIn(0, 40) ?: component.marginDp,
                                    elevationDp = elevationText.toIntOrNull()?.coerceIn(0, 24) ?: component.elevationDp,
                                    widthMode = widthMode,
                                    gravity = gravity,
                                    colorRole = colorRole,
                                    action = action,
                                    actionTarget = actionTarget,
                                    dataBinding = dataBinding
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Basic", "Layout", "Style", "Events").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = inspectorTab == idx,
                        onClick = { inspectorTab = idx },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            when (inspectorTab) {
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Primary Text") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle / Values") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = dataBinding, onValueChange = { dataBinding = it }, label = { Text("Data Binding (e.g. firestore://orders/total)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                1 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = paddingText, onValueChange = { paddingText = it }, label = { Text("Pad dp") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = marginText, onValueChange = { marginText = it }, label = { Text("Margin dp") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("MATCH_PARENT", "WRAP_CONTENT").forEach { mode ->
                            FilterChip(selected = widthMode == mode, onClick = { widthMode = mode }, label = { Text(mode, style = MaterialTheme.typography.labelSmall) })
                        }
                        listOf("START", "CENTER", "END").forEach { g ->
                            FilterChip(selected = gravity == g, onClick = { gravity = g }, label = { Text("Gravity: $g", style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = radiusText, onValueChange = { radiusText = it }, label = { Text("Corner Radius dp") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = elevationText, onValueChange = { elevationText = it }, label = { Text("Elevation dp") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ColorRole.entries.forEach { role ->
                            FilterChip(selected = colorRole == role, onClick = { colorRole = role }, label = { Text(role.label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
                else -> {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ComponentAction.entries.forEach { act ->
                            FilterChip(selected = action == act, onClick = { action = act }, label = { Text(act.label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = actionTarget,
                            onValueChange = { actionTarget = it },
                            label = { Text("Target (${availableRoutes.joinToString("/")})") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = onOpenBlocks,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Extension, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Blocks")
                        }
                    }
                }
            }
        }
    }
}
