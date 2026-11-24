package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiProviderSettings
import com.example.model.AiReviewReport
import com.example.model.ApiExecutionResult
import com.example.model.ApiRequestSpec
import com.example.model.AppComponent
import com.example.model.AppProject
import com.example.model.AppThemePreset
import com.example.model.AssetCategory
import com.example.model.BlockOpType
import com.example.model.BuildJobRecord
import com.example.model.BuildTarget
import com.example.model.ChatMessage
import com.example.model.CodeLayer
import com.example.model.ComponentType
import com.example.model.CustomMoreBlock
import com.example.model.DatabaseEngine
import com.example.model.DeploymentRecord
import com.example.model.DiagnosticIssue
import com.example.model.FirebaseSetupConfig
import com.example.model.LogicBlock
import com.example.model.PendingFilePatch
import com.example.model.ProjectFile
import com.example.model.ScalingTier
import com.example.model.SketchComponent
import com.example.model.SketchComponentType
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.PreviewRuntimeState
import com.example.viewmodel.UiState
import java.io.OutputStream

enum class WorkspaceSubMode(val label: String, val icon: ImageVector) {
    SPLIT_IDE("Code + Tree + AI", Icons.Filled.Code),
    DESIGNER("View Designer", Icons.Filled.Widgets),
    EVENTS("Events & MoreBlocks", Icons.Filled.Bolt),
    BLOCKS("Logic Blocks", Icons.Filled.Extension),
    PREVIEW("Preview", Icons.Filled.PlayArrow),
    PROJECT_CONFIG("Libs & Res", Icons.Filled.Tune),
    FIREBASE_DB("Firebase & DB", Icons.Filled.LocalFireDepartment),
    BACKEND_API("Backend & API", Icons.Filled.Api),
    BUILD_EXPORT("APK & Export", Icons.Filled.Build)
}

