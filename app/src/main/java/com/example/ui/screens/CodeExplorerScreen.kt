package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppProject
import com.example.model.CodeLayer
import com.example.model.ProjectFile
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.AiTaskState

@Composable
fun CodeExplorerScreen(
    project: AppProject?,
    selectedFileId: String?,
    aiState: AiTaskState,
    editorFontSizeSp: Int,
    onSelectFile: (String) -> Unit,
    onSaveFileContent: (String, String) -> Unit,
    onAddCodeFile: (String, String, CodeLayer) -> Unit,
    onDeleteCodeFile: (String) -> Unit,
    onAiRefactorFile: (ProjectFile, String) -> Unit,
    onAiExplainFile: (ProjectFile) -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a project to manage code structure.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val context = LocalContext.current
    var activeLayer by remember { mutableStateOf<CodeLayer?>(null) }
    var globalSearch by remember { mutableStateOf("") }
    val filteredFiles = project.files.filter { file ->
        val matchesLayer = activeLayer == null || file.layer == activeLayer
        val matchesSearch = globalSearch.isBlank() ||
            file.path.contains(globalSearch, ignoreCase = true) ||
            file.content.contains(globalSearch, ignoreCase = true)
        matchesLayer && matchesSearch
    }
    val currentFile = project.files.firstOrNull { it.fileId == selectedFileId }
        ?: filteredFiles.firstOrNull()
        ?: project.files.firstOrNull()

    var isEditing by remember(currentFile?.fileId) { mutableStateOf(false) }
    var editableContent by remember(currentFile?.fileId, currentFile?.content) { mutableStateOf(currentFile?.content.orEmpty()) }
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var aiInstruction by remember { mutableStateOf("") }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFilePath by remember { mutableStateOf("") }
    var newFileLayer by remember { mutableStateOf(CodeLayer.UI) }
    var copiedNotice by remember { mutableStateOf(false) }

    val snippets = remember {
        listOf(
            "@Composable" to "\n@Composable\nfun NewSection(modifier: Modifier = Modifier) {\n    // TODO\n}\n",
            "StateFlow" to "\nprivate val _uiState = MutableStateFlow(Unit)\nval uiState = _uiState.asStateFlow()\n",
            "Express Route" to "\nrouter.get('/items', async (req, res) => {\n  res.json({ ok: true });\n});\n"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("${project.architecture.displayName} • ${filteredFiles.size}/${project.files.size} files", style = MaterialTheme.typography.titleSmall)
                    }
                    OutlinedButton(onClick = { showNewFileDialog = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New File", style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedTextField(
                    value = globalSearch,
                    onValueChange = { globalSearch = it },
                    leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                    placeholder = { Text("Find in files (path or code)...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = activeLayer == null, onClick = { activeLayer = null }, label = { Text("All") })
                    CodeLayer.entries.forEach { layer ->
                        FilterChip(selected = activeLayer == layer, onClick = { activeLayer = layer }, label = { Text(layer.label) })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredFiles.forEach { file ->
                        FilterChip(
                            selected = file.fileId == currentFile?.fileId,
                            onClick = { onSelectFile(file.fileId) },
                            label = { Text(file.path.substringAfterLast('/'), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        if (currentFile != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentFile.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    Row {
                        IconButton(onClick = { onAiExplainFile(currentFile) }) {
                            Icon(Icons.Filled.Psychology, "AI Explain file", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { showFindReplace = !showFindReplace }) {
                            Icon(Icons.Filled.FindReplace, "Find and Replace", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText(currentFile.path, currentFile.content))
                            copiedNotice = true
                        }) {
                            Icon(if (copiedNotice) Icons.Filled.Check else Icons.Filled.ContentCopy, "Copy file code", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            if (isEditing) {
                                onSaveFileContent(currentFile.fileId, editableContent)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        }) {
                            Icon(
                                if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                                if (isEditing) "Save code" else "Edit code",
                                tint = if (isEditing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (project.files.size > 1) {
                            IconButton(onClick = { onDeleteCodeFile(currentFile.fileId) }) {
                                Icon(Icons.Filled.DeleteOutline, "Delete code file", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (showFindReplace) {
                Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = findText, onValueChange = { findText = it }, label = { Text("Find") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = replaceText, onValueChange = { replaceText = it }, label = { Text("Replace") }, singleLine = true, modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (findText.isNotBlank()) {
                                    val updated = currentFile.content.replace(findText, replaceText)
                                    editableContent = updated
                                    onSaveFileContent(currentFile.fileId, updated)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("All") }
                    }
                }
            }

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    snippets.forEach { (label, code) ->
                        FilterChip(selected = false, onClick = { editableContent += code }, label = { Text("+ $label", style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editableContent,
                        onValueChange = { editableContent = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = editorFontSizeSp.sp, lineHeight = (editorFontSizeSp + 6).sp),
                        shape = RoundedCornerShape(14.dp)
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        val lines = currentFile.content.lines()
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            lines.forEachIndexed { idx, line ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "${idx + 1}".padStart(3, ' '),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = (editorFontSizeSp - 1).coerceAtLeast(9).sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = line,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = editorFontSizeSp.sp,
                                        color = when {
                                            line.trimStart().startsWith("//") || line.trimStart().startsWith("#") -> MaterialTheme.colorScheme.onSurfaceVariant
                                            line.contains("fun ") || line.contains("class ") || line.contains("export ") || line.contains("allow ") -> MaterialTheme.colorScheme.primary
                                            line.contains("import ") || line.contains("package ") -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = aiInstruction,
                        onValueChange = { aiInstruction = it },
                        placeholder = { Text("AI instruction for ${currentFile.path.substringAfterLast('/')}...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("ai_refactor_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = { onAiRefactorFile(currentFile, aiInstruction); aiInstruction = "" },
                        enabled = aiInstruction.isNotBlank() && aiState !is AiTaskState.Working,
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Edit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Add Project File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newFilePath, onValueChange = { newFilePath = it }, label = { Text("Relative Path") }, placeholder = { Text("app/src/main/java/com/bcf/OrdersRepo.kt") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CodeLayer.entries.forEach { layer ->
                            FilterChip(selected = newFileLayer == layer, onClick = { newFileLayer = layer }, label = { Text(layer.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lang = when {
                        newFilePath.endsWith(".ts") || newFilePath.endsWith(".js") -> "typescript"
                        newFilePath.endsWith(".sql") -> "sql"
                        newFilePath.endsWith(".rules") -> "rules"
                        newFilePath.endsWith(".json") -> "json"
                        else -> "kotlin"
                    }
                    onAddCodeFile(newFilePath, lang, newFileLayer)
                    newFilePath = ""
                    showNewFileDialog = false
                }) { Text("Add File") }
            },
            dismissButton = { TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") } }
        )
    }
}
