package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiStudioEngine
import com.example.data.ProjectRepository
import com.example.local.LocalProjectEntity
import com.example.local.LocalStudioDatabase
import com.example.local.ProjectSnapshotEntity
import com.example.local.StudioPreferences
import com.example.model.AgentStep
import com.example.model.AgentStepStatus
import com.example.model.AiGenerationMode
import com.example.model.AiProviderSettings
import com.example.model.AiReviewReport
import com.example.model.AndroidBuildConfig
import com.example.model.ApiExecutionResult
import com.example.model.ApiRequestSpec
import com.example.model.AppComponent
import com.example.model.AppProject
import com.example.model.AppScreen
import com.example.model.AppThemePreset
import com.example.model.ArchitecturePattern
import com.example.model.AssetCategory
import com.example.model.BlockCompiler
import com.example.model.BlockOpType
import com.example.model.BlockRuntimeEngine
import com.example.model.BuildJobRecord
import com.example.model.BuildTarget
import com.example.model.ChatMessage
import com.example.model.CloudEndpointSpec
import com.example.model.CodeLayer
import com.example.model.ComponentType
import com.example.model.CustomMoreBlock
import com.example.model.DatabaseEngine
import com.example.model.DatabaseTableSpec
import com.example.model.DeploymentRecord
import com.example.model.DiagnosticIssue
import com.example.model.FirebaseSetupConfig
import com.example.model.FirestoreCollectionSpec
import com.example.model.LogicBlock
import com.example.model.PendingFilePatch
import com.example.model.ProjectAsset
import com.example.model.ProjectFile
import com.example.model.ProjectGenerators
import com.example.model.ScalingTier
import com.example.model.SketchComponent
import com.example.model.SketchComponentType
import com.example.model.StarterBlueprints
import com.example.network.HttpWorkspaceClient
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Working(val label: String) : AiTaskState
    data class Success(val message: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

enum class StudioTab(val label: String) {
    HOME("Home"),
    PROJECTS("My Projects"),
    WORKSPACE("Project IDE"),
    AI_ARCHITECT("AI Studio"),
    VISUAL_BUILDER("Designer"),
    BLOCKS("Blocks"),
    CODE_TREE("Code"),
    PREVIEW_REVIEW("Preview"),
    BACKEND("Backend"),
    FIREBASE("Firebase"),
    API_MANAGER("API"),
    DATABASE("Database"),
    BUILD_CENTER("Build"),
    TEMPLATES("Templates"),
    SETTINGS("Settings"),
    HELP("Help")
}

data class PreviewRuntimeState(
    val currentScreenId: String? = null,
    val darkMode: Boolean = true,
    val tabletFrame: Boolean = false,
    val xrayMode: Boolean = false,
    val inputValues: Map<String, String> = emptyMap(),
    val switchValues: Map<String, Boolean> = emptyMap(),
    val sliderValues: Map<String, Float> = emptyMap(),
    val chipSelections: Map<String, String> = emptyMap(),
    val variables: Map<String, String> = emptyMap(),
    val sharedPrefs: Map<String, String> = emptyMap(),
    val dynamicTexts: Map<String, String> = emptyMap(),
    val hiddenComponentIds: Set<String> = emptySet(),
    val activeToast: String? = null,
    val activeDialog: Pair<String, String>? = null,
    val traceLogs: List<String> = listOf("BCF Builder Runtime Ready • App Check Active"),
    val logcatEntries: List<String> = listOf("[INFO/Runtime] Preview engine initialized")
)

class StudioViewModel(
    private val repository: ProjectRepository,
    private val aiEngine: AiStudioEngine,
    private val localDb: LocalStudioDatabase,
    private val preferences: StudioPreferences,
    private val httpClient: HttpWorkspaceClient,
    private val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "StudioViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }

    val providerSettings: StateFlow<AiProviderSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AiProviderSettings())

    val projectsState: StateFlow<UiState<List<AppProject>>> = repository.observeProjects(currentUserId)
        .map<List<AppProject>, UiState<List<AppProject>>> { projects ->
            cacheProjectsLocally(projects)
            UiState.Success(projects)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing projects", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load projects"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val localCachedCount: StateFlow<Int> = localDb.projectDao().observeLocalProjects(currentUserId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val deploymentsState: StateFlow<UiState<List<DeploymentRecord>>> = repository.observeDeployments(currentUserId)
        .map<List<DeploymentRecord>, UiState<List<DeploymentRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing deployments", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load deployments"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _currentTab = MutableStateFlow(StudioTab.HOME)
    val currentTab: StateFlow<StudioTab> = _currentTab.asStateFlow()

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private val _activeScreenId = MutableStateFlow<String?>(null)
    val activeScreenId: StateFlow<String?> = _activeScreenId.asStateFlow()

    private val _selectedComponentId = MutableStateFlow<String?>(null)
    val selectedComponentId: StateFlow<String?> = _selectedComponentId.asStateFlow()

    private val _selectedFileId = MutableStateFlow<String?>(null)
    val selectedFileId: StateFlow<String?> = _selectedFileId.asStateFlow()

    private val _selectedBlockWidgetId = MutableStateFlow("")
    val selectedBlockWidgetId: StateFlow<String> = _selectedBlockWidgetId.asStateFlow()

    private val _selectedBlockEvent = MutableStateFlow("onCreate")
    val selectedBlockEvent: StateFlow<String> = _selectedBlockEvent.asStateFlow()

    private val _lastAiGeneratedFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val lastAiGeneratedFiles: StateFlow<List<ProjectFile>> = _lastAiGeneratedFiles.asStateFlow()

    private val _aiState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiState: StateFlow<AiTaskState> = _aiState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _reviewReport = MutableStateFlow<AiReviewReport?>(null)
    val reviewReport: StateFlow<AiReviewReport?> = _reviewReport.asStateFlow()

    private val _agentSteps = MutableStateFlow<List<AgentStep>>(emptyList())
    val agentSteps: StateFlow<List<AgentStep>> = _agentSteps.asStateFlow()

    private val _pendingPatches = MutableStateFlow<List<PendingFilePatch>>(emptyList())
    val pendingPatches: StateFlow<List<PendingFilePatch>> = _pendingPatches.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<DiagnosticIssue>>(emptyList())
    val diagnostics: StateFlow<List<DiagnosticIssue>> = _diagnostics.asStateFlow()

    private val _apiResult = MutableStateFlow<ApiExecutionResult?>(null)
    val apiResult: StateFlow<ApiExecutionResult?> = _apiResult.asStateFlow()

    private val _apiRunning = MutableStateFlow(false)
    val apiRunning: StateFlow<Boolean> = _apiRunning.asStateFlow()

    private val _buildWorkerStatus = MutableStateFlow("Not Configured")
    val buildWorkerStatus: StateFlow<String> = _buildWorkerStatus.asStateFlow()

    private val _buildJobs = MutableStateFlow<List<BuildJobRecord>>(emptyList())
    val buildJobs: StateFlow<List<BuildJobRecord>> = _buildJobs.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                content = "BCF Builder AI Architect is online. Pick any generation mode (Full Stack, Add Feature, Fix Errors, Generate Tests) or send a prompt—every change syncs directly into your project."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _previewState = MutableStateFlow(PreviewRuntimeState())
    val previewState: StateFlow<PreviewRuntimeState> = _previewState.asStateFlow()

    // Undo/Redo stacks for Visual Designer
    private val undoStack = ArrayDeque<List<AppScreen>>()
    private val redoStack = ArrayDeque<List<AppScreen>>()

    val activeProject: StateFlow<AppProject?> = combine(projectsState, _activeProjectId) { state, id ->
        val list = (state as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    @Suppress("OPT_IN_USAGE")
    val snapshots: StateFlow<List<ProjectSnapshotEntity>> = _activeProjectId
        .flatMapLatest { pid ->
            if (pid.isNullOrBlank()) flowOf(emptyList())
            else localDb.projectDao().observeSnapshots(pid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun selectTab(tab: StudioTab) {
        _currentTab.value = tab
        if (tab == StudioTab.BUILD_CENTER) {
            runDiagnostics()
        }
    }

    fun selectProject(project: AppProject) {
        _activeProjectId.value = project.id
        _activeScreenId.value = project.screens.firstOrNull()?.screenId
        _selectedComponentId.value = null
        _selectedFileId.value = project.files.firstOrNull()?.fileId
        _selectedBlockWidgetId.value = ""
        _selectedBlockEvent.value = "onCreate"
        _reviewReport.value = null
        _previewState.value = PreviewRuntimeState(currentScreenId = project.screens.firstOrNull()?.screenId)
        undoStack.clear()
        redoStack.clear()
        _diagnostics.value = ProjectGenerators.runProjectDiagnostics(project)
    }

    fun openProjectInTab(project: AppProject, tab: StudioTab = StudioTab.WORKSPACE) {
        selectProject(project)
        selectTab(tab)
    }

    fun selectScreen(screenId: String) {
        _activeScreenId.value = screenId
        _selectedComponentId.value = null
        _selectedBlockWidgetId.value = ""
        _selectedBlockEvent.value = "onCreate"
    }

    fun selectComponent(componentId: String?) {
        _selectedComponentId.value = componentId
    }

    fun selectFile(fileId: String) {
        _selectedFileId.value = fileId
    }

    fun selectBlockTarget(widgetId: String, eventTrigger: String) {
        _selectedBlockWidgetId.value = widgetId
        _selectedBlockEvent.value = eventTrigger.ifBlank { if (widgetId.isBlank()) "onCreate" else "onClick" }
    }

    fun openBlocksForWidget(component: AppComponent, stayInWorkspace: Boolean = false) {
        val defaultEvent = when (component.type) {
            ComponentType.TEXT_INPUT,
            ComponentType.PASSWORD_FIELD,
            ComponentType.SLIDER_CONTROL -> "onTextChange"
            ComponentType.SWITCH_TOGGLE,
            ComponentType.CHECKBOX,
            ComponentType.RADIO_BUTTON,
            ComponentType.CHIP_GROUP -> "onCheckedChange"
            else -> "onClick"
        }
        _selectedComponentId.value = component.componentId
        _selectedBlockWidgetId.value = component.componentId
        _selectedBlockEvent.value = defaultEvent
        if (!stayInWorkspace) {
            _currentTab.value = StudioTab.BLOCKS
        }
        _statusBanner.value = "Editing $defaultEvent blocks for '${component.text.ifBlank { component.type.displayName }}'"
    }

    fun clearLastAiGeneratedFiles() {
        _lastAiGeneratedFiles.value = emptyList()
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearAiState() {
        _aiState.value = AiTaskState.Idle
    }

    fun saveProviderSettings(settings: AiProviderSettings) {
        viewModelScope.launch {
            preferences.saveSettings(settings)
            _statusBanner.value = "Saved settings (${settings.provider.displayName})."
        }
    }

    // --- Project CRUD, Duplication, Import/Export & Version Snapshots ---

    fun createFromTemplate(template: AppProject) {
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("Creating '${template.name}' in cloud & Room cache...")
            val result = repository.createProject(currentUserId, template)
            result.onSuccess { newId ->
                _activeProjectId.value = newId
                _activeScreenId.value = template.screens.firstOrNull()?.screenId
                _selectedFileId.value = template.files.firstOrNull()?.fileId
                _selectedBlockWidgetId.value = ""
                _selectedBlockEvent.value = "onCreate"
                _lastAiGeneratedFiles.value = template.files
                _previewState.value = PreviewRuntimeState(currentScreenId = template.screens.firstOrNull()?.screenId)
                saveLocalSnapshot(template.copy(id = newId), "Initial template")
                _aiState.value = AiTaskState.Success("Created ${template.name}")
                _currentTab.value = StudioTab.WORKSPACE
            }.onFailure { err ->
                _aiState.value = AiTaskState.Error(err.localizedMessage ?: "Failed to create project.")
            }
        }
    }

    fun createBlankProject(name: String) {
        val cleanName = name.trim().ifBlank { "Custom BCF App" }
        createFromTemplate(StarterBlueprints.blankProject(currentUserId, cleanName))
    }

    fun duplicateProject(project: AppProject) {
        val copy = project.copy(
            id = "",
            name = "${project.name.take(68)} Copy",
            activeVersion = "v1.0.0"
        )
        createFromTemplate(copy)
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(currentUserId, projectId)
                .onSuccess {
                    localDb.projectDao().deleteProject(projectId)
                    if (_activeProjectId.value == projectId) _activeProjectId.value = null
                    _statusBanner.value = "Project deleted."
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to delete project."
                }
        }
    }

    fun createVersionSnapshot(label: String) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            saveLocalSnapshot(project, label.ifBlank { "Manual checkpoint" })
            _statusBanner.value = "Saved version snapshot '$label'."
        }
    }

    fun restoreSnapshot(snapshot: ProjectSnapshotEntity) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            val (restored, report) = ProjectGenerators.importProjectJson(snapshot.jsonPayload, currentUserId)
            if (restored != null) {
                val merged = project.copy(
                    name = restored.name,
                    packageName = restored.packageName,
                    tagline = restored.tagline,
                    themePreset = restored.themePreset,
                    architecture = restored.architecture,
                    screens = restored.screens,
                    files = restored.files,
                    backend = restored.backend
                )
                saveDesignUpdate(merged, "Rolled back to '${snapshot.label}' ($report)")
            } else {
                _statusBanner.value = report
            }
        }
    }

    fun importProjectFromJson(rawJson: String) {
        val (parsed, report) = ProjectGenerators.importProjectJson(rawJson, currentUserId)
        if (parsed == null) {
            _aiState.value = AiTaskState.Error(report)
            return
        }
        createFromTemplate(parsed)
        _statusBanner.value = report
    }

    fun exportProjectZip(outputStream: OutputStream, mode: String, onDone: (Int) -> Unit) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                ProjectGenerators.writeProjectZip(project, outputStream, mode)
            }
            onDone(count)
            _statusBanner.value = "Exported $count files to ZIP archive."
        }
    }

    // --- AI Studio (13 Modes) & AI Project Agent (Plan -> Inspect -> Modify -> Validate -> Report) ---

    fun runAiStudioTask(
        prompt: String,
        mode: AiGenerationMode,
        architecture: ArchitecturePattern,
        themePreset: AppThemePreset
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return
        val settings = providerSettings.value
        val current = activeProject.value

        if (mode == AiGenerationMode.EXPLAIN_CODE || mode == AiGenerationMode.PLAN_FIRST) {
            viewModelScope.launch {
                _aiState.value = AiTaskState.Working("Running ${mode.label}...")
                try {
                    val file = current?.files?.firstOrNull { it.fileId == _selectedFileId.value }
                    val prefix = if (mode == AiGenerationMode.PLAN_FIRST) "Produce a clear step-by-step architecture & module plan before writing code:\n" else "Explain the code structure and data flow:\n"
                    val answer = if (current != null) {
                        aiEngine.explainOrDiagnoseCode(current, file, "$prefix$trimmed", settings)
                    } else {
                        val sb = StringBuilder()
                        aiEngine.streamStudioAssistant(null, null, "$prefix$trimmed", settings).collect { sb.append(it) }
                        sb.toString()
                    }
                    _chatMessages.update {
                        it + ChatMessage(StarterBlueprints.newId("u"), true, "[${mode.label}] $trimmed") +
                            ChatMessage(StarterBlueprints.newId("a"), false, answer)
                    }
                    _aiState.value = AiTaskState.Success("${mode.label} complete — view in AI Chat.")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _aiState.value = AiTaskState.Error(e.localizedMessage ?: "AI request failed.")
                }
            }
            return
        }

        if (mode.targetsExistingProject && current != null) {
            runProjectAgentPatch(current, trimmed, mode, settings)
            return
        }

        // Full or new project generation
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("${settings.modelName} is building (${mode.label})...")
            _agentSteps.value = listOf(
                AgentStep("1", "Plan", "Extracting app screens, backend & schema for '$trimmed'", AgentStepStatus.RUNNING),
                AgentStep("2", "Generate", "Synthesizing Kotlin Compose, Gradle, Node.js & Firestore rules", AgentStepStatus.PENDING),
                AgentStep("3", "Validate", "Running static project diagnostics", AgentStepStatus.PENDING)
            )
            try {
                val generated = aiEngine.generateFullStackApp(
                    prompt = trimmed,
                    mode = mode,
                    architecture = architecture,
                    themePreset = themePreset,
                    userId = currentUserId,
                    providerSettings = settings
                )
                _agentSteps.value = listOf(
                    AgentStep("1", "Plan", "Designed ${generated.screens.size} screens & ${generated.backend.collections.size} collections", AgentStepStatus.DONE),
                    AgentStep("2", "Generate", "Generated ${generated.files.size} source & config files", AgentStepStatus.DONE),
                    AgentStep("3", "Validate", "Diagnostics verified — syncing to cloud", AgentStepStatus.RUNNING)
                )
                val saveRes = repository.createProject(currentUserId, generated)
                saveRes.onSuccess { newId ->
                    _activeProjectId.value = newId
                    _activeScreenId.value = generated.screens.firstOrNull()?.screenId
                    _selectedFileId.value = generated.files.firstOrNull()?.fileId
                    _selectedBlockWidgetId.value = ""
                    _selectedBlockEvent.value = "onCreate"
                    _lastAiGeneratedFiles.value = generated.files
                    _previewState.value = PreviewRuntimeState(currentScreenId = generated.screens.firstOrNull()?.screenId)
                    saveLocalSnapshot(generated.copy(id = newId), "AI: ${mode.label}")
                    _agentSteps.update { it.dropLast(1) + AgentStep("3", "Report", "Saved '${generated.name}' (${generated.files.size} files)", AgentStepStatus.DONE) }
                    _aiState.value = AiTaskState.Success("Generated '${generated.name}' (${generated.screens.size} screens, ${generated.files.size} files)")
                    _currentTab.value = StudioTab.WORKSPACE
                }.onFailure { err ->
                    _aiState.value = AiTaskState.Error("Generated app, but Firestore save failed: ${err.localizedMessage}")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Full-stack generation timed out", e)
                _aiState.value = AiTaskState.Error("AI generation timed out after 60s. Please retry with a concise prompt.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Full-stack generation failed", e)
                _aiState.value = AiTaskState.Error(e.localizedMessage ?: "AI generation failed.")
            }
        }
    }

    private fun runProjectAgentPatch(
        project: AppProject,
        instruction: String,
        mode: AiGenerationMode,
        settings: AiProviderSettings
    ) {
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("AI Agent inspecting '${project.name}' (${mode.label})...")
            _agentSteps.value = listOf(
                AgentStep("1", "Plan", "Scoping targeted change: $instruction", AgentStepStatus.DONE),
                AgentStep("2", "Inspect", "Reading ${project.files.size} project files & diagnostics", AgentStepStatus.DONE),
                AgentStep("3", "Modify", "Generating minimal incremental file patches", AgentStepStatus.RUNNING)
            )
            try {
                val (planSummary, patches) = aiEngine.generateTargetedProjectPatches(project, instruction, mode, settings)
                _pendingPatches.value = patches
                _agentSteps.value = listOf(
                    AgentStep("1", "Plan", planSummary, AgentStepStatus.DONE),
                    AgentStep("2", "Inspect", "Inspected ${project.files.size} files", AgentStepStatus.DONE),
                    AgentStep("3", "Modify", "${patches.size} targeted patch(es) ready for review", AgentStepStatus.REQUIRES_CONFIRM)
                )
                _aiState.value = AiTaskState.Success("Review ${patches.size} AI patch(es) before applying.")
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiTaskState.Error("AI Agent timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Agent patch failed", e)
                _aiState.value = AiTaskState.Error(e.localizedMessage ?: "AI Agent failed.")
            }
        }
    }

    fun applyPendingPatches() {
        val project = activeProject.value ?: return
        val patches = _pendingPatches.value
        if (patches.isEmpty()) return

        viewModelScope.launch {
            saveLocalSnapshot(project, "Before AI Agent patch")
            val updatedList = project.files.toMutableList()
            patches.forEach { patch ->
                val idx = updatedList.indexOfFirst { it.path.equals(patch.path, ignoreCase = true) }
                if (idx >= 0) {
                    updatedList[idx] = updatedList[idx].copy(content = patch.newContent)
                } else {
                    updatedList += ProjectFile(
                        fileId = StarterBlueprints.newId("file"),
                        path = patch.path,
                        language = patch.language,
                        layer = patch.layer,
                        content = patch.newContent
                    )
                }
            }
            _pendingPatches.value = emptyList()
            _agentSteps.update { it + AgentStep("4", "Report", "Applied ${patches.size} file patch(es)", AgentStepStatus.DONE) }
            saveDesignUpdate(project.copy(files = updatedList.take(60)), "Applied ${patches.size} AI patch(es).")
        }
    }

    fun discardPendingPatches() {
        _pendingPatches.value = emptyList()
        _agentSteps.update { it + AgentStep("4", "Cancelled", "User discarded pending patches", AgentStepStatus.FAILED) }
    }

    fun populateActiveScreenWithAi(prompt: String) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("Generating UI components for ${screen.title}...")
            try {
                pushDesignerUndo(project.screens)
                val routes = project.screens.map { it.route }
                val newComponents = aiEngine.generateScreenComponents(trimmed, screen.title, routes, providerSettings.value)
                val updatedScreens = project.screens.map {
                    if (it.screenId == screen.screenId) it.copy(components = newComponents) else it
                }
                saveDesignUpdate(project.copy(screens = updatedScreens), "Updated ${screen.title} with ${newComponents.size} AI components.")
                _aiState.value = AiTaskState.Success("Populated ${screen.title} with ${newComponents.size} components.")
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiTaskState.Error("Screen generation timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Screen AI generation failed", e)
                _aiState.value = AiTaskState.Error(e.localizedMessage ?: "Failed to generate screen components.")
            }
        }
    }

    fun sendAssistantMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return
        val userMsg = ChatMessage(StarterBlueprints.newId("u"), isUser = true, content = trimmed)
        val assistantMsgId = StarterBlueprints.newId("a")
        _chatMessages.update { it + userMsg + ChatMessage(assistantMsgId, isUser = false, content = "Thinking...") }
        val activeFile = activeProject.value?.files?.firstOrNull { it.fileId == _selectedFileId.value }

        viewModelScope.launch {
            try {
                aiEngine.streamStudioAssistant(activeProject.value, activeFile, trimmed, providerSettings.value)
                    .collect { accumulated ->
                        _chatMessages.update { list ->
                            list.map { if (it.id == assistantMsgId) it.copy(content = accumulated) else it }
                        }
                    }
            } catch (e: TimeoutCancellationException) {
                _chatMessages.update { list ->
                    list.map { if (it.id == assistantMsgId) it.copy(content = "Timed out after 60 seconds. Please try again.") else it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant stream failed", e)
                _chatMessages.update { list ->
                    list.map { if (it.id == assistantMsgId) it.copy(content = "AI error: ${e.localizedMessage ?: "Request failed"}") else it }
                }
            }
        }
    }

    // --- Visual Designer + Undo/Redo ---

    private fun pushDesignerUndo(screens: List<AppScreen>) {
        if (undoStack.size >= 20) undoStack.removeFirst()
        undoStack.addLast(screens)
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undoDesigner() {
        val project = activeProject.value ?: return
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(project.screens)
        saveDesignUpdate(project.copy(screens = prev), "Undid layout change")
    }

    fun redoDesigner() {
        val project = activeProject.value ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(project.screens)
        saveDesignUpdate(project.copy(screens = next), "Redid layout change")
    }

    fun addComponentToActiveScreen(type: ComponentType) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        pushDesignerUndo(project.screens)
        val newComp = AppComponent(
            componentId = StarterBlueprints.newId("cmp"),
            type = type,
            text = type.defaultText,
            subtitle = type.defaultSubtitle
        )
        val updatedScreens = project.screens.map {
            if (it.screenId == screen.screenId) it.copy(components = it.components + newComp) else it
        }
        _selectedComponentId.value = newComp.componentId
        saveDesignUpdate(project.copy(screens = updatedScreens), "Added ${type.displayName}")
    }

    fun updateComponent(updated: AppComponent) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        pushDesignerUndo(project.screens)
        val updatedScreens = project.screens.map { scr ->
            if (scr.screenId == screen.screenId) {
                scr.copy(components = scr.components.map { if (it.componentId == updated.componentId) updated else it })
            } else scr
        }
        saveDesignUpdate(project.copy(screens = updatedScreens), "Saved component properties")
    }

    fun moveComponent(componentId: String, direction: Int) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val list = screen.components.toMutableList()
        val idx = list.indexOfFirst { it.componentId == componentId }
        if (idx == -1) return
        val target = (idx + direction).coerceIn(0, list.lastIndex)
        if (idx == target) return
        pushDesignerUndo(project.screens)
        val item = list.removeAt(idx)
        list.add(target, item)
        val updatedScreens = project.screens.map {
            if (it.screenId == screen.screenId) it.copy(components = list) else it
        }
        saveDesignUpdate(project.copy(screens = updatedScreens))
    }

    fun duplicateComponent(componentId: String) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val list = screen.components.toMutableList()
        val idx = list.indexOfFirst { it.componentId == componentId }
        if (idx == -1) return
        pushDesignerUndo(project.screens)
        val copy = list[idx].copy(componentId = StarterBlueprints.newId("cmp"))
        list.add(idx + 1, copy)
        _selectedComponentId.value = copy.componentId
        val updatedScreens = project.screens.map {
            if (it.screenId == screen.screenId) it.copy(components = list) else it
        }
        saveDesignUpdate(project.copy(screens = updatedScreens), "Duplicated component")
    }

    fun deleteComponent(componentId: String) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        pushDesignerUndo(project.screens)
        val updatedScreens = project.screens.map { scr ->
            if (scr.screenId == screen.screenId) {
                scr.copy(components = scr.components.filterNot { it.componentId == componentId })
            } else scr
        }
        if (_selectedComponentId.value == componentId) _selectedComponentId.value = null
        saveDesignUpdate(project.copy(screens = updatedScreens), "Removed component")
    }

    fun addScreen(title: String, navArgs: String = "", deepLink: String = "") {
        val project = activeProject.value ?: return
        val cleanTitle = title.trim().ifBlank { "Screen ${project.screens.size + 1}" }
        val route = cleanTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "screen_${project.screens.size + 1}" }
        pushDesignerUndo(project.screens)
        val newScreen = AppScreen(
            screenId = StarterBlueprints.newId("scr"),
            title = cleanTitle,
            route = route,
            iconName = "Layers",
            components = listOf(
                AppComponent(StarterBlueprints.newId("cmp"), ComponentType.TOP_APP_BAR, cleanTitle, project.name),
                AppComponent(StarterBlueprints.newId("cmp"), ComponentType.HEADLINE, cleanTitle),
                AppComponent(StarterBlueprints.newId("cmp"), ComponentType.BODY_TEXT, "Design this screen visually or populate with AI.")
            ),
            navArguments = navArgs.trim(),
            deepLinkUri = deepLink.trim()
        )
        _activeScreenId.value = newScreen.screenId
        _selectedComponentId.value = null
        saveDesignUpdate(project.copy(screens = project.screens + newScreen), "Added screen '$cleanTitle'")
    }

    fun deleteScreen(screenId: String) {
        val project = activeProject.value ?: return
        if (project.screens.size <= 1) {
            _statusBanner.value = "Project must have at least one screen."
            return
        }
        pushDesignerUndo(project.screens)
        val remaining = project.screens.filterNot { it.screenId == screenId }
        _activeScreenId.value = remaining.firstOrNull()?.screenId
        _selectedComponentId.value = null
        saveDesignUpdate(project.copy(screens = remaining), "Deleted screen")
    }

    fun updateThemePreset(preset: AppThemePreset) {
        val project = activeProject.value ?: return
        saveDesignUpdate(project.copy(themePreset = preset), "Applied ${preset.displayName} theme")
    }

    fun syncComposeCodeFromVisuals() {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val safeClass = screen.title.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Main" }
        val composePath = "app/src/main/java/${project.packageName.replace('.', '/')}/ui/${safeClass}Screen.kt"
        val composeCode = StarterBlueprints.generateComposeScreenCode(project.name, screen)
        val safeRoute = screen.route.lowercase().replace(Regex("[^a-z0-9_]"), "_").ifBlank { "main" }
        val xmlPath = "app/src/main/res/layout/activity_${safeRoute}.xml"
        val xmlCode = ProjectGenerators.generateAndroidXmlLayout(screen)

        val merged = project.files.toMutableList()
        listOf(
            ProjectFile(StarterBlueprints.newId("file"), composePath, "kotlin", CodeLayer.UI, composeCode),
            ProjectFile(StarterBlueprints.newId("file"), xmlPath, "xml", CodeLayer.UI, xmlCode)
        ).forEach { gen ->
            val idx = merged.indexOfFirst { it.path.equals(gen.path, ignoreCase = true) }
            if (idx >= 0) merged[idx] = merged[idx].copy(content = gen.content) else merged += gen
        }
        val next = project.copy(files = merged.take(60))
        _diagnostics.value = ProjectGenerators.runProjectDiagnostics(next)
        saveDesignUpdate(next, "Synced Compose Kotlin + Android XML for '${screen.title}'")
    }

    // --- Visual Block Programming Operations & AI Block Architect ---

    fun addLogicBlock(
        opType: BlockOpType,
        targetWidgetId: String = _selectedBlockWidgetId.value,
        eventTrigger: String = _selectedBlockEvent.value
    ) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val normalizedTrigger = if (targetWidgetId.isBlank()) eventTrigger.ifBlank { "onCreate" } else eventTrigger.ifBlank { "onClick" }
        val block = LogicBlock(
            blockId = StarterBlueprints.newId("blk"),
            opType = opType,
            arg1 = opType.defaultArg1,
            arg2 = opType.defaultArg2,
            targetWidgetId = targetWidgetId,
            eventTrigger = normalizedTrigger
        )
        val updatedScreens = project.screens.map {
            if (it.screenId == screen.screenId) it.copy(logicBlocks = it.logicBlocks + block) else it
        }
        saveDesignUpdate(project.copy(screens = updatedScreens), "Added block: ${opType.title} -> $normalizedTrigger")
    }

    fun generateBlocksWithAi(prompt: String) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("AI Block Architect generating logic blocks...")
            try {
                val generatedBlocks = aiEngine.generateLogicBlocksFromPrompt(
                    prompt = trimmed,
                    screenTitle = screen.title,
                    targetWidgetId = _selectedBlockWidgetId.value,
                    targetEvent = _selectedBlockEvent.value,
                    availableRoutes = project.screens.map { it.route },
                    widgetNames = screen.components.map { "${it.text} (${it.componentId})" },
                    moreBlockNames = project.backend.customMoreBlocks.map { it.name },
                    providerSettings = providerSettings.value
                )
                if (generatedBlocks.isEmpty()) {
                    _aiState.value = AiTaskState.Error("AI returned 0 blocks. Please rephrase your request.")
                    return@launch
                }
                val updatedScreens = project.screens.map {
                    if (it.screenId == screen.screenId) it.copy(logicBlocks = it.logicBlocks + generatedBlocks) else it
                }
                saveDesignUpdate(project.copy(screens = updatedScreens), "Added ${generatedBlocks.size} AI logic block(s).")
                _aiState.value = AiTaskState.Success("Generated ${generatedBlocks.size} visual block(s)")
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiTaskState.Error("AI block generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiTaskState.Error(e.localizedMessage ?: "AI block generation failed.")
            }
        }
    }

    fun updateLogicBlock(updated: LogicBlock) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val updatedScreens = project.screens.map { scr ->
            if (scr.screenId == screen.screenId) {
                scr.copy(logicBlocks = scr.logicBlocks.map { if (it.blockId == updated.blockId) updated else it })
            } else scr
        }
        saveDesignUpdate(project.copy(screens = updatedScreens), "Updated block")
    }

    fun moveLogicBlock(blockId: String, direction: Int) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val list = screen.logicBlocks.toMutableList()
        val idx = list.indexOfFirst { it.blockId == blockId }
        if (idx == -1) return
        val target = (idx + direction).coerceIn(0, list.lastIndex)
        if (idx == target) return
        val item = list.removeAt(idx)
        list.add(target, item)
        val updatedScreens = project.screens.map {
            if (it.screenId == screen.screenId) it.copy(logicBlocks = list) else it
        }
        saveDesignUpdate(project.copy(screens = updatedScreens))
    }

    fun deleteLogicBlock(blockId: String) {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val updatedScreens = project.screens.map { scr ->
            if (scr.screenId == screen.screenId) {
                scr.copy(logicBlocks = scr.logicBlocks.filterNot { it.blockId == blockId })
            } else scr
        }
        saveDesignUpdate(project.copy(screens = updatedScreens), "Removed block")
    }

    fun compileBlocksToKotlin() {
        val project = activeProject.value ?: return
        val screen = currentScreenFor(project) ?: return
        val safeClass = screen.title.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Main" }
        val targetPath = "app/src/main/java/${project.packageName.replace('.', '/')}/logic/${safeClass}BlockController.kt"
        val kotlin = BlockCompiler.compileScreenBlocksToKotlin(
            packageName = project.packageName,
            screen = screen,
            sketchComponents = project.backend.sketchComponents,
            customMoreBlocks = project.backend.customMoreBlocks
        )
        upsertProjectFile(project, targetPath, "kotlin", CodeLayer.VIEWMODEL, kotlin, "Compiled ${screen.logicBlocks.size} blocks -> ${safeClass}BlockController.kt")
    }

    // --- Sketchware Events, Non-UI Components & MoreBlocks ---

    fun addSketchComponent(name: String, type: SketchComponentType, configNote: String) {
        val project = activeProject.value ?: return
        val cleanName = name.trim().replace(Regex("[^A-Za-z0-9_]"), "").ifBlank {
            type.displayName.lowercase().replace(Regex("[^a-z0-9]"), "")
        }
        val comp = SketchComponent(
            componentId = StarterBlueprints.newId("skc"),
            name = cleanName,
            type = type,
            configNote = configNote.trim()
        )
        val updatedBackend = project.backend.copy(sketchComponents = project.backend.sketchComponents + comp)
        saveDesignUpdate(project.copy(backend = updatedBackend), "Added ${type.displayName} component '$cleanName'")
    }

    fun deleteSketchComponent(componentId: String) {
        val project = activeProject.value ?: return
        val updatedBackend = project.backend.copy(
            sketchComponents = project.backend.sketchComponents.filterNot { it.componentId == componentId }
        )
        saveDesignUpdate(project.copy(backend = updatedBackend), "Removed component")
    }

    fun openBlocksForSketchComponent(component: SketchComponent, eventName: String, stayInWorkspace: Boolean = true) {
        _selectedBlockWidgetId.value = "comp:${component.componentId}"
        _selectedBlockEvent.value = eventName
        if (!stayInWorkspace) {
            _currentTab.value = StudioTab.BLOCKS
        }
        _statusBanner.value = "Editing ${component.name}.$eventName blocks"
    }

    fun addCustomMoreBlock(name: String, paramsSignature: String, description: String) {
        val project = activeProject.value ?: return
        val cleanName = name.trim().replace(Regex("[^A-Za-z0-9_]"), "").ifBlank { "customAction" }
        val mb = CustomMoreBlock(
            blockId = StarterBlueprints.newId("mb"),
            name = cleanName,
            paramsSignature = paramsSignature.trim().ifBlank { "input: String = \"\"" },
            description = description.trim()
        )
        val updatedBackend = project.backend.copy(customMoreBlocks = project.backend.customMoreBlocks + mb)
        saveDesignUpdate(project.copy(backend = updatedBackend), "Created MoreBlock '$cleanName'")
    }

    fun deleteCustomMoreBlock(blockId: String) {
        val project = activeProject.value ?: return
        val updatedBackend = project.backend.copy(
            customMoreBlocks = project.backend.customMoreBlocks.filterNot { it.blockId == blockId }
        )
        saveDesignUpdate(project.copy(backend = updatedBackend), "Deleted MoreBlock")
    }

    fun openBlocksForMoreBlock(moreBlock: CustomMoreBlock, stayInWorkspace: Boolean = true) {
        _selectedBlockWidgetId.value = "moreblock:${moreBlock.name}"
        _selectedBlockEvent.value = "onInvoke"
        if (!stayInWorkspace) {
            _currentTab.value = StudioTab.BLOCKS
        }
        _statusBanner.value = "Editing MoreBlock '${moreBlock.name}()' body"
    }

    // --- Android Studio Project Config Manager (Libs, Permissions, Strings, Colors) ---

    fun toggleProjectLibrary(dependencyCoord: String) {
        val project = activeProject.value ?: return
        val current = project.backend.androidConfig.dependencies
        val nextDeps = if (dependencyCoord in current) current - dependencyCoord else current + dependencyCoord
        val nextProject = project.copy(
            backend = project.backend.copy(
                androidConfig = project.backend.androidConfig.copy(dependencies = nextDeps)
            )
        )
        val syncedFiles = mergeConfigFiles(nextProject)
        saveDesignUpdate(nextProject.copy(files = syncedFiles), "Updated Gradle dependencies (${nextDeps.size})")
    }

    fun toggleProjectPermission(permissionName: String) {
        val project = activeProject.value ?: return
        val current = project.backend.androidConfig.permissions
        val nextPerms = if (permissionName in current) current - permissionName else current + permissionName
        val nextProject = project.copy(
            backend = project.backend.copy(
                androidConfig = project.backend.androidConfig.copy(permissions = nextPerms)
            )
        )
        val syncedFiles = mergeConfigFiles(nextProject)
        saveDesignUpdate(nextProject.copy(files = syncedFiles), "Updated AndroidManifest permissions (${nextPerms.size})")
    }

    fun upsertStringResource(key: String, value: String) {
        val project = activeProject.value ?: return
        val cleanKey = key.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        if (cleanKey.isBlank()) return
        val nextMap = project.backend.stringResources.toMutableMap().apply { put(cleanKey, value) }
        val nextProject = project.copy(backend = project.backend.copy(stringResources = nextMap))
        val syncedFiles = mergeConfigFiles(nextProject)
        saveDesignUpdate(nextProject.copy(files = syncedFiles), "Saved string '@string/$cleanKey'")
    }

    fun removeStringResource(key: String) {
        val project = activeProject.value ?: return
        val nextMap = project.backend.stringResources - key
        val nextProject = project.copy(backend = project.backend.copy(stringResources = nextMap))
        val syncedFiles = mergeConfigFiles(nextProject)
        saveDesignUpdate(nextProject.copy(files = syncedFiles), "Removed string '$key'")
    }

    fun upsertColorResource(key: String, hex: String) {
        val project = activeProject.value ?: return
        val cleanKey = key.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        if (cleanKey.isBlank()) return
        val cleanHex = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
        val nextMap = project.backend.colorResources.toMutableMap().apply { put(cleanKey, cleanHex) }
        val nextProject = project.copy(backend = project.backend.copy(colorResources = nextMap))
        val syncedFiles = mergeConfigFiles(nextProject)
        saveDesignUpdate(nextProject.copy(files = syncedFiles), "Saved color '@color/$cleanKey'")
    }

    fun removeColorResource(key: String) {
        val project = activeProject.value ?: return
        val nextMap = project.backend.colorResources - key
        val nextProject = project.copy(backend = project.backend.copy(colorResources = nextMap))
        val syncedFiles = mergeConfigFiles(nextProject)
        saveDesignUpdate(nextProject.copy(files = syncedFiles), "Removed color '$key'")
    }

    private fun mergeConfigFiles(project: AppProject): List<ProjectFile> {
        val generated = ProjectGenerators.generateAndroidStudioGradleFiles(project)
        val merged = project.files.toMutableList()
        generated.forEach { f ->
            val idx = merged.indexOfFirst { it.path.equals(f.path, ignoreCase = true) }
            if (idx >= 0) merged[idx] = merged[idx].copy(content = f.content) else merged += f
        }
        return merged.take(60)
    }

    // --- Code Editor & AI Actions ---

    fun saveFileContent(fileId: String, newContent: String) {
        val project = activeProject.value ?: return
        val updatedFiles = project.files.map {
            if (it.fileId == fileId) it.copy(content = newContent) else it
        }
        saveDesignUpdate(project.copy(files = updatedFiles), "Saved file changes")
    }

    fun addCodeFile(path: String, language: String, layer: CodeLayer) {
        val project = activeProject.value ?: return
        val cleanPath = path.trim().ifBlank { "app/src/main/java/NewFile.kt" }
        val newFile = ProjectFile(
            fileId = StarterBlueprints.newId("file"),
            path = cleanPath,
            language = language,
            layer = layer,
            content = "// Created in BCF Builder AI\n"
        )
        _selectedFileId.value = newFile.fileId
        saveDesignUpdate(project.copy(files = (project.files + newFile).take(60)), "Added $cleanPath")
    }

    fun deleteCodeFile(fileId: String) {
        val project = activeProject.value ?: return
        val remaining = project.files.filterNot { it.fileId == fileId }
        if (_selectedFileId.value == fileId) _selectedFileId.value = remaining.firstOrNull()?.fileId
        saveDesignUpdate(project.copy(files = remaining), "Deleted file")
    }

    fun refactorFileWithAi(file: ProjectFile, instruction: String) {
        val project = activeProject.value ?: return
        val trimmed = instruction.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("AI updating ${file.path.substringAfterLast('/')}...")
            try {
                val updatedCode = aiEngine.refactorCodeFile(project, file, trimmed, providerSettings.value)
                val updatedFiles = project.files.map {
                    if (it.fileId == file.fileId) it.copy(content = updatedCode) else it
                }
                saveDesignUpdate(project.copy(files = updatedFiles), "AI updated ${file.path.substringAfterLast('/')}")
                _aiState.value = AiTaskState.Success("AI updated ${file.path.substringAfterLast('/')}")
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiTaskState.Error("Code refactor timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiTaskState.Error(e.localizedMessage ?: "AI refactor failed.")
            }
        }
    }

    fun explainFileWithAi(file: ProjectFile) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("Analyzing ${file.path.substringAfterLast('/')}...")
            try {
                val explanation = aiEngine.explainOrDiagnoseCode(
                    project,
                    file,
                    "Explain what this file does, list any bugs or missing imports, and suggest improvements.",
                    providerSettings.value
                )
                _chatMessages.update {
                    it + ChatMessage(StarterBlueprints.newId("u"), true, "Explain `${file.path}`") +
                        ChatMessage(StarterBlueprints.newId("a"), false, explanation)
                }
                _aiState.value = AiTaskState.Success("Explanation ready in AI Studio Chat.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiTaskState.Error(e.localizedMessage ?: "Failed to explain file.")
            }
        }
    }

    // --- Backend Builder, Database Studio, API Manager, Firebase & Assets ---

    fun generateNodeTsBackendFiles() {
        val project = activeProject.value ?: return
        val generated = ProjectGenerators.generateNodeTsBackendFiles(project)
        val merged = project.files.toMutableList()
        generated.forEach { f ->
            val idx = merged.indexOfFirst { it.path.equals(f.path, ignoreCase = true) }
            if (idx >= 0) merged[idx] = merged[idx].copy(content = f.content) else merged += f
        }
        saveDesignUpdate(project.copy(files = merged.take(60)), "Generated ${generated.size} Node.js TypeScript backend files.")
    }

    fun syncAndroidStudioStructure() {
        val project = activeProject.value ?: return
        val generated = ProjectGenerators.generateAndroidStudioGradleFiles(project)
        val merged = project.files.toMutableList()
        generated.forEach { f ->
            val idx = merged.indexOfFirst { it.path.equals(f.path, ignoreCase = true) }
            if (idx >= 0) merged[idx] = merged[idx].copy(content = f.content) else merged += f
        }
        val updated = project.copy(files = merged.take(60))
        _diagnostics.value = ProjectGenerators.runProjectDiagnostics(updated)
        saveDesignUpdate(updated, "Synced Android Studio Gradle, Manifest & NavGraph.")
    }

    fun updateBackendSecurityToggles(jwt: Boolean, rateLimit: Boolean, authGoogle: Boolean, authEmail: Boolean, appCheck: Boolean) {
        val project = activeProject.value ?: return
        val updated = project.backend.copy(
            jwtEnabled = jwt,
            rateLimitEnabled = rateLimit,
            authGoogle = authGoogle,
            authEmail = authEmail,
            appCheckEnabled = appCheck
        )
        saveCloudUpdate(project.copy(backend = updated), "Updated backend security settings")
    }

    fun updateFirebaseSetupConfig(config: FirebaseSetupConfig) {
        val project = activeProject.value ?: return
        val isValid = config.firebaseProjectId.isNotBlank() && config.androidAppId.contains(":")
        val nextCfg = config.copy(validated = isValid)
        val updatedBackend = project.backend.copy(
            firebaseConfig = nextCfg,
            authGoogle = nextCfg.useAuthGoogle,
            authEmail = nextCfg.useAuthEmail,
            appCheckEnabled = nextCfg.useAppCheck
        )
        saveCloudUpdate(project.copy(backend = updatedBackend), if (isValid) "Firebase config validated & saved." else "Saved Firebase draft config.")
    }

    fun addDatabaseTable(engine: DatabaseEngine, name: String, columns: String, relations: String, indexName: String) {
        val project = activeProject.value ?: return
        val cleanName = name.trim().ifBlank { return }
        val table = DatabaseTableSpec(
            tableId = StarterBlueprints.newId("tbl"),
            engine = engine,
            name = cleanName,
            columnsSummary = columns.trim().ifBlank { "id UUID PK, created_at TIMESTAMPTZ" },
            relationsSummary = relations.trim(),
            indexSummary = indexName.trim()
        )
        val updated = project.backend.copy(databaseTables = project.backend.databaseTables + table)
        saveDesignUpdate(project.copy(backend = updated), "Added ${engine.displayName} table '$cleanName'")
    }

    fun generateMigrationFile() {
        val project = activeProject.value ?: return
        val sql = ProjectGenerators.generateSqlMigration(project.name, project.backend.databaseTables)
        upsertProjectFile(project, "backend/migrations/001_init.sql", "sql", CodeLayer.BACKEND, sql, "Generated SQL migration file")
    }

    fun saveApiRequest(spec: ApiRequestSpec) {
        val project = activeProject.value ?: return
        val existing = project.backend.apiRequests.indexOfFirst { it.requestId == spec.requestId }
        val nextList = if (existing >= 0) {
            project.backend.apiRequests.mapIndexed { i, item -> if (i == existing) spec else item }
        } else {
            project.backend.apiRequests + spec.copy(requestId = StarterBlueprints.newId("api"))
        }
        saveDesignUpdate(project.copy(backend = project.backend.copy(apiRequests = nextList)), "Saved API request '${spec.name}'")
    }

    fun executeApiRequest(spec: ApiRequestSpec) {
        viewModelScope.launch {
            _apiRunning.value = true
            _apiResult.value = httpClient.executeApiRequest(spec)
            _apiRunning.value = false
        }
    }

    fun generateKotlinClientForApi(spec: ApiRequestSpec) {
        val project = activeProject.value ?: return
        val file = ProjectGenerators.generateApiClientKotlin(project.packageName, spec)
        upsertProjectFile(project, file.path, file.language, file.layer, file.content, "Generated ${file.path.substringAfterLast('/')}")
    }

    fun addProjectAsset(name: String, category: AssetCategory, uriOrPath: String, mimeType: String) {
        val project = activeProject.value ?: return
        val normalized = name.lowercase().replace(Regex("[^a-z0-9_]"), "_").trim('_').ifBlank { "asset_${System.currentTimeMillis() % 10000}" }
        val asset = ProjectAsset(
            assetId = StarterBlueprints.newId("ast"),
            name = normalized,
            category = category,
            uriOrPath = uriOrPath,
            mimeType = mimeType
        )
        saveDesignUpdate(project.copy(backend = project.backend.copy(assets = project.backend.assets + asset)), "Added asset '$normalized'")
    }

    fun deleteProjectAsset(assetId: String) {
        val project = activeProject.value ?: return
        val updated = project.backend.assets.filterNot { it.assetId == assetId }
        saveDesignUpdate(project.copy(backend = project.backend.copy(assets = updated)), "Removed asset")
    }

    fun updateAndroidBuildConfig(config: AndroidBuildConfig, projectName: String, packageName: String) {
        val project = activeProject.value ?: return
        val updated = project.copy(
            name = projectName.trim().take(80).ifBlank { project.name },
            packageName = packageName.trim().take(120).ifBlank { project.packageName },
            backend = project.backend.copy(androidConfig = config)
        )
        saveDesignUpdate(updated, "Updated Android SDK & package configuration")
    }

    fun addFirestoreCollection(name: String, fields: String, accessRule: String) {
        val project = activeProject.value ?: return
        val cleanName = name.trim().ifBlank { return }
        val nextCols = project.backend.collections + FirestoreCollectionSpec(
            name = cleanName,
            fieldsSummary = fields.trim().ifBlank { "userId, title, createdAt" },
            accessRule = accessRule.trim().ifBlank { "Owner Only" }
        )
        val updated = project.backend.copy(
            collections = nextCols,
            firestoreRules = StarterBlueprints.generateHardenedRules(nextCols)
        )
        saveCloudUpdate(project.copy(backend = updated), "Added Firestore collection '$cleanName'")
    }

    fun removeFirestoreCollection(name: String) {
        val project = activeProject.value ?: return
        val nextCols = project.backend.collections.filterNot { it.name == name }
        val updated = project.backend.copy(collections = nextCols)
        saveCloudUpdate(project.copy(backend = updated), "Removed collection '$name'")
    }

    fun addCloudEndpoint(method: String, path: String, runtime: String, description: String) {
        val project = activeProject.value ?: return
        val cleanPath = path.trim().ifBlank { return }
        val updated = project.backend.copy(
            endpoints = project.backend.endpoints + CloudEndpointSpec(
                method = method.uppercase(),
                path = cleanPath,
                runtime = runtime.ifBlank { "Node.js TypeScript • Express" },
                description = description.ifBlank { "REST endpoint handler" }
            )
        )
        saveCloudUpdate(project.copy(backend = updated), "Added endpoint $method $cleanPath")
    }

    fun updateCloudInfra(region: String, tier: ScalingTier) {
        val project = activeProject.value ?: return
        saveCloudUpdate(project.copy(cloudRegion = region, scalingTier = tier), "Configured $region • ${tier.displayName}")
    }

    fun deployProject(environment: String, versionTag: String, summary: String) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("Deploying ${project.name} ($versionTag) to $environment...")
            val result = repository.deployRelease(
                userId = currentUserId,
                project = project,
                environment = environment,
                versionTag = versionTag.trim().ifBlank { "v1.0.1" },
                summary = summary.trim().ifBlank { "Full-stack release via BCF Builder AI" }
            )
            result.onSuccess { url ->
                _aiState.value = AiTaskState.Success("Release recorded at $url")
            }.onFailure { err ->
                _aiState.value = AiTaskState.Error(err.localizedMessage ?: "Deployment failed.")
            }
        }
    }

    fun rollbackDeployment(deploymentId: String) {
        viewModelScope.launch {
            repository.rollbackDeployment(currentUserId, deploymentId)
                .onSuccess { _statusBanner.value = "Deployment rolled back." }
                .onFailure { err -> _statusBanner.value = err.localizedMessage ?: "Rollback failed." }
        }
    }

    // --- Build Center & Diagnostics ---

    fun runDiagnostics() {
        val project = activeProject.value ?: return
        _diagnostics.value = ProjectGenerators.runProjectDiagnostics(project)
    }

    fun testBuildWorkerConnection() {
        val s = providerSettings.value
        viewModelScope.launch {
            _buildWorkerStatus.value = "Checking connection..."
            httpClient.pingBuildWorker(s.buildWorkerUrl, s.buildWorkerToken)
                .onSuccess { _buildWorkerStatus.value = "Connected: $it" }
                .onFailure { _buildWorkerStatus.value = "Not Connected: ${it.localizedMessage}" }
        }
    }

    fun triggerRemoteBuild(target: BuildTarget) {
        val project = activeProject.value ?: return
        val s = providerSettings.value
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("Submitting ${target.label} to Build Worker...")
            val payload = ProjectGenerators.exportProjectJson(project)
            httpClient.triggerRemoteBuild(
                workerBaseUrl = s.buildWorkerUrl,
                token = s.buildWorkerToken,
                projectName = project.name,
                packageName = project.packageName,
                target = target,
                filesPayloadJson = payload
            ).onSuccess { job ->
                _buildJobs.update { listOf(job) + it }
                _aiState.value = AiTaskState.Success("Build queued: ${job.buildId}")
            }.onFailure { err ->
                _aiState.value = AiTaskState.Error(err.localizedMessage ?: "Build submission failed.")
            }
        }
    }

    fun refreshBuildJob(job: BuildJobRecord) {
        val s = providerSettings.value
        if (s.buildWorkerUrl.isBlank()) return
        viewModelScope.launch {
            httpClient.pollBuildStatus(s.buildWorkerUrl, s.buildWorkerToken, job.buildId, job.target)
                .onSuccess { updated ->
                    _buildJobs.update { list -> list.map { if (it.buildId == job.buildId) updated else it } }
                }
        }
    }

    fun downloadBuildArtifact(job: BuildJobRecord, outputStream: OutputStream, onDone: (Result<Long>) -> Unit) {
        val s = providerSettings.value
        if (s.buildWorkerUrl.isBlank() || job.artifactUrl.isBlank()) {
            onDone(Result.failure(IllegalStateException("Build worker URL or artifact URL is unavailable.")))
            return
        }
        viewModelScope.launch {
            val res = httpClient.downloadBuildArtifact(s.buildWorkerUrl, s.buildWorkerToken, job.artifactUrl, outputStream)
            res.onSuccess { bytes ->
                _statusBanner.value = "Saved APK/AAB artifact (${bytes / 1024} KB)."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to download APK artifact."
            }
            onDone(res)
        }
    }

    // --- Preview & App Review ---

    fun runAiAppReview() {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            _aiState.value = AiTaskState.Working("Running AI App Review & Security Audit...")
            try {
                val report = aiEngine.runAppReview(project, providerSettings.value)
                _reviewReport.value = report
                _aiState.value = AiTaskState.Success("App Review Score: ${report.overallScore}/100")
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiTaskState.Error("App Review timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiTaskState.Error(e.localizedMessage ?: "App Review failed.")
            }
        }
    }

    fun updatePreviewScreen(screenId: String) {
        _previewState.update { it.copy(currentScreenId = screenId) }
    }

    fun togglePreviewDarkMode() {
        _previewState.update { it.copy(darkMode = !it.darkMode) }
    }

    fun togglePreviewTabletFrame() {
        _previewState.update { it.copy(tabletFrame = !it.tabletFrame) }
    }

    fun togglePreviewXray() {
        _previewState.update { it.copy(xrayMode = !it.xrayMode) }
    }

    fun onPreviewInputChange(componentId: String, value: String) {
        _previewState.update {
            it.copy(
                inputValues = it.inputValues + (componentId to value),
                variables = it.variables + ("inputText" to value)
            )
        }
        val project = activeProject.value ?: return
        val screen = project.screens.firstOrNull { it.screenId == _previewState.value.currentScreenId } ?: project.screens.firstOrNull() ?: return
        val matching = screen.logicBlocks.filter { it.targetWidgetId == componentId && it.eventTrigger == "onTextChange" }
        if (matching.isNotEmpty()) {
            executeRuntimeBlocks(project, screen, matching, "onTextChange($componentId)")
        }
    }

    fun onPreviewSwitchChange(componentId: String, checked: Boolean) {
        _previewState.update {
            it.copy(
                switchValues = it.switchValues + (componentId to checked),
                variables = it.variables + ("isChecked" to checked.toString()),
                traceLogs = (listOf("Toggle [$componentId] -> $checked") + it.traceLogs).take(16)
            )
        }
        val project = activeProject.value ?: return
        val screen = project.screens.firstOrNull { it.screenId == _previewState.value.currentScreenId } ?: project.screens.firstOrNull() ?: return
        val matching = screen.logicBlocks.filter { it.targetWidgetId == componentId && it.eventTrigger == "onCheckedChange" }
        if (matching.isNotEmpty()) {
            executeRuntimeBlocks(project, screen, matching, "onCheckedChange($componentId)")
        }
    }

    fun onPreviewSliderChange(componentId: String, value: Float) {
        _previewState.update {
            it.copy(
                sliderValues = it.sliderValues + (componentId to value),
                variables = it.variables + ("sliderValue" to value.toInt().toString())
            )
        }
    }

    fun onPreviewChipSelect(componentId: String, chip: String) {
        _previewState.update {
            it.copy(
                chipSelections = it.chipSelections + (componentId to chip),
                variables = it.variables + ("selectedOption" to chip),
                traceLogs = (listOf("Selection -> $chip") + it.traceLogs).take(16)
            )
        }
    }

    fun runPreviewCreateBlocks(project: AppProject) {
        val screen = project.screens.firstOrNull { it.screenId == _previewState.value.currentScreenId }
            ?: project.screens.firstOrNull() ?: return
        val createBlocks = screen.logicBlocks.filter {
            it.targetWidgetId.isBlank() || it.eventTrigger in listOf("onCreate", "onStart", "onResume")
        }
        executeRuntimeBlocks(project, screen, createBlocks, "${screen.title}.onCreate")
    }

    fun dismissPreviewDialog() {
        _previewState.update { it.copy(activeDialog = null) }
    }

    fun clearPreviewToast() {
        _previewState.update { it.copy(activeToast = null) }
    }

    fun clearPreviewLogcat() {
        _previewState.update { it.copy(logcatEntries = listOf("[INFO/Runtime] Logcat cleared")) }
    }

    fun onPreviewComponentClick(project: AppProject, component: AppComponent) {
        val screen = project.screens.firstOrNull { it.screenId == _previewState.value.currentScreenId }
            ?: project.screens.firstOrNull()
        val widgetBlocks = screen?.logicBlocks?.filter {
            it.targetWidgetId == component.componentId && it.eventTrigger in listOf("onClick", "onLongClick")
        }.orEmpty()

        if (widgetBlocks.isNotEmpty() && screen != null) {
            executeRuntimeBlocks(project, screen, widgetBlocks, "onClick(${component.text.ifBlank { component.type.displayName }})")
        }

        when (component.action) {
            com.example.model.ComponentAction.NAVIGATE -> {
                val targetScreen = project.screens.firstOrNull {
                    it.route.equals(component.actionTarget, ignoreCase = true) ||
                        it.title.equals(component.actionTarget, ignoreCase = true)
                }
                if (targetScreen != null) {
                    _previewState.update {
                        it.copy(
                            currentScreenId = targetScreen.screenId,
                            traceLogs = (listOf("Navigate -> ${targetScreen.title} (${targetScreen.route})") + it.traceLogs).take(16),
                            logcatEntries = (listOf("[INFO/Nav] Opened ${targetScreen.title} (${targetScreen.route})") + it.logcatEntries).take(40)
                        )
                    }
                } else {
                    _previewState.update {
                        it.copy(traceLogs = (listOf("Navigate target '${component.actionTarget}' not found") + it.traceLogs).take(16))
                    }
                }
            }
            com.example.model.ComponentAction.SUBMIT_FIRESTORE -> {
                _previewState.update {
                    it.copy(
                        activeToast = "Saved to /${component.actionTarget.ifBlank { "records" }}",
                        traceLogs = (listOf("Firestore Write -> /${component.actionTarget.ifBlank { "records" }}") + it.traceLogs).take(16),
                        logcatEntries = (listOf("[INFO/Firestore] Write -> /${component.actionTarget.ifBlank { "records" }}") + it.logcatEntries).take(40)
                    )
                }
            }
            com.example.model.ComponentAction.CALL_ENDPOINT -> {
                _previewState.update {
                    it.copy(
                        activeToast = "Called ${component.actionTarget.ifBlank { "/api/v1/action" }}",
                        traceLogs = (listOf("API Call -> ${component.actionTarget.ifBlank { "/api/v1/action" }}") + it.traceLogs).take(16),
                        logcatEntries = (listOf("[INFO/Network] Called ${component.actionTarget.ifBlank { "/api/v1/action" }}") + it.logcatEntries).take(40)
                    )
                }
            }
            com.example.model.ComponentAction.TRIGGER_AI -> {
                _previewState.update {
                    it.copy(
                        activeToast = "AI Trigger: ${component.actionTarget.ifBlank { component.text }}",
                        traceLogs = (listOf("AI Trigger -> '${component.actionTarget.ifBlank { component.text }}'") + it.traceLogs).take(16)
                    )
                }
            }
            com.example.model.ComponentAction.SHOW_SNACKBAR -> {
                val msg = component.actionTarget.ifBlank { component.text }
                _previewState.update {
                    it.copy(
                        activeToast = msg,
                        traceLogs = (listOf("Alert: $msg") + it.traceLogs).take(16)
                    )
                }
            }
            com.example.model.ComponentAction.NONE -> {
                if (widgetBlocks.isEmpty()) {
                    _previewState.update {
                        it.copy(traceLogs = (listOf("Tapped '${component.text}'") + it.traceLogs).take(16))
                    }
                }
            }
        }
    }

    private fun executeRuntimeBlocks(
        project: AppProject,
        screen: com.example.model.AppScreen,
        blocks: List<LogicBlock>,
        triggerLabel: String
    ) {
        val current = _previewState.value
        val result = BlockRuntimeEngine.executeBlocks(
            project = project,
            screen = screen,
            blocksToRun = blocks,
            currentVariables = current.variables,
            currentSharedPrefs = current.sharedPrefs,
            currentDynamicTexts = current.dynamicTexts,
            currentHiddenIds = current.hiddenComponentIds,
            triggerLabel = triggerLabel
        )
        val navScreenId = result.navigateToRoute?.let { target ->
            project.screens.firstOrNull {
                it.route.equals(target, ignoreCase = true) || it.title.equals(target, ignoreCase = true)
            }?.screenId
        }
        val formattedLogs = result.logs.map { it.formatLine() }.reversed()
        _previewState.update { prev ->
            prev.copy(
                currentScreenId = navScreenId ?: if (result.navigateBack) project.screens.firstOrNull()?.screenId else prev.currentScreenId,
                variables = result.variables,
                sharedPrefs = result.sharedPrefs,
                dynamicTexts = result.dynamicTexts,
                hiddenComponentIds = result.hiddenComponentIds,
                activeToast = result.toastMessage ?: prev.activeToast,
                activeDialog = if (result.dialogTitle != null) (result.dialogTitle to (result.dialogMessage ?: "")) else prev.activeDialog,
                traceLogs = (listOf("Executed ${blocks.size} block(s) via $triggerLabel") + prev.traceLogs).take(16),
                logcatEntries = (formattedLogs + prev.logcatEntries).take(60)
            )
        }
    }

    // --- Helpers ---

    private fun currentScreenFor(project: AppProject): AppScreen? {
        val id = _activeScreenId.value
        return project.screens.firstOrNull { it.screenId == id } ?: project.screens.firstOrNull()
    }

    private fun upsertProjectFile(
        project: AppProject,
        path: String,
        language: String,
        layer: CodeLayer,
        content: String,
        banner: String
    ) {
        val idx = project.files.indexOfFirst { it.path.equals(path, ignoreCase = true) }
        val updatedFiles = if (idx >= 0) {
            project.files.mapIndexed { i, f -> if (i == idx) f.copy(content = content) else f }
        } else {
            (project.files + ProjectFile(StarterBlueprints.newId("file"), path, language, layer, content)).take(60)
        }
        val next = project.copy(files = updatedFiles)
        _diagnostics.value = ProjectGenerators.runProjectDiagnostics(next)
        saveDesignUpdate(next, banner)
    }

    private fun saveDesignUpdate(updated: AppProject, banner: String? = null) {
        viewModelScope.launch {
            cacheSingleLocally(updated, dirty = true)
            repository.updateProjectDesign(currentUserId, updated)
                .onSuccess {
                    cacheSingleLocally(updated, dirty = false)
                    if (banner != null) _statusBanner.value = banner
                }
                .onFailure { err ->
                    _statusBanner.value = "Saved offline in Room; cloud sync error: ${err.localizedMessage}"
                }
        }
    }

    private fun saveCloudUpdate(updated: AppProject, banner: String? = null) {
        viewModelScope.launch {
            cacheSingleLocally(updated, dirty = true)
            repository.updateProjectCloud(currentUserId, updated)
                .onSuccess {
                    cacheSingleLocally(updated, dirty = false)
                    if (banner != null) _statusBanner.value = banner
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to update cloud config."
                }
        }
    }

    private fun cacheProjectsLocally(projects: List<AppProject>) {
        viewModelScope.launch(Dispatchers.IO) {
            projects.forEach { cacheSingleLocally(it, dirty = false) }
        }
    }

    private suspend fun cacheSingleLocally(project: AppProject, dirty: Boolean) {
        if (project.id.isBlank()) return
        withContext(Dispatchers.IO) {
            localDb.projectDao().upsertProject(
                LocalProjectEntity(
                    projectId = project.id,
                    userId = currentUserId,
                    name = project.name,
                    packageName = project.packageName,
                    jsonPayload = ProjectGenerators.exportProjectJson(project),
                    updatedAtMs = System.currentTimeMillis(),
                    dirtyOffline = dirty
                )
            )
        }
    }

    private suspend fun saveLocalSnapshot(project: AppProject, label: String) {
        if (project.id.isBlank()) return
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        withContext(Dispatchers.IO) {
            localDb.projectDao().insertSnapshot(
                ProjectSnapshotEntity(
                    snapshotId = StarterBlueprints.newId("snap"),
                    projectId = project.id,
                    userId = currentUserId,
                    label = "$label ($fmt)",
                    jsonPayload = ProjectGenerators.exportProjectJson(project),
                    createdAtMs = System.currentTimeMillis()
                )
            )
        }
    }
}