@Composable
fun ProjectWorkspaceScreen(
    project: AppProject?,
    activeScreenId: String?,
    selectedComponentId: String?,
    selectedFileId: String?,
    selectedBlockWidgetId: String,
    selectedBlockEvent: String,
    lastAiGeneratedFiles: List<ProjectFile>,
    aiState: AiTaskState,
    providerSettings: AiProviderSettings,
    pendingPatches: List<PendingFilePatch>,
    chatMessages: List<ChatMessage>,
    diagnostics: List<DiagnosticIssue>,
    apiResult: ApiExecutionResult?,
    apiRunning: Boolean,
    deploymentsState: UiState<List<DeploymentRecord>>,
    buildWorkerStatus: String,
    buildJobs: List<BuildJobRecord>,
    previewState: PreviewRuntimeState,
    reviewReport: AiReviewReport?,
    canUndo: Boolean,
    canRedo: Boolean,
    onDismissGeneratedBanner: () -> Unit,
    onOpenAiStudio: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    // Designer actions
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
    onOpenBlocksForWidgetInWorkspace: (AppComponent) -> Unit,
    // Blocks actions
    onSelectBlockTarget: (String, String) -> Unit,
    onAddBlock: (BlockOpType) -> Unit,
    onGenerateBlocksWithAi: (String) -> Unit,
    onUpdateBlock: (LogicBlock) -> Unit,
    onMoveBlock: (String, Int) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onCompileBlocks: () -> Unit,
    // Events, Components & MoreBlocks
    onAddSketchComponent: (String, SketchComponentType, String) -> Unit,
    onDeleteSketchComponent: (String) -> Unit,
    onOpenBlocksForSketchComponent: (SketchComponent, String) -> Unit,
    onAddCustomMoreBlock: (String, String, String) -> Unit,
    onDeleteCustomMoreBlock: (String) -> Unit,
    onOpenBlocksForMoreBlock: (CustomMoreBlock) -> Unit,
    // Project Config (Libs, Permissions, Resources)
    onToggleProjectLibrary: (String) -> Unit,
    onToggleProjectPermission: (String) -> Unit,
    onUpsertStringResource: (String, String) -> Unit,
    onRemoveStringResource: (String) -> Unit,
    onUpsertColorResource: (String, String) -> Unit,
    onRemoveColorResource: (String) -> Unit,
    // Code & AI actions
    onSelectFile: (String) -> Unit,
    onSaveFileContent: (String, String) -> Unit,
    onAddCodeFile: (String, String, CodeLayer) -> Unit,
    onDeleteCodeFile: (String) -> Unit,
    onAiRefactorFile: (ProjectFile, String) -> Unit,
    onAiExplainFile: (ProjectFile) -> Unit,
    onSendAssistantMessage: (String) -> Unit,
    onApplyPatches: () -> Unit,
    onDiscardPatches: () -> Unit,
    // Firebase & DB
    onUpdateFirebaseConfig: (FirebaseSetupConfig) -> Unit,
    onAddCollection: (String, String, String) -> Unit,
    onRemoveCollection: (String) -> Unit,
    onAddDatabaseTable: (DatabaseEngine, String, String, String, String) -> Unit,
    onGenerateMigration: () -> Unit,
    onDeployProject: (String, String, String) -> Unit,
    onRollbackDeployment: (String) -> Unit,
    // Backend & API
    onUpdateCloudInfra: (String, ScalingTier) -> Unit,
    onUpdateSecurityToggles: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onAddEndpoint: (String, String, String, String) -> Unit,
    onGenerateNodeBackend: () -> Unit,
    onSyncAndroidGradle: () -> Unit,
    onSaveApiRequest: (ApiRequestSpec) -> Unit,
    onExecuteApiRequest: (ApiRequestSpec) -> Unit,
    onGenerateKotlinClient: (ApiRequestSpec) -> Unit,
    // Preview
    onSelectPreviewScreen: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleTablet: () -> Unit,
    onToggleXray: () -> Unit,
    onPreviewComponentTap: (AppProject, AppComponent) -> Unit,
    onPreviewInputChange: (String, String) -> Unit,
    onPreviewSwitchChange: (String, Boolean) -> Unit,
    onPreviewSliderChange: (String, Float) -> Unit,
    onPreviewChipSelect: (String, String) -> Unit,
    onRunAiReview: () -> Unit,
    onRunCreateBlocks: (AppProject) -> Unit,
    onDismissPreviewDialog: () -> Unit,
    onClearPreviewToast: () -> Unit,
    onClearLogcat: () -> Unit,
    // Build & Export
    onRunDiagnostics: () -> Unit,
    onExportZip: (OutputStream, String, (Int) -> Unit) -> Unit,
    onTestWorkerConnection: () -> Unit,
    onTriggerRemoteBuild: (BuildTarget) -> Unit,
    onRefreshBuildJob: (BuildJobRecord) -> Unit,
    onDownloadBuildArtifact: (BuildJobRecord, OutputStream, (Result<Long>) -> Unit) -> Unit,
    onAddAsset: (String, AssetCategory, String, String) -> Unit,
    onDeleteAsset: (String) -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Text("No Active Project Loaded", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Generate a complete app in AI Studio or open an existing project from My Projects to launch the full split-pane IDE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenAiStudio) { Text("AI App Generator") }
                        OutlinedButton(onClick = onOpenProjects) { Text("My Projects") }
                    }
                }
            }
        }
        return
    }

    var activeMode by remember(project.id) { mutableStateOf(WorkspaceSubMode.SPLIT_IDE) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Unified Project IDE Bar: Everything for this project lives right here
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkspaceSubMode.entries.forEach { mode ->
                        FilterChip(
                            selected = activeMode == mode,
                            onClick = { activeMode = mode },
                            leadingIcon = { Icon(mode.icon, null, modifier = Modifier.size(15.dp)) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("workspace_tab_${mode.name.lowercase()}")
                        )
                    }
                }

                // Generated code summary banner right after AI builds an app
                AnimatedVisibility(visible = lastAiGeneratedFiles.isNotEmpty() && activeMode == WorkspaceSubMode.SPLIT_IDE) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Generated ${lastAiGeneratedFiles.size} Project Files • Tap any file to view code",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = onDismissGeneratedBanner, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, "Dismiss generated files banner", modifier = Modifier.size(14.dp))
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                lastAiGeneratedFiles.take(12).forEach { file ->
                                    FilterChip(
                                        selected = file.fileId == selectedFileId,
                                        onClick = { onSelectFile(file.fileId) },
                                        label = {
                                            Text(
                                                file.path.substringAfterLast('/'),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeMode) {
                WorkspaceSubMode.SPLIT_IDE -> SplitMonacoIdePane(
                    project = project,
                    selectedFileId = selectedFileId,
                    aiState = aiState,
                    editorFontSizeSp = providerSettings.editorFontSizeSp,
                    pendingPatches = pendingPatches,
                    chatMessages = chatMessages,
                    onSelectFile = onSelectFile,
                    onSaveFileContent = onSaveFileContent,
                    onAddCodeFile = onAddCodeFile,
                    onDeleteCodeFile = onDeleteCodeFile,
                    onAiRefactorFile = onAiRefactorFile,
                    onAiExplainFile = onAiExplainFile,
                    onSendAssistantMessage = onSendAssistantMessage,
                    onApplyPatches = onApplyPatches,
                    onDiscardPatches = onDiscardPatches
                )

                WorkspaceSubMode.DESIGNER -> VisualBuilderScreen(
                    project = project,
                    activeScreenId = activeScreenId,
                    selectedComponentId = selectedComponentId,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onSelectScreen = onSelectScreen,
                    onAddScreen = onAddScreen,
                    onDeleteScreen = onDeleteScreen,
                    onSelectComponent = onSelectComponent,
                    onAddComponent = onAddComponent,
                    onUpdateComponent = onUpdateComponent,
                    onMoveComponent = onMoveComponent,
                    onDuplicateComponent = onDuplicateComponent,
                    onDeleteComponent = onDeleteComponent,
                    onUpdateTheme = onUpdateTheme,
                    onAiPopulateScreen = onAiPopulateScreen,
                    onSyncCode = onSyncCode,
                    onOpenBlocksForWidget = { comp ->
                        onOpenBlocksForWidgetInWorkspace(comp)
                        activeMode = WorkspaceSubMode.BLOCKS
                    }
                )

                WorkspaceSubMode.EVENTS -> EventsComponentsScreen(
                    project = project,
                    activeScreenId = activeScreenId,
                    onSelectScreen = onSelectScreen,
                    onOpenBlocksForActivityEvent = { ev ->
                        onSelectBlockTarget("", ev)
                        activeMode = WorkspaceSubMode.BLOCKS
                    },
                    onOpenBlocksForWidgetEvent = { comp, ev ->
                        onSelectBlockTarget(comp.componentId, ev)
                        activeMode = WorkspaceSubMode.BLOCKS
                    },
                    onAddSketchComponent = onAddSketchComponent,
                    onDeleteSketchComponent = onDeleteSketchComponent,
                    onOpenBlocksForSketchComponent = { comp, ev ->
                        onOpenBlocksForSketchComponent(comp, ev)
                        activeMode = WorkspaceSubMode.BLOCKS
                    },
                    onAddCustomMoreBlock = onAddCustomMoreBlock,
                    onDeleteCustomMoreBlock = onDeleteCustomMoreBlock,
                    onOpenBlocksForMoreBlock = { mb ->
                        onOpenBlocksForMoreBlock(mb)
                        activeMode = WorkspaceSubMode.BLOCKS
                    }
                )

                WorkspaceSubMode.BLOCKS -> BlocksEditorScreen(
                    project = project,
                    activeScreenId = activeScreenId,
                    selectedWidgetId = selectedBlockWidgetId,
                    selectedEvent = selectedBlockEvent,
                    onSelectScreen = onSelectScreen,
                    onSelectBlockTarget = onSelectBlockTarget,
                    onAddBlock = onAddBlock,
                    onGenerateBlocksWithAi = onGenerateBlocksWithAi,
                    onUpdateBlock = onUpdateBlock,
                    onMoveBlock = onMoveBlock,
                    onDeleteBlock = onDeleteBlock,
                    onCompileBlocks = onCompileBlocks,
                    onViewCompiledCode = {
                        val scr = project.screens.firstOrNull { it.screenId == activeScreenId } ?: project.screens.firstOrNull()
                        val safeClass = scr?.title?.replace(Regex("[^A-Za-z0-9]"), "")?.ifBlank { "Main" } ?: "Main"
                        val compiledFile = project.files.firstOrNull { it.path.endsWith("${safeClass}BlockController.kt") }
                        if (compiledFile != null) onSelectFile(compiledFile.fileId)
                        activeMode = WorkspaceSubMode.SPLIT_IDE
                    },
                    onRunInPreview = {
                        onRunCreateBlocks(project)
                        activeMode = WorkspaceSubMode.PREVIEW
                    }
                )

                WorkspaceSubMode.PREVIEW -> PreviewReviewScreen(
                    project = project,
                    previewState = previewState,
                    reviewReport = reviewReport,
                    aiState = aiState,
                    onSelectPreviewScreen = onSelectPreviewScreen,
                    onToggleDarkMode = onToggleDarkMode,
                    onToggleTablet = onToggleTablet,
                    onToggleXray = onToggleXray,
                    onComponentTap = onPreviewComponentTap,
                    onInputChange = onPreviewInputChange,
                    onSwitchChange = onPreviewSwitchChange,
                    onSliderChange = onPreviewSliderChange,
                    onChipSelect = onPreviewChipSelect,
                    onRunAiReview = onRunAiReview,
                    onRunCreateBlocks = onRunCreateBlocks,
                    onDismissPreviewDialog = onDismissPreviewDialog,
                    onClearPreviewToast = onClearPreviewToast,
                    onClearLogcat = onClearLogcat
                )

                WorkspaceSubMode.PROJECT_CONFIG -> ProjectConfigScreen(
                    project = project,
                    onToggleLibrary = onToggleProjectLibrary,
                    onTogglePermission = onToggleProjectPermission,
                    onUpsertStringResource = onUpsertStringResource,
                    onRemoveStringResource = onRemoveStringResource,
                    onUpsertColorResource = onUpsertColorResource,
                    onRemoveColorResource = onRemoveColorResource,
                    onSyncGradleAndManifest = onSyncAndroidGradle
                )

                WorkspaceSubMode.FIREBASE_DB -> FirebaseDatabaseScreen(
                    project = project,
                    initialTab = 0,
                    deploymentsState = deploymentsState,
                    aiState = aiState,
                    onUpdateFirebaseConfig = onUpdateFirebaseConfig,
                    onAddCollection = onAddCollection,
                    onRemoveCollection = onRemoveCollection,
                    onAddDatabaseTable = onAddDatabaseTable,
                    onGenerateMigration = onGenerateMigration,
                    onDeployProject = onDeployProject,
                    onRollbackDeployment = onRollbackDeployment
                )

                WorkspaceSubMode.BACKEND_API -> BackendApiStudioScreen(
                    project = project,
                    initialTab = 0,
                    apiResult = apiResult,
                    apiRunning = apiRunning,
                    onUpdateCloudInfra = onUpdateCloudInfra,
                    onUpdateSecurityToggles = onUpdateSecurityToggles,
                    onAddEndpoint = onAddEndpoint,
                    onGenerateNodeBackend = onGenerateNodeBackend,
                    onSyncAndroidGradle = onSyncAndroidGradle,
                    onSaveApiRequest = onSaveApiRequest,
                    onExecuteApiRequest = onExecuteApiRequest,
                    onGenerateKotlinClient = onGenerateKotlinClient
                )

                WorkspaceSubMode.BUILD_EXPORT -> BuildCenterScreen(
                    project = project,
                    diagnostics = diagnostics,
                    providerSettings = providerSettings,
                    buildWorkerStatus = buildWorkerStatus,
                    buildJobs = buildJobs,
                    onRunDiagnostics = onRunDiagnostics,
                    onAiFixIssues = { activeMode = WorkspaceSubMode.SPLIT_IDE },
                    onExportZip = onExportZip,
                    onTestWorkerConnection = onTestWorkerConnection,
                    onTriggerRemoteBuild = onTriggerRemoteBuild,
                    onRefreshBuildJob = onRefreshBuildJob,
                    onDownloadBuildArtifact = onDownloadBuildArtifact,
                    onAddAsset = onAddAsset,
                    onDeleteAsset = onDeleteAsset,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun SplitMonacoIdePane(
    project: AppProject,
    selectedFileId: String?,
    aiState: AiTaskState,
    editorFontSizeSp: Int,
    pendingPatches: List<PendingFilePatch>,
    chatMessages: List<ChatMessage>,
    onSelectFile: (String) -> Unit,
    onSaveFileContent: (String, String) -> Unit,
    onAddCodeFile: (String, String, CodeLayer) -> Unit,
    onDeleteCodeFile: (String) -> Unit,
    onAiRefactorFile: (ProjectFile, String) -> Unit,
    onAiExplainFile: (ProjectFile) -> Unit,
    onSendAssistantMessage: (String) -> Unit,
    onApplyPatches: () -> Unit,
    onDiscardPatches: () -> Unit
) {
    val context = LocalContext.current
    var showExplorer by remember { mutableStateOf(true) }
    var showAiPane by remember { mutableStateOf(true) }
    var treeSearch by remember { mutableStateOf("") }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFilePath by remember { mutableStateOf("") }
    var newFileLayer by remember { mutableStateOf(CodeLayer.UI) }

    val openTabs = remember(project.id) { mutableStateListOf<String>() }
    val currentFile = project.files.firstOrNull { it.fileId == selectedFileId } ?: project.files.firstOrNull()
    if (currentFile != null && currentFile.fileId !in openTabs) {
        openTabs.add(currentFile.fileId)
        if (openTabs.size > 6) openTabs.removeAt(0)
    }

    var isEditing by remember(currentFile?.fileId) { mutableStateOf(false) }
    var editableContent by remember(currentFile?.fileId, currentFile?.content) {
        mutableStateOf(currentFile?.content.orEmpty())
    }
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var aiChatInput by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // IDE Pane Toggles Toolbar
        Surface(
            color = Color(0xFF111622),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = showExplorer,
                        onClick = { showExplorer = !showExplorer },
                        leadingIcon = { Icon(Icons.Filled.AccountTree, null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Files (${project.files.size})", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("toggle_ide_file_tree")
                    )
                    FilterChip(
                        selected = showAiPane,
                        onClick = { showAiPane = !showAiPane },
                        leadingIcon = { Icon(Icons.Filled.SmartToy, null, modifier = Modifier.size(14.dp)) },
                        label = { Text("AI Chat", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("toggle_ide_ai_chat")
                    )
                }
                OutlinedButton(
                    onClick = { showNewFileDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New File", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Responsive Split-Pane Body: File Tree | Monaco Code Editor | AI Chat Panel
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isDesktopOrTablet = maxWidth >= 820.dp
            val treeWidth = if (isDesktopOrTablet) 210.dp else 132.dp

            if (isDesktopOrTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (showExplorer) {
                        FileTreeExplorerPane(
                            files = project.files,
                            selectedFileId = currentFile?.fileId,
                            searchQuery = treeSearch,
                            onSearchChange = { treeSearch = it },
                            onSelectFile = onSelectFile,
                            modifier = Modifier.width(treeWidth).fillMaxHeight()
                        )
                    }
                    MonacoEditorPane(
                        project = project,
                        currentFile = currentFile,
                        openTabIds = openTabs,
                        isEditing = isEditing,
                        editableContent = editableContent,
                        editorFontSizeSp = editorFontSizeSp,
                        showFindReplace = showFindReplace,
                        findText = findText,
                        replaceText = replaceText,
                        copiedNotice = copiedNotice,
                        onSelectTab = onSelectFile,
                        onCloseTab = { id ->
                            openTabs.remove(id)
                            if (id == currentFile?.fileId) {
                                openTabs.lastOrNull()?.let(onSelectFile)
                            }
                        },
                        onToggleEdit = {
                            if (isEditing && currentFile != null) {
                                onSaveFileContent(currentFile.fileId, editableContent)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        },
                        onEditChange = { editableContent = it },
                        onToggleFindReplace = { showFindReplace = !showFindReplace },
                        onFindChange = { findText = it },
                        onReplaceChange = { replaceText = it },
                        onApplyReplaceAll = {
                            if (currentFile != null && findText.isNotBlank()) {
                                val next = editableContent.replace(findText, replaceText)
                                editableContent = next
                                onSaveFileContent(currentFile.fileId, next)
                            }
                        },
                        onCopyCode = {
                            if (currentFile != null) {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText(currentFile.path, currentFile.content))
                                copiedNotice = true
                            }
                        },
                        onAiExplainFile = { currentFile?.let(onAiExplainFile) },
                        onDeleteFile = { currentFile?.let { onDeleteCodeFile(it.fileId) } },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    if (showAiPane) {
                        AiChatSplitPane(
                            currentFile = currentFile,
                            aiState = aiState,
                            pendingPatches = pendingPatches,
                            chatMessages = chatMessages,
                            input = aiChatInput,
                            onInputChange = { aiChatInput = it },
                            onSend = {
                                if (aiChatInput.isNotBlank()) {
                                    onSendAssistantMessage(aiChatInput)
                                    aiChatInput = ""
                                }
                            },
                            onRefactorCurrentFile = { instruction ->
                                if (currentFile != null && instruction.isNotBlank()) {
                                    onAiRefactorFile(currentFile, instruction)
                                }
                            },
                            onApplyPatches = onApplyPatches,
                            onDiscardPatches = onDiscardPatches,
                            modifier = Modifier.width(276.dp).fillMaxHeight()
                        )
                    }
                }
            } else {
                // Compact / Phone Split Layout: Top Split (File Tree + Monaco Editor), Bottom Split (AI Chat Panel)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(if (showAiPane) 1.35f else 1f).fillMaxWidth()) {
                        if (showExplorer) {
                            FileTreeExplorerPane(
                                files = project.files,
                                selectedFileId = currentFile?.fileId,
                                searchQuery = treeSearch,
                                onSearchChange = { treeSearch = it },
                                onSelectFile = onSelectFile,
                                modifier = Modifier.width(treeWidth).fillMaxHeight()
                            )
                        }
                        MonacoEditorPane(
                            project = project,
                            currentFile = currentFile,
                            openTabIds = openTabs,
                            isEditing = isEditing,
                            editableContent = editableContent,
                            editorFontSizeSp = editorFontSizeSp,
                            showFindReplace = showFindReplace,
                            findText = findText,
                            replaceText = replaceText,
                            copiedNotice = copiedNotice,
                            onSelectTab = onSelectFile,
                            onCloseTab = { id ->
                                openTabs.remove(id)
                                if (id == currentFile?.fileId) {
                                    openTabs.lastOrNull()?.let(onSelectFile)
                                }
                            },
                            onToggleEdit = {
                                if (isEditing && currentFile != null) {
                                    onSaveFileContent(currentFile.fileId, editableContent)
                                    isEditing = false
                                } else {
                                    isEditing = true
                                }
                            },
                            onEditChange = { editableContent = it },
                            onToggleFindReplace = { showFindReplace = !showFindReplace },
                            onFindChange = { findText = it },
                            onReplaceChange = { replaceText = it },
                            onApplyReplaceAll = {
                                if (currentFile != null && findText.isNotBlank()) {
                                    val next = editableContent.replace(findText, replaceText)
                                    editableContent = next
                                    onSaveFileContent(currentFile.fileId, next)
                                }
                            },
                            onCopyCode = {
                                if (currentFile != null) {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText(currentFile.path, currentFile.content))
                                    copiedNotice = true
                                }
                            },
                            onAiExplainFile = { currentFile?.let(onAiExplainFile) },
                            onDeleteFile = { currentFile?.let { onDeleteCodeFile(it.fileId) } },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    if (showAiPane) {
                        AiChatSplitPane(
                            currentFile = currentFile,
                            aiState = aiState,
                            pendingPatches = pendingPatches,
                            chatMessages = chatMessages,
                            input = aiChatInput,
                            onInputChange = { aiChatInput = it },
                            onSend = {
                                if (aiChatInput.isNotBlank()) {
                                    onSendAssistantMessage(aiChatInput)
                                    aiChatInput = ""
                                }
                            },
                            onRefactorCurrentFile = { instruction ->
                                if (currentFile != null && instruction.isNotBlank()) {
                                    onAiRefactorFile(currentFile, instruction)
                                }
                            },
                            onApplyPatches = onApplyPatches,
                            onDiscardPatches = onDiscardPatches,
                            modifier = Modifier.weight(0.85f).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create Project File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFilePath,
                        onValueChange = { newFilePath = it },
                        label = { Text("Relative File Path") },
                        placeholder = { Text("app/src/main/java/com/bcf/OrdersScreen.kt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FileTreeExplorerPane(
    files: List<ProjectFile>,
    selectedFileId: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(files, searchQuery) {
        files.filter {
            searchQuery.isBlank() || it.path.contains(searchQuery, ignoreCase = true)
        }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { it.layer }
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF0B0F19),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "EXPLORER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search files...", style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (layer, layerFiles) ->
                    item(key = "folder_${layer.name}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.FolderOpen, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Text(
                                text = layer.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    items(layerFiles, key = { it.fileId }) { file ->
                        val isSelected = file.fileId == selectedFileId
                        val langDot = when (file.language.lowercase()) {
                            "kotlin" -> Color(0xFFA855F7)
                            "typescript", "javascript" -> Color(0xFF38BDF8)
                            "sql" -> Color(0xFF10B981)
                            "rules" -> Color(0xFFF97316)
                            else -> Color(0xFFF59E0B)
                        }
                        Surface(
                            onClick = { onSelectFile(file.fileId) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(langDot))
                                Text(
                                    text = file.path.substringAfterLast('/'),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
private fun MonacoEditorPane(
    project: AppProject,
    currentFile: ProjectFile?,
    openTabIds: List<String>,
    isEditing: Boolean,
    editableContent: String,
    editorFontSizeSp: Int,
    showFindReplace: Boolean,
    findText: String,
    replaceText: String,
    copiedNotice: Boolean,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onEditChange: (String) -> Unit,
    onToggleFindReplace: () -> Unit,
    onFindChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onApplyReplaceAll: () -> Unit,
    onCopyCode: () -> Unit,
    onAiExplainFile: () -> Unit,
    onDeleteFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0D1117),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (currentFile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a file from Explorer", color = Color(0xFF8B949E))
            }
            return@Surface
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Monaco Tab Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                openTabIds.mapNotNull { id -> project.files.firstOrNull { it.fileId == id } }.forEach { tabFile ->
                    val active = tabFile.fileId == currentFile.fileId
                    Surface(
                        onClick = { onSelectTab(tabFile.fileId) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (active) Color(0xFF0D1117) else Color(0xFF21262D),
                        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                            Text(
                                text = tabFile.path.substringAfterLast('/'),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else Color(0xFF8B949E)
                            )
                            if (openTabIds.size > 1) {
                                IconButton(onClick = { onCloseTab(tabFile.fileId) }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Filled.Close, "Close tab", tint = Color(0xFF8B949E), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Breadcrumb & Editor Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121720))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentFile.path,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF58A6FF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAiExplainFile, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Psychology, "AI Explain", tint = Color(0xFFD2A8FF), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleFindReplace, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.FindReplace, "Find and Replace", tint = Color(0xFF8B949E), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onCopyCode, modifier = Modifier.size(32.dp)) {
                        Icon(if (copiedNotice) Icons.Filled.Check else Icons.Filled.ContentCopy, "Copy code", tint = Color(0xFF8B949E), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onToggleEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                            if (isEditing) "Save edits" else "Edit file",
                            tint = if (isEditing) Color(0xFF3FB950) else Color(0xFFE6EDF3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (project.files.size > 1) {
                        IconButton(onClick = onDeleteFile, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.DeleteOutline, "Delete file", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (showFindReplace) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(value = findText, onValueChange = onFindChange, label = { Text("Find") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = replaceText, onValueChange = onReplaceChange, label = { Text("Replace") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = onApplyReplaceAll, shape = RoundedCornerShape(8.dp)) { Text("All") }
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = editableContent,
                    onValueChange = onEditChange,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = editorFontSizeSp.sp,
                        lineHeight = (editorFontSizeSp + 6).sp,
                        color = Color(0xFFE6EDF3)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                val lines = remember(currentFile.fileId, currentFile.content) { currentFile.content.lines() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    lines.forEachIndexed { idx, line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "${idx + 1}".padStart(4, ' '),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = (editorFontSizeSp - 1).coerceAtLeast(9).sp,
                                color = Color(0xFF6E7681)
                            )
                            Text(
                                text = highlightMonacoLine(line),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = editorFontSizeSp.sp,
                                lineHeight = (editorFontSizeSp + 5).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun highlightMonacoLine(line: String) = buildAnnotatedString {
    val trimmed = line.trimStart()
    if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
        withStyle(SpanStyle(color = Color(0xFF8B949E))) { append(line) }
        return@buildAnnotatedString
    }
    val keywords = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var", "if", "else", "when",
        "return", "suspend", "private", "public", "override", "data", "sealed", "enum", "async", "await",
        "export", "const", "let", "function", "from", "allow", "match", "service", "rules_version"
    )
    val tokens = line.split(Regex("(?<=\\s)|(?=\\s)|(?<=[(){}:,.<>])|(?=[(){}:,.<>])"))
    tokens.forEach { token ->
        val color = when {
            token in keywords -> Color(0xFFFF7B72)
            token.startsWith("\"") || token.endsWith("\"") || token.startsWith("'") -> Color(0xFFA5D6FF)
            token.startsWith("@") -> Color(0xFFD2A8FF)
            token.firstOrNull()?.isUpperCase() == true -> Color(0xFF79C0FF)
            else -> Color(0xFFE6EDF3)
        }
        withStyle(SpanStyle(color = color)) { append(token) }
    }
}

@Composable
private fun AiChatSplitPane(
    currentFile: ProjectFile?,
    aiState: AiTaskState,
    pendingPatches: List<PendingFilePatch>,
    chatMessages: List<ChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRefactorCurrentFile: (String) -> Unit,
    onApplyPatches: () -> Unit,
    onDiscardPatches: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF101522),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("AI Coding Agent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                if (currentFile != null) {
                    Text(
                        text = currentFile.path.substringAfterLast('/'),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (pendingPatches.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1A2438),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${pendingPatches.size} Pending AI Patch(es)", style = MaterialTheme.typography.labelMedium, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = onApplyPatches, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                                Text("Apply", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(onClick = onDiscardPatches, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                                Text("Discard", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (msg.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color(0xFF171E2E),
                        border = BorderStroke(
                            1.dp,
                            if (msg.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (msg.isUser) "YOU" else "BCF AGENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(msg.content, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Ask AI or edit active file...", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("workspace_ai_chat_input")
                )
                IconButton(
                    onClick = onSend,
                    enabled = input.isNotBlank() && aiState !is AiTaskState.Working
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send AI chat message", tint = MaterialTheme.colorScheme.primary)
                }
                if (currentFile != null) {
                    OutlinedButton(
                        onClick = { onRefactorCurrentFile(input); onInputChange("") },
                        enabled = input.isNotBlank() && aiState !is AiTaskState.Working,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Patch", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
