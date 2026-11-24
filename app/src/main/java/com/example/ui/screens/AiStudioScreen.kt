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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AgentStep
import com.example.model.AgentStepStatus
import com.example.model.AiGenerationMode
import com.example.model.AiProviderSettings
import com.example.model.AppProject
import com.example.model.AppThemePreset
import com.example.model.ArchitecturePattern
import com.example.model.ChatMessage
import com.example.model.PendingFilePatch
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.AiTaskState

@Composable
fun AiStudioScreen(
    activeProject: AppProject?,
    aiState: AiTaskState,
    providerSettings: AiProviderSettings,
    agentSteps: List<AgentStep>,
    pendingPatches: List<PendingFilePatch>,
    chatMessages: List<ChatMessage>,
    onRunAiTask: (String, AiGenerationMode, ArchitecturePattern, AppThemePreset) -> Unit,
    onApplyPatches: () -> Unit,
    onDiscardPatches: () -> Unit,
    onSendAssistantMessage: (String) -> Unit
) {
    var modeTab by remember { mutableIntStateOf(0) }
    var appPrompt by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(AiGenerationMode.FULL_STACK) }
    var selectedArch by remember { mutableStateOf(ArchitecturePattern.CLEAN_MVVM) }
    var selectedTheme by remember { mutableStateOf(AppThemePreset.OBSIDIAN_CYAN) }
    var chatInput by remember { mutableStateOf("") }

    val quickPrompts = remember {
        listOf(
            "Complete e-commerce Android app with Firebase auth, product catalog, cart, checkout, order history & admin UI",
            "Multi-screen CRM with client pipeline, analytics chart, invoice generator & Node.js REST backend",
            "Add dark/light theme toggle & CSV export button to active project",
            "Generate Robolectric unit tests for active ViewModel and Repository"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = modeTab) {
            Tab(selected = modeTab == 0, onClick = { modeTab = 0 }, text = { Text("Prompt-to-App & Agent") })
            Tab(selected = modeTab == 1, onClick = { modeTab = 1 }, text = { Text("AI Architect Chat") })
        }

        if (modeTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode & Provider Card
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text("BCF AI Prompt-to-App & Project Agent", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${providerSettings.provider.displayName} • ${providerSettings.modelName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text("AI Generation Mode (13)", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AiGenerationMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = selectedMode == mode,
                                        onClick = { selectedMode = mode },
                                        label = { Text(mode.label) }
                                    )
                                }
                            }
                            Text(
                                text = selectedMode.description + if (selectedMode.targetsExistingProject && activeProject != null) " (Targets '${activeProject.name}')" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = appPrompt,
                                onValueChange = { appPrompt = it },
                                label = { Text("Instruction or App Specification") },
                                placeholder = { Text("Describe the full app or targeted incremental feature...") },
                                minLines = 3,
                                maxLines = 7,
                                modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickPrompts.forEach { sample ->
                                    FilterChip(
                                        selected = appPrompt == sample,
                                        onClick = { appPrompt = sample },
                                        label = { Text(sample.take(30) + "...", style = MaterialTheme.typography.labelMedium) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Agent Steps Progress
                if (agentSteps.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("AI Project Agent Pipeline", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                                agentSteps.forEach { step ->
                                    val dotColor = when (step.status) {
                                        AgentStepStatus.DONE -> MaterialTheme.colorScheme.tertiary
                                        AgentStepStatus.RUNNING -> MaterialTheme.colorScheme.primary
                                        AgentStepStatus.REQUIRES_CONFIRM -> Color(0xFFF59E0B)
                                        AgentStepStatus.FAILED -> MaterialTheme.colorScheme.error
                                        AgentStepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                                        Text("${step.phase}: ${step.detail}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Reviewable Incremental File Patches
                if (pendingPatches.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Review Targeted File Patches (${pendingPatches.size})", style = MaterialTheme.typography.titleSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                pendingPatches.forEach { patch ->
                                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(patch.path, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            Text(patch.summary, style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = patch.newContent.lines().take(5).joinToString("\n"),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = onApplyPatches, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Apply Patches")
                                    }
                                    OutlinedButton(onClick = onDiscardPatches, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Discard")
                                    }
                                }
                            }
                        }
                    }
                }

                if (!selectedMode.targetsExistingProject || activeProject == null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Code Architecture Pattern", style = MaterialTheme.typography.titleSmall)
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ArchitecturePattern.entries.forEach { arch ->
                                        FilterChip(selected = selectedArch == arch, onClick = { selectedArch = arch }, label = { Text(arch.displayName) })
                                    }
                                }
                                Text("Theme System", style = MaterialTheme.typography.titleSmall)
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppThemePreset.entries.forEach { preset ->
                                        FilterChip(
                                            selected = selectedTheme == preset,
                                            onClick = { selectedTheme = preset },
                                            leadingIcon = { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(preset.primaryHex))) },
                                            label = { Text(preset.displayName) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val isBusy = aiState is AiTaskState.Working
                    Button(
                        onClick = { onRunAiTask(appPrompt, selectedMode, selectedArch, selectedTheme) },
                        enabled = !isBusy && appPrompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("generate_fullstack_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("AI Agent Running...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Execute ${selectedMode.label}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (activeProject != null) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Psychology, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Context: ${activeProject.name} (${activeProject.screens.size} screens, ${activeProject.files.size} files)", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(chatMessages, key = { it.id }) { msg ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (msg.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth(0.88f)
                            ) {
                                Text(msg.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                            }
                        }
                    }
                }

                Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Ask AI Architect about Kotlin, Gradle, Firebase rules...") },
                            modifier = Modifier.weight(1f).testTag("ai_chat_input"),
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp)
                        )
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    onSendAssistantMessage(chatInput)
                                    chatInput = ""
                                }
                            },
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).testTag("ai_chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send prompt", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}
