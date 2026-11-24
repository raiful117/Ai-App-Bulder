package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppProject
import com.example.model.BlockCategory
import com.example.model.BlockCompiler
import com.example.model.BlockOpType
import com.example.model.ComponentType
import com.example.model.LogicBlock
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun BlocksEditorScreen(
    project: AppProject?,
    activeScreenId: String?,
    selectedWidgetId: String = "",
    selectedEvent: String = "onCreate",
    onSelectScreen: (String) -> Unit,
    onSelectBlockTarget: (String, String) -> Unit = { _, _ -> },
    onAddBlock: (BlockOpType) -> Unit,
    onGenerateBlocksWithAi: (String) -> Unit = {},
    onUpdateBlock: (LogicBlock) -> Unit,
    onMoveBlock: (String, Int) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onCompileBlocks: () -> Unit,
    onViewCompiledCode: (() -> Unit)? = null,
    onRunInPreview: (() -> Unit)? = null
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select or create a project to build visual logic blocks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val currentScreen = project.screens.firstOrNull { it.screenId == activeScreenId } ?: project.screens.firstOrNull()
    var showSidePalette by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<BlockCategory?>(null) }
    var showAllTargets by remember { mutableStateOf(false) }
    var showKotlinPreview by remember { mutableStateOf(false) }
    var aiBlockPrompt by remember { mutableStateOf("") }

    val screenWidgets = currentScreen?.components.orEmpty()
    val activeWidget = screenWidgets.firstOrNull { it.componentId == selectedWidgetId }
    val widgetLabelMap = remember(screenWidgets, project.backend.sketchComponents, project.backend.customMoreBlocks) {
        buildMap {
            screenWidgets.forEach { comp ->
                put(comp.componentId, comp.text.ifBlank { comp.type.displayName })
            }
            project.backend.sketchComponents.forEach { comp ->
                put("comp:${comp.componentId}", "${comp.name} (${comp.type.displayName})")
            }
            project.backend.customMoreBlocks.forEach { mb ->
                put("moreblock:${mb.name}", "fun ${mb.name}()")
            }
        }
    }

    val compiledKotlinPreview = remember(
        project.packageName,
        currentScreen?.screenId,
        currentScreen?.logicBlocks,
        project.backend.sketchComponents,
        project.backend.customMoreBlocks
    ) {
        if (currentScreen == null) ""
        else BlockCompiler.compileScreenBlocksToKotlin(
            packageName = project.packageName,
            screen = currentScreen,
            sketchComponents = project.backend.sketchComponents,
            customMoreBlocks = project.backend.customMoreBlocks
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Screen, Widget Event Target & Action Toolbar
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    project.screens.forEach { scr ->
                        FilterChip(
                            selected = scr.screenId == currentScreen?.screenId,
                            onClick = { onSelectScreen(scr.screenId) },
                            label = { Text("${scr.title} (${scr.logicBlocks.size})") }
                        )
                    }
                }

                // Event / Widget Binding Target Selector (Sketchware Events bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedWidgetId.isBlank() && !showAllTargets,
                        onClick = {
                            showAllTargets = false
                            onSelectBlockTarget("", "onCreate")
                        },
                        leadingIcon = { Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Screen onCreate", style = MaterialTheme.typography.labelSmall) }
                    )
                    screenWidgets.forEach { comp ->
                        val defaultEv = when (comp.type) {
                            ComponentType.TEXT_INPUT,
                            ComponentType.PASSWORD_FIELD,
                            ComponentType.SLIDER_CONTROL -> "onTextChange"
                            ComponentType.SWITCH_TOGGLE,
                            ComponentType.CHECKBOX,
                            ComponentType.RADIO_BUTTON -> "onCheckedChange"
                            else -> "onClick"
                        }
                        val count = currentScreen?.logicBlocks?.count { it.targetWidgetId == comp.componentId } ?: 0
                        val labelText = comp.text.ifBlank { comp.type.displayName }.take(18)
                        FilterChip(
                            selected = selectedWidgetId == comp.componentId && !showAllTargets,
                            onClick = {
                                showAllTargets = false
                                onSelectBlockTarget(comp.componentId, defaultEv)
                            },
                            label = {
                                Text(
                                    text = if (count > 0) "$labelText ($count)" else labelText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    project.backend.customMoreBlocks.forEach { mb ->
                        val targetId = "moreblock:${mb.name}"
                        val count = currentScreen?.logicBlocks?.count { it.targetWidgetId == targetId } ?: 0
                        FilterChip(
                            selected = selectedWidgetId == targetId && !showAllTargets,
                            onClick = {
                                showAllTargets = false
                                onSelectBlockTarget(targetId, "onInvoke")
                            },
                            leadingIcon = { Icon(Icons.Filled.Functions, null, modifier = Modifier.size(14.dp)) },
                            label = {
                                Text(
                                    text = if (count > 0) "${mb.name}() ($count)" else "${mb.name}()",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    FilterChip(
                        selected = showAllTargets,
                        onClick = { showAllTargets = !showAllTargets },
                        label = { Text("Show All", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                if (activeWidget != null && !showAllTargets) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Event:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf("onClick", "onLongClick", "onTextChange", "onCheckedChange").forEach { ev ->
                            FilterChip(
                                selected = selectedEvent == ev,
                                onClick = { onSelectBlockTarget(activeWidget.componentId, ev) },
                                label = { Text(ev, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // AI Block Architect Prompt Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = aiBlockPrompt,
                        onValueChange = { aiBlockPrompt = it },
                        placeholder = { Text("AI Block Architect (English or Bangla): e.g. Save token & open dashboard", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("ai_block_prompt_input")
                    )
                    Button(
                        onClick = {
                            if (aiBlockPrompt.isNotBlank()) {
                                onGenerateBlocksWithAi(aiBlockPrompt)
                                aiBlockPrompt = ""
                            }
                        },
                        enabled = aiBlockPrompt.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("ai_generate_blocks_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Blocks", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSidePalette = !showSidePalette },
                        modifier = Modifier.testTag("toggle_blocks_palette_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ViewSidebar,
                            null,
                            tint = if (showSidePalette) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (showSidePalette) "Hide Blocks" else "Show Blocks", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = {
                            onCompileBlocks()
                            if (onViewCompiledCode != null) showKotlinPreview = true
                        },
                        modifier = Modifier.testTag("compile_blocks_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Compile to Kotlin", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { showKotlinPreview = !showKotlinPreview },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (showKotlinPreview) "Block Stack" else "Kotlin Preview", style = MaterialTheme.typography.labelMedium)
                    }
                    if (onRunInPreview != null) {
                        OutlinedButton(
                            onClick = onRunInPreview,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Run Preview", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (onViewCompiledCode != null) {
                        OutlinedButton(
                            onClick = {
                                onCompileBlocks()
                                onViewCompiledCode()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open in Code IDE", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (showKotlinPreview) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0D1117),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = compiledKotlinPreview,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFE6EDF3)
                    )
                }
            }
        } else {
            // Split View: Side Block Palette (Left) + Visual Block Stack (Right)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWide = maxWidth >= 680.dp
                val paletteWidth = if (isWide) 232.dp else 142.dp

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
                                Text(
                                    text = if (activeWidget != null) {
                                        "+ ADD TO ${activeWidget.text.ifBlank { activeWidget.type.displayName }.take(12).uppercase()}"
                                    } else {
                                        "+ ADD TO ONCREATE"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    BlockCategory.entries.forEach { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { selectedCategory = cat },
                                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                val filteredOps = remember(selectedCategory) {
                                    BlockOpType.entries.filter { selectedCategory == null || it.category == selectedCategory }
                                }
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredOps, key = { it.name }) { op ->
                                        val accent = Color(op.category.accentHex)
                                        Surface(
                                            onClick = { onAddBlock(op) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                                            modifier = Modifier.testTag("palette_block_${op.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(24.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(accent)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = op.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = op.category.label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = accent
                                                    )
                                                }
                                                Icon(Icons.Filled.Add, "Add ${op.title}", tint = accent, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Visual Block Stack Canvas (Right)
                    val allBlocks = currentScreen?.logicBlocks.orEmpty()
                    val visibleBlocks = if (showAllTargets) {
                        allBlocks
                    } else if (selectedWidgetId.isBlank()) {
                        allBlocks.filter { it.targetWidgetId.isBlank() || it.eventTrigger == "onCreate" }
                    } else {
                        allBlocks.filter { it.targetWidgetId == selectedWidgetId }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (visibleBlocks.isEmpty()) {
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
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Extension, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(34.dp))
                                        Text(
                                            text = if (activeWidget != null) {
                                                "No blocks for '${activeWidget.text.ifBlank { activeWidget.type.displayName }}'"
                                            } else {
                                                "Visual Logic Blocks Workspace"
                                            },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Tap any block in the left palette to attach it to ${if (activeWidget != null) "$selectedEvent of '${activeWidget.text}'" else "Screen onCreate"}, then compile directly into Kotlin.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(visibleBlocks, key = { it.blockId }) { block ->
                                val boundLabel = if (block.targetWidgetId.isBlank()) {
                                    "Screen • ${block.eventTrigger}"
                                } else {
                                    "${widgetLabelMap[block.targetWidgetId] ?: block.targetWidgetId.takeLast(6)} • ${block.eventTrigger}"
                                }
                                LogicBlockCard(
                                    block = block,
                                    boundTargetLabel = boundLabel,
                                    onUpdate = onUpdateBlock,
                                    onMoveUp = { onMoveBlock(block.blockId, -1) },
                                    onMoveDown = { onMoveBlock(block.blockId, 1) },
                                    onDelete = { onDeleteBlock(block.blockId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogicBlockCard(
    block: LogicBlock,
    boundTargetLabel: String,
    onUpdate: (LogicBlock) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = Color(block.opType.category.accentHex)
    var arg1 by remember(block.blockId, block.arg1) { mutableStateOf(block.arg1) }
    var arg2 by remember(block.blockId, block.arg2) { mutableStateOf(block.arg2) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, accent.copy(alpha = if (block.enabled) 0.65f else 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = accent.copy(alpha = 0.2f)) {
                            Text(
                                text = block.opType.category.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text = boundTargetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(block.opType.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = block.enabled,
                    onCheckedChange = { onUpdate(block.copy(enabled = it)) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = arg1,
                    onValueChange = { arg1 = it },
                    label = { Text("Parameter / Target") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (block.opType.defaultArg2.isNotBlank() || arg2.isNotBlank()) {
                    OutlinedTextField(
                        value = arg2,
                        onValueChange = { arg2 = it },
                        label = { Text("Value / Payload") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Filled.ArrowUpward, "Move block up", modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onMoveDown) { Icon(Icons.Filled.ArrowDownward, "Move block down", modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, "Delete block", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
                OutlinedButton(
                    onClick = { onUpdate(block.copy(arg1 = arg1, arg2 = arg2)) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Block")
                }
            }
        }
    }
}
