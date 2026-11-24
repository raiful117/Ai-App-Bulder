package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiReviewReport
import com.example.model.AppComponent
import com.example.model.AppProject
import com.example.ui.components.PreviewComponentRenderer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.PreviewRuntimeState

@Composable
fun PreviewReviewScreen(
    project: AppProject?,
    previewState: PreviewRuntimeState,
    reviewReport: AiReviewReport?,
    aiState: AiTaskState,
    onSelectPreviewScreen: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleTablet: () -> Unit,
    onToggleXray: () -> Unit,
    onComponentTap: (AppProject, AppComponent) -> Unit,
    onInputChange: (String, String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit,
    onSliderChange: (String, Float) -> Unit,
    onChipSelect: (String, String) -> Unit,
    onRunAiReview: () -> Unit,
    onRunCreateBlocks: (AppProject) -> Unit = {},
    onDismissPreviewDialog: () -> Unit = {},
    onClearPreviewToast: () -> Unit = {},
    onClearLogcat: () -> Unit = {}
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a project to preview and review.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val currentScreen = project.screens.firstOrNull { it.screenId == previewState.currentScreenId }
        ?: project.screens.firstOrNull()
    var logLevelFilter by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preview Controls Bar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        project.screens.forEach { scr ->
                            FilterChip(
                                selected = scr.screenId == currentScreen?.screenId,
                                onClick = { onSelectPreviewScreen(scr.screenId) },
                                label = { Text(scr.title) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onRunCreateBlocks(project) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("run_oncreate_preview_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Run onCreate")
                        }
                        OutlinedButton(
                            onClick = onToggleDarkMode,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (previewState.darkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (previewState.darkMode) "Dark" else "Light")
                        }
                        OutlinedButton(
                            onClick = onToggleTablet,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (previewState.tabletFrame) Icons.Filled.TabletAndroid else Icons.Filled.PhoneAndroid,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (previewState.tabletFrame) "Tablet" else "Phone")
                        }
                        OutlinedButton(
                            onClick = onToggleXray,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.GridOn, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (previewState.xrayMode) "X-Ray On" else "X-Ray")
                        }
                        Button(
                            onClick = onRunAiReview,
                            enabled = aiState !is AiTaskState.Working,
                            modifier = Modifier.testTag("run_ai_review_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (aiState is AiTaskState.Working) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("AI App Review")
                        }
                    }
                }
            }
        }

        // Interactive Device Viewport Frame
        item {
            val frameBg = if (previewState.darkMode) Color(0xFF0A0F1D) else Color(0xFFF8FAFC)
            val bezelColor = if (previewState.darkMode) Color(0xFF283556) else Color(0xFFCBD5E1)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = if (previewState.tabletFrame) 620.dp else 380.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = frameBg,
                    border = BorderStroke(3.dp, bezelColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Simulated Android Status Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "9:41",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (previewState.darkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                            Box(
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bezelColor)
                            )
                            Text(
                                text = "${project.activeVersion} • 5G",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(project.themePreset.primaryHex)
                            )
                        }

                        if (currentScreen == null || currentScreen.components.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No components on this screen yet.",
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        } else {
                            currentScreen.components.forEach { comp ->
                                PreviewComponentRenderer(
                                    component = comp,
                                    themePreset = project.themePreset,
                                    darkSurface = previewState.darkMode,
                                    interactive = true,
                                    xrayMode = previewState.xrayMode,
                                    runtimeState = previewState,
                                    onComponentTap = { onComponentTap(project, comp) },
                                    onInputChange = { onInputChange(comp.componentId, it) },
                                    onSwitchChange = { onSwitchChange(comp.componentId, it) },
                                    onSliderChange = { onSliderChange(comp.componentId, it) },
                                    onChipSelect = { onChipSelect(comp.componentId, it) },
                                    onBottomNavTabTap = { tabName ->
                                        val match = project.screens.firstOrNull {
                                            it.title.equals(tabName, ignoreCase = true) ||
                                                it.route.equals(tabName, ignoreCase = true)
                                        }
                                        if (match != null) onSelectPreviewScreen(match.screenId)
                                    }
                                )
                            }
                        }

                        AnimatedVisibility(visible = !previewState.activeToast.isNullOrBlank()) {
                            Surface(
                                onClick = onClearPreviewToast,
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(project.themePreset.primaryHex)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Toast: ${previewState.activeToast.orEmpty()} (tap to dismiss)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Runtime Variables & SharedPreferences Inspector
        if (previewState.variables.isNotEmpty() || previewState.sharedPrefs.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Runtime Variables & SharedPreferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (previewState.variables.isNotEmpty()) {
                            previewState.variables.entries.take(8).forEach { (k, v) ->
                                Text("var $k = \"$v\"", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (previewState.sharedPrefs.isNotEmpty()) {
                            previewState.sharedPrefs.entries.take(6).forEach { (k, v) ->
                                Text("prefs[\"$k\"] = \"$v\"", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }

        // Live Logcat & Runtime Trace Console
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0B0F19),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Terminal, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Logcat Console", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        }
                        IconButton(onClick = onClearLogcat, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.DeleteSweep, "Clear logcat", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL", "INFO", "DEBUG", "WARN").forEach { lvl ->
                            FilterChip(
                                selected = logLevelFilter == lvl,
                                onClick = { logLevelFilter = lvl },
                                label = { Text(lvl, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    val visibleLogs = previewState.logcatEntries.filter {
                        logLevelFilter == "ALL" || it.contains("[$logLevelFilter/")
                    }
                    visibleLogs.take(12).forEach { line ->
                        val logColor = when {
                            line.contains("[WARN/") -> Color(0xFFF59E0B)
                            line.contains("[DEBUG/") -> Color(0xFF38BDF8)
                            else -> Color(0xFFA7F3D0)
                        }
                        Text(
                            text = line,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = logColor
                        )
                    }
                }
            }
        }

        // AI App Review Scorecard
        if (reviewReport != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Verified, null, tint = MaterialTheme.colorScheme.tertiary)
                                Text("Gemini App Review Audit", style = MaterialTheme.typography.titleMedium)
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${reviewReport.overallScore}/100",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScoreBadge("UI Polish", reviewReport.uiPolishScore, Modifier.weight(1f))
                            ScoreBadge("Security", reviewReport.securityScore, Modifier.weight(1f))
                            ScoreBadge("Scalability", reviewReport.scalabilityScore, Modifier.weight(1f))
                        }

                        Text(reviewReport.summary, style = MaterialTheme.typography.bodyMedium)

                        if (reviewReport.strengths.isNotEmpty()) {
                            Text("Strengths", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                            reviewReport.strengths.forEach { s ->
                                Text("• $s", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (reviewReport.recommendations.isNotEmpty()) {
                            Text("Next Recommendations", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            reviewReport.recommendations.forEach { r ->
                                Text("→ $r", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    val dialog = previewState.activeDialog
    if (dialog != null) {
        AlertDialog(
            onDismissRequest = onDismissPreviewDialog,
            title = { Text(dialog.first.ifBlank { "Alert" }) },
            text = { Text(dialog.second) },
            confirmButton = {
                Button(onClick = onDismissPreviewDialog) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismissPreviewDialog) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ScoreBadge(label: String, score: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$score", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
