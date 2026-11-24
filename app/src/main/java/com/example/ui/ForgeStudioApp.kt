package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.signOut
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.BackendApiStudioScreen
import com.example.ui.screens.BlocksEditorScreen
import com.example.ui.screens.BuildCenterScreen
import com.example.ui.screens.CodeExplorerScreen
import com.example.ui.screens.FirebaseDatabaseScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.PreviewReviewScreen
import com.example.ui.screens.ProjectWorkspaceScreen
import com.example.ui.screens.ProjectsHubScreen
import com.example.ui.screens.SettingsAdminHelpScreen
import com.example.ui.screens.TemplatesGalleryScreen
import com.example.ui.screens.VisualBuilderScreen
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.StudioTab
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeStudioApp(
    viewModel: StudioViewModel,
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    val providerSettings by viewModel.providerSettings.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val localCachedCount by viewModel.localCachedCount.collectAsStateWithLifecycle()
    val deploymentsState by viewModel.deploymentsState.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val activeScreenId by viewModel.activeScreenId.collectAsStateWithLifecycle()
    val selectedComponentId by viewModel.selectedComponentId.collectAsStateWithLifecycle()
    val selectedFileId by viewModel.selectedFileId.collectAsStateWithLifecycle()
    val selectedBlockWidgetId by viewModel.selectedBlockWidgetId.collectAsStateWithLifecycle()
    val selectedBlockEvent by viewModel.selectedBlockEvent.collectAsStateWithLifecycle()
    val lastAiGeneratedFiles by viewModel.lastAiGeneratedFiles.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val reviewReport by viewModel.reviewReport.collectAsStateWithLifecycle()
    val agentSteps by viewModel.agentSteps.collectAsStateWithLifecycle()
    val pendingPatches by viewModel.pendingPatches.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val apiResult by viewModel.apiResult.collectAsStateWithLifecycle()
    val apiRunning by viewModel.apiRunning.collectAsStateWithLifecycle()
    val buildWorkerStatus by viewModel.buildWorkerStatus.collectAsStateWithLifecycle()
    val buildJobs by viewModel.buildJobs.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAccountMenu by remember { mutableStateOf(false) }

    val primaryPhoneTabs = remember {
        listOf(
            StudioTab.HOME,
            StudioTab.WORKSPACE,
            StudioTab.AI_ARCHITECT,
            StudioTab.VISUAL_BUILDER,
            StudioTab.BLOCKS
        )
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    // Keep Navigation Compose backstack synchronized with ViewModel tab state
    LaunchedEffect(currentTab) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != currentTab.name) {
            navController.navigate(currentTab.name) {
                popUpTo(StudioTab.HOME.name) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Reflect system Back navigation back into ViewModel tab indicator
    LaunchedEffect(backStackEntry) {
        val route = backStackEntry?.destination?.route
        val matched = StudioTab.entries.firstOrNull { it.name == route }
        if (matched != null && matched != currentTab) {
            viewModel.selectTab(matched)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    Text(
                        text = activeProject?.name ?: "No active project selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    StudioTab.entries.forEach { tab ->
                        val selected = tab == currentTab
                        NavigationDrawerItem(
                            label = { Text(tab.label) },
                            selected = selected,
                            icon = { Icon(tabIcon(tab, selected), contentDescription = tab.label) },
                            onClick = {
                                viewModel.selectTab(tab)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 720.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("open_ide_drawer_button")
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open IDE workspace menu")
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentTab.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeProject != null) {
                                    Surface(
                                        onClick = { viewModel.selectTab(StudioTab.WORKSPACE) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = activeProject!!.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            Box {
                                IconButton(
                                    onClick = { showAccountMenu = true },
                                    modifier = Modifier.testTag("account_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Account menu"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showAccountMenu,
                                    onDismissRequest = { showAccountMenu = false }
                                ) {
                                    if (!userEmail.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(userEmail, style = MaterialTheme.typography.bodySmall) },
                                            onClick = {}
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Settings & AI Providers") },
                                        leadingIcon = { Icon(Icons.Filled.Settings, null) },
                                        onClick = {
                                            showAccountMenu = false
                                            viewModel.selectTab(StudioTab.SETTINGS)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.sign_out)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                                        onClick = {
                                            showAccountMenu = false
                                            signOut(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            primaryPhoneTabs.forEach { tab ->
                                val selected = tab == currentTab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(tabIcon(tab, selected), contentDescription = tab.label) },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.size(8.dp))
                            StudioTab.entries.forEach { tab ->
                                val selected = tab == currentTab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(tabIcon(tab, selected), contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedVisibility(visible = aiState !is AiTaskState.Idle) {
                            val bannerColor = when (aiState) {
                                is AiTaskState.Working -> MaterialTheme.colorScheme.primaryContainer
                                is AiTaskState.Success -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
                                is AiTaskState.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                                AiTaskState.Idle -> MaterialTheme.colorScheme.surface
                            }
                            val textColor = when (aiState) {
                                is AiTaskState.Working -> MaterialTheme.colorScheme.primary
                                is AiTaskState.Success -> MaterialTheme.colorScheme.tertiary
                                is AiTaskState.Error -> MaterialTheme.colorScheme.error
                                AiTaskState.Idle -> MaterialTheme.colorScheme.onSurface
                            }
                            Surface(
                                color = bannerColor,
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val label = when (val s = aiState) {
                                            is AiTaskState.Working -> s.label
                                            is AiTaskState.Success -> s.message
                                            is AiTaskState.Error -> s.message
                                            AiTaskState.Idle -> ""
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (aiState !is AiTaskState.Working) {
                                            IconButton(
                                                onClick = { viewModel.clearAiState() },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, "Dismiss", tint = textColor, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    if (aiState is AiTaskState.Working) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = StudioTab.HOME.name,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            composable(StudioTab.HOME.name) {
                                HomeDashboardScreen(
                                    activeProject = activeProject,
                                    projectsState = projectsState,
                                    localCachedCount = localCachedCount,
                                    providerSettings = providerSettings,
                                    buildWorkerStatus = buildWorkerStatus,
                                    onNavigateTab = viewModel::selectTab,
                                    onOpenProject = viewModel::openProjectInTab
                                )
                            }

                            composable(StudioTab.PROJECTS.name) {
                                ProjectsHubScreen(
                                    currentUserId = currentUserId,
                                    projectsState = projectsState,
                                    activeProject = activeProject,
                                    snapshots = snapshots,
                                    onOpenAiForge = { viewModel.selectTab(StudioTab.AI_ARCHITECT) },
                                    onCreateBlank = viewModel::createBlankProject,
                                    onCreateTemplate = viewModel::createFromTemplate,
                                    onDuplicateProject = viewModel::duplicateProject,
                                    onImportJson = viewModel::importProjectFromJson,
                                    onCreateSnapshot = viewModel::createVersionSnapshot,
                                    onRestoreSnapshot = viewModel::restoreSnapshot,
                                    onOpenProject = viewModel::openProjectInTab,
                                    onDeleteProject = viewModel::deleteProject
                                )
                            }

                            composable(StudioTab.WORKSPACE.name) {
                                ProjectWorkspaceScreen(
                                    project = activeProject,
                                    activeScreenId = activeScreenId,
                                    selectedComponentId = selectedComponentId,
                                    selectedFileId = selectedFileId,
                                    selectedBlockWidgetId = selectedBlockWidgetId,
                                    selectedBlockEvent = selectedBlockEvent,
                                    lastAiGeneratedFiles = lastAiGeneratedFiles,
                                    aiState = aiState,
                                    providerSettings = providerSettings,
                                    pendingPatches = pendingPatches,
                                    chatMessages = chatMessages,
                                    diagnostics = diagnostics,
                                    apiResult = apiResult,
                                    apiRunning = apiRunning,
                                    deploymentsState = deploymentsState,
                                    buildWorkerStatus = buildWorkerStatus,
                                    buildJobs = buildJobs,
                                    previewState = previewState,
                                    reviewReport = reviewReport,
                                    canUndo = viewModel.canUndo(),
                                    canRedo = viewModel.canRedo(),
                                    onDismissGeneratedBanner = viewModel::clearLastAiGeneratedFiles,
                                    onOpenAiStudio = { viewModel.selectTab(StudioTab.AI_ARCHITECT) },
                                    onOpenProjects = { viewModel.selectTab(StudioTab.PROJECTS) },
                                    onOpenSettings = { viewModel.selectTab(StudioTab.SETTINGS) },
                                    onUndo = viewModel::undoDesigner,
                                    onRedo = viewModel::redoDesigner,
                                    onSelectScreen = viewModel::selectScreen,
                                    onAddScreen = viewModel::addScreen,
                                    onDeleteScreen = viewModel::deleteScreen,
                                    onSelectComponent = viewModel::selectComponent,
                                    onAddComponent = viewModel::addComponentToActiveScreen,
                                    onUpdateComponent = viewModel::updateComponent,
                                    onMoveComponent = viewModel::moveComponent,
                                    onDuplicateComponent = viewModel::duplicateComponent,
                                    onDeleteComponent = viewModel::deleteComponent,
                                    onUpdateTheme = viewModel::updateThemePreset,
                                    onAiPopulateScreen = viewModel::populateActiveScreenWithAi,
                                    onSyncCode = viewModel::syncComposeCodeFromVisuals,
                                    onOpenBlocksForWidgetInWorkspace = { comp ->
                                        viewModel.openBlocksForWidget(comp, stayInWorkspace = true)
                                    },
                                    onSelectBlockTarget = viewModel::selectBlockTarget,
                                    onAddBlock = viewModel::addLogicBlock,
                                    onGenerateBlocksWithAi = viewModel::generateBlocksWithAi,
                                    onUpdateBlock = viewModel::updateLogicBlock,
                                    onMoveBlock = viewModel::moveLogicBlock,
                                    onDeleteBlock = viewModel::deleteLogicBlock,
                                    onCompileBlocks = viewModel::compileBlocksToKotlin,
                                    onAddSketchComponent = viewModel::addSketchComponent,
                                    onDeleteSketchComponent = viewModel::deleteSketchComponent,
                                    onOpenBlocksForSketchComponent = { comp, ev ->
                                        viewModel.openBlocksForSketchComponent(comp, ev, stayInWorkspace = true)
                                    },
                                    onAddCustomMoreBlock = viewModel::addCustomMoreBlock,
                                    onDeleteCustomMoreBlock = viewModel::deleteCustomMoreBlock,
                                    onOpenBlocksForMoreBlock = { mb ->
                                        viewModel.openBlocksForMoreBlock(mb, stayInWorkspace = true)
                                    },
                                    onToggleProjectLibrary = viewModel::toggleProjectLibrary,
                                    onToggleProjectPermission = viewModel::toggleProjectPermission,
                                    onUpsertStringResource = viewModel::upsertStringResource,
                                    onRemoveStringResource = viewModel::removeStringResource,
                                    onUpsertColorResource = viewModel::upsertColorResource,
                                    onRemoveColorResource = viewModel::removeColorResource,
                                    onSelectFile = viewModel::selectFile,
                                    onSaveFileContent = viewModel::saveFileContent,
                                    onAddCodeFile = viewModel::addCodeFile,
                                    onDeleteCodeFile = viewModel::deleteCodeFile,
                                    onAiRefactorFile = viewModel::refactorFileWithAi,
                                    onAiExplainFile = viewModel::explainFileWithAi,
                                    onSendAssistantMessage = viewModel::sendAssistantMessage,
                                    onApplyPatches = viewModel::applyPendingPatches,
                                    onDiscardPatches = viewModel::discardPendingPatches,
                                    onUpdateFirebaseConfig = viewModel::updateFirebaseSetupConfig,
                                    onAddCollection = viewModel::addFirestoreCollection,
                                    onRemoveCollection = viewModel::removeFirestoreCollection,
                                    onAddDatabaseTable = viewModel::addDatabaseTable,
                                    onGenerateMigration = viewModel::generateMigrationFile,
                                    onDeployProject = viewModel::deployProject,
                                    onRollbackDeployment = viewModel::rollbackDeployment,
                                    onUpdateCloudInfra = viewModel::updateCloudInfra,
                                    onUpdateSecurityToggles = viewModel::updateBackendSecurityToggles,
                                    onAddEndpoint = viewModel::addCloudEndpoint,
                                    onGenerateNodeBackend = viewModel::generateNodeTsBackendFiles,
                                    onSyncAndroidGradle = viewModel::syncAndroidStudioStructure,
                                    onSaveApiRequest = viewModel::saveApiRequest,
                                    onExecuteApiRequest = viewModel::executeApiRequest,
                                    onGenerateKotlinClient = viewModel::generateKotlinClientForApi,
                                    onSelectPreviewScreen = viewModel::updatePreviewScreen,
                                    onToggleDarkMode = viewModel::togglePreviewDarkMode,
                                    onToggleTablet = viewModel::togglePreviewTabletFrame,
                                    onToggleXray = viewModel::togglePreviewXray,
                                    onPreviewComponentTap = viewModel::onPreviewComponentClick,
                                    onPreviewInputChange = viewModel::onPreviewInputChange,
                                    onPreviewSwitchChange = viewModel::onPreviewSwitchChange,
                                    onPreviewSliderChange = viewModel::onPreviewSliderChange,
                                    onPreviewChipSelect = viewModel::onPreviewChipSelect,
                                    onRunAiReview = viewModel::runAiAppReview,
                                    onRunCreateBlocks = viewModel::runPreviewCreateBlocks,
                                    onDismissPreviewDialog = viewModel::dismissPreviewDialog,
                                    onClearPreviewToast = viewModel::clearPreviewToast,
                                    onClearLogcat = viewModel::clearPreviewLogcat,
                                    onRunDiagnostics = viewModel::runDiagnostics,
                                    onExportZip = viewModel::exportProjectZip,
                                    onTestWorkerConnection = viewModel::testBuildWorkerConnection,
                                    onTriggerRemoteBuild = viewModel::triggerRemoteBuild,
                                    onRefreshBuildJob = viewModel::refreshBuildJob,
                                    onDownloadBuildArtifact = viewModel::downloadBuildArtifact,
                                    onAddAsset = viewModel::addProjectAsset,
                                    onDeleteAsset = viewModel::deleteProjectAsset
                                )
                            }

                            composable(StudioTab.AI_ARCHITECT.name) {
                                AiStudioScreen(
                                    activeProject = activeProject,
                                    aiState = aiState,
                                    providerSettings = providerSettings,
                                    agentSteps = agentSteps,
                                    pendingPatches = pendingPatches,
                                    chatMessages = chatMessages,
                                    onRunAiTask = viewModel::runAiStudioTask,
                                    onApplyPatches = viewModel::applyPendingPatches,
                                    onDiscardPatches = viewModel::discardPendingPatches,
                                    onSendAssistantMessage = viewModel::sendAssistantMessage
                                )
                            }

                            composable(StudioTab.VISUAL_BUILDER.name) {
                                VisualBuilderScreen(
                                    project = activeProject,
                                    activeScreenId = activeScreenId,
                                    selectedComponentId = selectedComponentId,
                                    canUndo = viewModel.canUndo(),
                                    canRedo = viewModel.canRedo(),
                                    onUndo = viewModel::undoDesigner,
                                    onRedo = viewModel::redoDesigner,
                                    onSelectScreen = viewModel::selectScreen,
                                    onAddScreen = viewModel::addScreen,
                                    onDeleteScreen = viewModel::deleteScreen,
                                    onSelectComponent = viewModel::selectComponent,
                                    onAddComponent = viewModel::addComponentToActiveScreen,
                                    onUpdateComponent = viewModel::updateComponent,
                                    onMoveComponent = viewModel::moveComponent,
                                    onDuplicateComponent = viewModel::duplicateComponent,
                                    onDeleteComponent = viewModel::deleteComponent,
                                    onUpdateTheme = viewModel::updateThemePreset,
                                    onAiPopulateScreen = viewModel::populateActiveScreenWithAi,
                                    onSyncCode = viewModel::syncComposeCodeFromVisuals,
                                    onOpenBlocksForWidget = { comp ->
                                        viewModel.openBlocksForWidget(comp, stayInWorkspace = false)
                                    }
                                )
                            }

                            composable(StudioTab.BLOCKS.name) {
                                BlocksEditorScreen(
                                    project = activeProject,
                                    activeScreenId = activeScreenId,
                                    selectedWidgetId = selectedBlockWidgetId,
                                    selectedEvent = selectedBlockEvent,
                                    onSelectScreen = viewModel::selectScreen,
                                    onSelectBlockTarget = viewModel::selectBlockTarget,
                                    onAddBlock = viewModel::addLogicBlock,
                                    onGenerateBlocksWithAi = viewModel::generateBlocksWithAi,
                                    onUpdateBlock = viewModel::updateLogicBlock,
                                    onMoveBlock = viewModel::moveLogicBlock,
                                    onDeleteBlock = viewModel::deleteLogicBlock,
                                    onCompileBlocks = viewModel::compileBlocksToKotlin,
                                    onViewCompiledCode = {
                                        val proj = activeProject
                                        val scr = proj?.screens?.firstOrNull { it.screenId == activeScreenId } ?: proj?.screens?.firstOrNull()
                                        val safeClass = scr?.title?.replace(Regex("[^A-Za-z0-9]"), "")?.ifBlank { "Main" } ?: "Main"
                                        val compiledFile = proj?.files?.firstOrNull { it.path.endsWith("${safeClass}BlockController.kt") }
                                        if (compiledFile != null) viewModel.selectFile(compiledFile.fileId)
                                        viewModel.selectTab(StudioTab.WORKSPACE)
                                    },
                                    onRunInPreview = {
                                        activeProject?.let { viewModel.runPreviewCreateBlocks(it) }
                                        viewModel.selectTab(StudioTab.PREVIEW_REVIEW)
                                    }
                                )
                            }

                            composable(StudioTab.CODE_TREE.name) {
                                CodeExplorerScreen(
                                    project = activeProject,
                                    selectedFileId = selectedFileId,
                                    aiState = aiState,
                                    editorFontSizeSp = providerSettings.editorFontSizeSp,
                                    onSelectFile = viewModel::selectFile,
                                    onSaveFileContent = viewModel::saveFileContent,
                                    onAddCodeFile = viewModel::addCodeFile,
                                    onDeleteCodeFile = viewModel::deleteCodeFile,
                                    onAiRefactorFile = viewModel::refactorFileWithAi,
                                    onAiExplainFile = viewModel::explainFileWithAi
                                )
                            }

                            composable(StudioTab.PREVIEW_REVIEW.name) {
                                PreviewReviewScreen(
                                    project = activeProject,
                                    previewState = previewState,
                                    reviewReport = reviewReport,
                                    aiState = aiState,
                                    onSelectPreviewScreen = viewModel::updatePreviewScreen,
                                    onToggleDarkMode = viewModel::togglePreviewDarkMode,
                                    onToggleTablet = viewModel::togglePreviewTabletFrame,
                                    onToggleXray = viewModel::togglePreviewXray,
                                    onComponentTap = viewModel::onPreviewComponentClick,
                                    onInputChange = viewModel::onPreviewInputChange,
                                    onSwitchChange = viewModel::onPreviewSwitchChange,
                                    onSliderChange = viewModel::onPreviewSliderChange,
                                    onChipSelect = viewModel::onPreviewChipSelect,
                                    onRunAiReview = viewModel::runAiAppReview,
                                    onRunCreateBlocks = viewModel::runPreviewCreateBlocks,
                                    onDismissPreviewDialog = viewModel::dismissPreviewDialog,
                                    onClearPreviewToast = viewModel::clearPreviewToast,
                                    onClearLogcat = viewModel::clearPreviewLogcat
                                )
                            }

                            composable(StudioTab.BACKEND.name) {
                                BackendApiStudioScreen(
                                    project = activeProject,
                                    initialTab = 0,
                                    apiResult = apiResult,
                                    apiRunning = apiRunning,
                                    onUpdateCloudInfra = viewModel::updateCloudInfra,
                                    onUpdateSecurityToggles = viewModel::updateBackendSecurityToggles,
                                    onAddEndpoint = viewModel::addCloudEndpoint,
                                    onGenerateNodeBackend = viewModel::generateNodeTsBackendFiles,
                                    onSyncAndroidGradle = viewModel::syncAndroidStudioStructure,
                                    onSaveApiRequest = viewModel::saveApiRequest,
                                    onExecuteApiRequest = viewModel::executeApiRequest,
                                    onGenerateKotlinClient = viewModel::generateKotlinClientForApi
                                )
                            }

                            composable(StudioTab.API_MANAGER.name) {
                                BackendApiStudioScreen(
                                    project = activeProject,
                                    initialTab = 1,
                                    apiResult = apiResult,
                                    apiRunning = apiRunning,
                                    onUpdateCloudInfra = viewModel::updateCloudInfra,
                                    onUpdateSecurityToggles = viewModel::updateBackendSecurityToggles,
                                    onAddEndpoint = viewModel::addCloudEndpoint,
                                    onGenerateNodeBackend = viewModel::generateNodeTsBackendFiles,
                                    onSyncAndroidGradle = viewModel::syncAndroidStudioStructure,
                                    onSaveApiRequest = viewModel::saveApiRequest,
                                    onExecuteApiRequest = viewModel::executeApiRequest,
                                    onGenerateKotlinClient = viewModel::generateKotlinClientForApi
                                )
                            }

                            composable(StudioTab.FIREBASE.name) {
                                FirebaseDatabaseScreen(
                                    project = activeProject,
                                    initialTab = 0,
                                    deploymentsState = deploymentsState,
                                    aiState = aiState,
                                    onUpdateFirebaseConfig = viewModel::updateFirebaseSetupConfig,
                                    onAddCollection = viewModel::addFirestoreCollection,
                                    onRemoveCollection = viewModel::removeFirestoreCollection,
                                    onAddDatabaseTable = viewModel::addDatabaseTable,
                                    onGenerateMigration = viewModel::generateMigrationFile,
                                    onDeployProject = viewModel::deployProject,
                                    onRollbackDeployment = viewModel::rollbackDeployment
                                )
                            }

                            composable(StudioTab.DATABASE.name) {
                                FirebaseDatabaseScreen(
                                    project = activeProject,
                                    initialTab = 1,
                                    deploymentsState = deploymentsState,
                                    aiState = aiState,
                                    onUpdateFirebaseConfig = viewModel::updateFirebaseSetupConfig,
                                    onAddCollection = viewModel::addFirestoreCollection,
                                    onRemoveCollection = viewModel::removeFirestoreCollection,
                                    onAddDatabaseTable = viewModel::addDatabaseTable,
                                    onGenerateMigration = viewModel::generateMigrationFile,
                                    onDeployProject = viewModel::deployProject,
                                    onRollbackDeployment = viewModel::rollbackDeployment
                                )
                            }

                            composable(StudioTab.BUILD_CENTER.name) {
                                BuildCenterScreen(
                                    project = activeProject,
                                    diagnostics = diagnostics,
                                    providerSettings = providerSettings,
                                    buildWorkerStatus = buildWorkerStatus,
                                    buildJobs = buildJobs,
                                    onRunDiagnostics = viewModel::runDiagnostics,
                                    onAiFixIssues = { viewModel.selectTab(StudioTab.AI_ARCHITECT) },
                                    onExportZip = viewModel::exportProjectZip,
                                    onTestWorkerConnection = viewModel::testBuildWorkerConnection,
                                    onTriggerRemoteBuild = viewModel::triggerRemoteBuild,
                                    onRefreshBuildJob = viewModel::refreshBuildJob,
                                    onDownloadBuildArtifact = viewModel::downloadBuildArtifact,
                                    onAddAsset = viewModel::addProjectAsset,
                                    onDeleteAsset = viewModel::deleteProjectAsset,
                                    onOpenSettings = { viewModel.selectTab(StudioTab.SETTINGS) }
                                )
                            }

                            composable(StudioTab.TEMPLATES.name) {
                                TemplatesGalleryScreen(
                                    currentUserId = currentUserId,
                                    onCreateTemplate = viewModel::createFromTemplate,
                                    onCreateBlank = viewModel::createBlankProject
                                )
                            }

                            composable(StudioTab.SETTINGS.name) {
                                SettingsAdminHelpScreen(
                                    initialTab = 0,
                                    activeProject = activeProject,
                                    providerSettings = providerSettings,
                                    localCachedCount = localCachedCount,
                                    buildJobsCount = buildJobs.size,
                                    onSaveProviderSettings = viewModel::saveProviderSettings,
                                    onUpdateAndroidBuildConfig = viewModel::updateAndroidBuildConfig,
                                    onTestWorkerConnection = viewModel::testBuildWorkerConnection
                                )
                            }

                            composable(StudioTab.HELP.name) {
                                SettingsAdminHelpScreen(
                                    initialTab = 1,
                                    activeProject = activeProject,
                                    providerSettings = providerSettings,
                                    localCachedCount = localCachedCount,
                                    buildJobsCount = buildJobs.size,
                                    onSaveProviderSettings = viewModel::saveProviderSettings,
                                    onUpdateAndroidBuildConfig = viewModel::updateAndroidBuildConfig,
                                    onTestWorkerConnection = viewModel::testBuildWorkerConnection
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: StudioTab, selected: Boolean): ImageVector = when (tab) {
    StudioTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    StudioTab.PROJECTS -> if (selected) Icons.Filled.Apps else Icons.Outlined.Apps
    StudioTab.WORKSPACE -> if (selected) Icons.Filled.DeveloperBoard else Icons.Outlined.DeveloperBoard
    StudioTab.AI_ARCHITECT -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    StudioTab.VISUAL_BUILDER -> if (selected) Icons.Filled.Widgets else Icons.Outlined.Widgets
    StudioTab.BLOCKS -> if (selected) Icons.Filled.Extension else Icons.Outlined.Extension
    StudioTab.CODE_TREE -> if (selected) Icons.Filled.Code else Icons.Outlined.Code
    StudioTab.PREVIEW_REVIEW -> Icons.Filled.PlayArrow
    StudioTab.BACKEND -> Icons.Filled.Dns
    StudioTab.FIREBASE -> Icons.Filled.LocalFireDepartment
    StudioTab.API_MANAGER -> Icons.Filled.Api
    StudioTab.DATABASE -> Icons.Filled.Storage
    StudioTab.BUILD_CENTER -> Icons.Filled.Build
    StudioTab.TEMPLATES -> Icons.Filled.FolderCopy
    StudioTab.SETTINGS -> Icons.Filled.Settings
    StudioTab.HELP -> Icons.AutoMirrored.Filled.HelpOutline
}
