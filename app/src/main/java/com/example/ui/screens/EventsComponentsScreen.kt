package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppComponent
import com.example.model.AppProject
import com.example.model.ComponentType
import com.example.model.CustomMoreBlock
import com.example.model.SketchComponent
import com.example.model.SketchComponentType

@Composable
fun EventsComponentsScreen(
    project: AppProject?,
    activeScreenId: String?,
    onSelectScreen: (String) -> Unit,
    onOpenBlocksForActivityEvent: (String) -> Unit,
    onOpenBlocksForWidgetEvent: (AppComponent, String) -> Unit,
    onAddSketchComponent: (String, SketchComponentType, String) -> Unit,
    onDeleteSketchComponent: (String) -> Unit,
    onOpenBlocksForSketchComponent: (SketchComponent, String) -> Unit,
    onAddCustomMoreBlock: (String, String, String) -> Unit,
    onDeleteCustomMoreBlock: (String) -> Unit,
    onOpenBlocksForMoreBlock: (CustomMoreBlock) -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a project to manage Events, Components & MoreBlocks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val screen = project.screens.firstOrNull { it.screenId == activeScreenId } ?: project.screens.firstOrNull()
    var subTab by remember { mutableIntStateOf(0) }
    var showAddComponentDialog by remember { mutableStateOf(false) }
    var showAddMoreBlockDialog by remember { mutableStateOf(false) }

    var newCompType by remember { mutableStateOf(SketchComponentType.SHARED_PREFS) }
    var newCompName by remember { mutableStateOf("spUser") }
    var newCompNote by remember { mutableStateOf("") }

    var newMbName by remember { mutableStateOf("validateOrder") }
    var newMbParams by remember { mutableStateOf("amount: Double = 0.0") }
    var newMbDesc by remember { mutableStateOf("Validates order input and updates UI") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Screen & Sub-tab Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    project.screens.forEach { scr ->
                        FilterChip(
                            selected = scr.screenId == screen?.screenId,
                            onClick = { onSelectScreen(scr.screenId) },
                            label = { Text(scr.title) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        0 to "Activity Events",
                        1 to "View Events (${screen?.components?.size ?: 0})",
                        2 to "Components (${project.backend.sketchComponents.size})",
                        3 to "MoreBlocks (${project.backend.customMoreBlocks.size})"
                    )
                    tabs.forEach { (idx, label) ->
                        FilterChip(
                            selected = subTab == idx,
                            onClick = { subTab = idx },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("events_subtab_$idx")
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (subTab) {
                0 -> {
                    val activityEvents = listOf(
                        Triple("onCreate", "Runs when screen first initializes", Color(0xFFF59E0B)),
                        Triple("onStart", "Runs when screen enters foreground", Color(0xFF38BDF8)),
                        Triple("onResume", "Runs every time user returns to screen", Color(0xFF10B981)),
                        Triple("onBackPressed", "Intercepts system back navigation", Color(0xFF8B5CF6))
                    )
                    items(activityEvents, key = { it.first }) { (eventName, desc, accent) ->
                        val blockCount = screen?.logicBlocks?.count {
                            it.targetWidgetId.isBlank() && it.eventTrigger == eventName
                        } ?: 0
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accent.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Bolt, null, tint = accent, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(eventName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Button(
                                    onClick = { onOpenBlocksForActivityEvent(eventName) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (blockCount > 0) "Blocks ($blockCount)" else "Edit Logic")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val comps = screen?.components.orEmpty()
                    if (comps.isEmpty()) {
                        item {
                            EmptyHubNotice(
                                icon = Icons.Filled.Widgets,
                                title = "No Widgets on '${screen?.title ?: "Screen"}'",
                                subtitle = "Add widgets in View Designer first to attach onClick, onLongClick, or onTextChange blocks."
                            )
                        }
                    } else {
                        items(comps, key = { it.componentId }) { comp ->
                            val eventsForType = when (comp.type) {
                                ComponentType.TEXT_INPUT, ComponentType.PASSWORD_FIELD, ComponentType.SLIDER_CONTROL ->
                                    listOf("onTextChange", "onClick")
                                ComponentType.SWITCH_TOGGLE, ComponentType.CHECKBOX, ComponentType.RADIO_BUTTON, ComponentType.CHIP_GROUP ->
                                    listOf("onCheckedChange", "onClick")
                                else -> listOf("onClick", "onLongClick")
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${comp.text.ifBlank { comp.type.displayName }} (${comp.type.displayName})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        eventsForType.forEach { ev ->
                                            val count = screen?.logicBlocks?.count {
                                                it.targetWidgetId == comp.componentId && it.eventTrigger == ev
                                            } ?: 0
                                            OutlinedButton(
                                                onClick = { onOpenBlocksForWidgetEvent(comp, ev) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Extension, null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (count > 0) "$ev ($count)" else ev)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Non-UI Sketchware Components", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showAddComponentDialog = true },
                                modifier = Modifier.testTag("add_sketch_component_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Component")
                            }
                        }
                    }
                    if (project.backend.sketchComponents.isEmpty()) {
                        item {
                            EmptyHubNotice(
                                icon = Icons.Filled.Layers,
                                title = "No Non-UI Components Added",
                                subtitle = "Add Intent, SharedPreferences, Timer, AlertDialog, FirebaseAuth, FirebaseDB, RequestNetwork, Camera/MediaPicker, or TextToSpeech."
                            )
                        }
                    } else {
                        items(project.backend.sketchComponents, key = { it.componentId }) { comp ->
                            val accent = Color(comp.type.accentHex)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${comp.name} : ${comp.type.displayName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                                            Text(comp.type.androidClass, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (comp.configNote.isNotBlank()) {
                                                Text(comp.configNote, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        IconButton(onClick = { onDeleteSketchComponent(comp.componentId) }) {
                                            Icon(Icons.Filled.DeleteOutline, "Delete component", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        comp.type.events.forEach { ev ->
                                            val count = screen?.logicBlocks?.count {
                                                it.targetWidgetId == "comp:${comp.componentId}" && it.eventTrigger == ev
                                            } ?: 0
                                            OutlinedButton(
                                                onClick = { onOpenBlocksForSketchComponent(comp, ev) },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(if (count > 0) "$ev ($count)" else ev)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Custom Reusable MoreBlocks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showAddMoreBlockDialog = true },
                                modifier = Modifier.testTag("add_more_block_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New MoreBlock")
                            }
                        }
                    }
                    if (project.backend.customMoreBlocks.isEmpty()) {
                        item {
                            EmptyHubNotice(
                                icon = Icons.Filled.Functions,
                                title = "No Custom MoreBlocks",
                                subtitle = "Create reusable helper functions callable from any button or event using the 'Call Custom MoreBlock' logic block."
                            )
                        }
                    } else {
                        items(project.backend.customMoreBlocks, key = { it.blockId }) { mb ->
                            val count = screen?.logicBlocks?.count {
                                it.targetWidgetId == "moreblock:${mb.name}" || it.targetWidgetId == mb.blockId
                            } ?: 0
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "fun ${mb.name}(${mb.paramsSignature})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE11D48)
                                        )
                                        if (mb.description.isNotBlank()) {
                                            Text(mb.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { onOpenBlocksForMoreBlock(mb) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(if (count > 0) "Body ($count)" else "Edit Body")
                                        }
                                        IconButton(onClick = { onDeleteCustomMoreBlock(mb.blockId) }) {
                                            Icon(Icons.Filled.DeleteOutline, "Delete MoreBlock", tint = MaterialTheme.colorScheme.error)
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

    if (showAddComponentDialog) {
        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text("Add Sketchware Component") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SketchComponentType.entries.forEach { t ->
                            FilterChip(
                                selected = newCompType == t,
                                onClick = {
                                    newCompType = t
                                    newCompName = t.displayName.lowercase().replace(Regex("[^a-z0-9]"), "").take(14)
                                },
                                label = { Text(t.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newCompName,
                        onValueChange = { newCompName = it },
                        label = { Text("Variable Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCompNote,
                        onValueChange = { newCompNote = it },
                        label = { Text("Config / Key / Path (Optional)") },
                        placeholder = { Text("e.g. prefs_name or collection path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddSketchComponent(newCompName, newCompType, newCompNote)
                    showAddComponentDialog = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddComponentDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddMoreBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddMoreBlockDialog = false },
            title = { Text("Create Custom MoreBlock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newMbName,
                        onValueChange = { newMbName = it },
                        label = { Text("Function Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newMbParams,
                        onValueChange = { newMbParams = it },
                        label = { Text("Parameters Signature") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newMbDesc,
                        onValueChange = { newMbDesc = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddCustomMoreBlock(newMbName, newMbParams, newMbDesc)
                    showAddMoreBlockDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAddMoreBlockDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EmptyHubNotice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
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
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
