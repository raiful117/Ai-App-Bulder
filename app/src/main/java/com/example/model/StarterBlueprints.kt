package com.example.model

import java.util.UUID

object StarterBlueprints {

    fun newId(prefix: String = "id"): String =
        "${prefix}_${UUID.randomUUID().toString().take(8)}"

    fun generateComposeScreenCode(projectName: String, screen: AppScreen): String {
        val safeClassName = screen.title.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Main" }
        val pkgSlug = projectName.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "app" }
        val componentCalls = screen.components.joinToString("\n") { comp ->
            when (comp.type) {
                ComponentType.TOP_APP_BAR -> """        TopAppBar(title = { Text("${comp.text}") })"""
                ComponentType.HERO_BANNER -> """        HeroCard(title = "${comp.text}", subtitle = "${comp.subtitle}")"""
                ComponentType.HEADLINE -> """        Text("${comp.text}", style = MaterialTheme.typography.headlineSmall)"""
                ComponentType.BODY_TEXT -> """        Text("${comp.text}", style = MaterialTheme.typography.bodyMedium)"""
                ComponentType.TEXT_INPUT -> """        OutlinedTextField(value = "", onValueChange = {}, label = { Text("${comp.text}") }, modifier = Modifier.fillMaxWidth())"""
                ComponentType.PASSWORD_FIELD -> """        OutlinedTextField(value = "", onValueChange = {}, label = { Text("${comp.text}") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())"""
                ComponentType.PRIMARY_BUTTON -> """        Button(onClick = { /* ${comp.action.name}: ${comp.actionTarget} */ }, modifier = Modifier.fillMaxWidth()) { Text("${comp.text}") }"""
                ComponentType.OUTLINED_BUTTON -> """        OutlinedButton(onClick = { /* ${comp.action.name} */ }, modifier = Modifier.fillMaxWidth()) { Text("${comp.text}") }"""
                ComponentType.CHECKBOX -> """        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = true, onCheckedChange = {}); Text("${comp.text}") }"""
                ComponentType.SWITCH_TOGGLE -> """        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${comp.text}"); Switch(checked = true, onCheckedChange = {}) }"""
                ComponentType.LIST_TILE, ComponentType.RECYCLER_LIST -> """        ListItem(headlineContent = { Text("${comp.text}") }, supportingContent = { Text("${comp.subtitle}") })"""
                ComponentType.DATA_CHART -> """        AnalyticsBarChart(title = "${comp.text}", series = listOf(${comp.subtitle.ifBlank { "40, 65, 80" }}))"""
                ComponentType.BOTTOM_NAV -> """        NavigationBar { /* Tabs: ${comp.text} */ }"""
                ComponentType.WEB_VIEW -> """        AndroidView(factory = { ctx -> android.webkit.WebView(ctx).apply { loadUrl("${comp.text}") } })"""
                else -> """        SurfaceCard(title = "${comp.text}", subtitle = "${comp.subtitle}") // ${comp.type.displayName}"""
            }
        }
        return """package com.bcf.$pkgSlug.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ${safeClassName}Screen(
    onNavigate: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
$componentCalls
    }
}
"""
    }

    fun blankProject(userId: String, name: String = "My Custom App"): AppProject {
        val homeScreen = AppScreen(
            screenId = "home",
            title = "Home",
            route = "home",
            iconName = "Home",
            components = listOf(
                AppComponent(
                    componentId = newId("cmp"),
                    type = ComponentType.TOP_APP_BAR,
                    text = name,
                    subtitle = "BCF Ready",
                    iconName = "Bolt"
                ),
                AppComponent(
                    componentId = newId("cmp"),
                    type = ComponentType.HERO_BANNER,
                    text = "Build Visually, with Blocks or AI",
                    subtitle = "Design UI, connect visual logic blocks, edit Kotlin code, and ship full-stack backends.",
                    colorRole = ColorRole.PRIMARY
                ),
                AppComponent(
                    componentId = newId("cmp"),
                    type = ComponentType.PRIMARY_BUTTON,
                    text = "Get Started",
                    action = ComponentAction.SHOW_SNACKBAR,
                    actionTarget = "Action triggered from Home!",
                    colorRole = ColorRole.PRIMARY
                )
            ),
            logicBlocks = listOf(
                LogicBlock(newId("blk"), BlockOpType.ON_SCREEN_CREATE, "Home"),
                LogicBlock(newId("blk"), BlockOpType.UI_TOAST, "Welcome to $name!")
            )
        )
        val pkg = "com.bcf.${name.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "myapp" }}"
        return AppProject(
            userId = userId,
            name = name,
            packageName = pkg,
            tagline = "Custom full-stack Jetpack Compose application",
            themePreset = AppThemePreset.OBSIDIAN_CYAN,
            architecture = ArchitecturePattern.CLEAN_MVVM,
            cloudRegion = "asia-southeast1",
            scalingTier = ScalingTier.SERVERLESS_AUTO,
            deploymentStatus = DeploymentStatus.DRAFT,
            activeVersion = "v1.0.0",
            screens = listOf(homeScreen),
            files = defaultFilesFor(pkg, name, listOf(homeScreen)),
            backend = defaultBackendFor(name)
        )
    }

    fun templates(userId: String): List<AppProject> = listOf(
        blankProject(userId, "Blank Android App"),
        quickTemplate(
            userId = userId,
            name = "Material Design 3 App",
            tagline = "Adaptive M3 cards, chips, sliders & bottom navigation",
            theme = AppThemePreset.INDIGO_PULSE,
            arch = ArchitecturePattern.CLEAN_MVVM,
            screenTitle = "Showcase",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Material 3 Showcase", "Dynamic UI", "Palette", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.HERO_BANNER, "Adaptive Surface System", "Smooth springs, neon glass cards & M3 tokens", "AutoAwesome", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.CHIP_GROUP, "Components, Typography, Motion, Color", "", "Widgets", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.SLIDER_CONTROL, "Corner Radius & Elevation", "60", "Tune", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.BOTTOM_NAV, "Showcase, Explore, Settings", "", "Dashboard", ColorRole.SURFACE)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Login & Auth App",
            tagline = "Email/Password & Google Sign-In onboarding with session validation",
            theme = AppThemePreset.OBSIDIAN_CYAN,
            arch = ArchitecturePattern.CLEAN_MVVM,
            screenTitle = "Login",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Account Access", "Zero-Trust", "Lock", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.TEXT_INPUT, "Email Address", "architect@bcf.dev", "Person", ColorRole.SURFACE),
                AppComponent(newId("c"), ComponentType.PASSWORD_FIELD, "Password", "••••••••", "Lock", ColorRole.SURFACE),
                AppComponent(newId("c"), ComponentType.CHECKBOX, "Keep me signed in", "true", "CheckCircle", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.PRIMARY_BUTTON, "Sign In", "", "Lock", ColorRole.PRIMARY, ComponentAction.NAVIGATE, "dashboard")
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Firebase Full-Stack App",
            tagline = "Credential Manager Google Sign-In, Firestore & App Check rules",
            theme = AppThemePreset.EMERALD_CLOUD,
            arch = ArchitecturePattern.CLEAN_MVVM,
            screenTitle = "Workspace",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Firebase Cloud Hub", "App Check", "Security", ColorRole.SUCCESS),
                AppComponent(newId("c"), ComponentType.STAT_METRIC, "14,820", "Realtime Firestore Snapshot Reads", "Analytics", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.LIST_TILE, "Sync Collection /users/{uid}/notes", "Owner-scoped zero-trust rules", "Storage", ColorRole.SURFACE, ComponentAction.SUBMIT_FIRESTORE, "notes")
            )
        ),
        omniCartTemplate(userId),
        quickTemplate(
            userId = userId,
            name = "Social Media App",
            tagline = "Feed, posts, image uploads, likes, comments & notifications",
            theme = AppThemePreset.INDIGO_PULSE,
            arch = ArchitecturePattern.MVI_MODULAR,
            screenTitle = "Feed",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "PulseSocial", "Live Feed", "Bolt", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.AVATAR_ROW, "@amina_builds", "Shipped a new Compose layout 4m ago", "Person", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.IMAGE_BANNER, "Creator Showcase Post", "1.2K likes • 84 comments • Share", "Star", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.FAB_BUTTON, "New Post", "", "Add", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.BOTTOM_NAV, "Feed, Reels, Messages, Profile", "", "Home", ColorRole.SURFACE)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Real-Time Chat App",
            tagline = "1:1 & group channels with Firestore listeners & FCM push",
            theme = AppThemePreset.OBSIDIAN_CYAN,
            arch = ArchitecturePattern.CLEAN_MVVM,
            screenTitle = "Chats",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "HyperChat", "Encrypted", "Notifications", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.LIST_TILE, "Backend Engineering Team", "Deployment v2.4 succeeded", "Person", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.LIST_TILE, "Design Systems Guild", "Updated dark neon tokens", "Palette", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.TEXT_INPUT, "Quick Message", "Type a message...", "Edit", ColorRole.SURFACE)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Music Streaming App",
            tagline = "Playlists, Media3 playback controls & offline downloads",
            theme = AppThemePreset.INDIGO_PULSE,
            arch = ArchitecturePattern.FEATURE_FIRST,
            screenTitle = "Player",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "SonicWave", "Hi-Fi 24-bit", "Bolt", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.IMAGE_BANNER, "Midnight Synthwave Mix", "Playing Track 3 of 18 • 02:14 / 04:08", "Star", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.SLIDER_CONTROL, "Track Scrubber", "55", "PlayArrow", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.RECYCLER_LIST, "Up Next Queue", "Neon Horizon, Cyber Rain, Starlight", "Widgets", ColorRole.SURFACE)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Video Streaming App",
            tagline = "Adaptive HLS video player, channels & watchlists",
            theme = AppThemePreset.SUNSET_CORAL,
            arch = ArchitecturePattern.FEATURE_FIRST,
            screenTitle = "Watch",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "StreamForge", "4K HDR", "Bolt", ColorRole.WARNING),
                AppComponent(newId("c"), ComponentType.VIDEO_PLAYER, "Keynote: AI Native Mobile IDEs", "1080p • Media3 ExoPlayer", "PlayArrow", ColorRole.ERROR),
                AppComponent(newId("c"), ComponentType.RECYCLER_LIST, "Recommended Channels", "Kotlin Conf, Compose Mastery, Cloud Run Deep Dive", "Widgets", ColorRole.SURFACE)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "News & Media App",
            tagline = "Breaking headlines, categories, bookmarks & offline reader",
            theme = AppThemePreset.MONOCHROME_PRO,
            arch = ArchitecturePattern.CLEAN_MVVM,
            screenTitle = "Headlines",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "TechPulse Daily", "Live Edition", "Bolt", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.CHIP_GROUP, "Top, AI, Mobile, Cloud, Security", "", "Category", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.LIST_TILE, "Jetpack Compose 2.0 Performance Guide", "6 min read • Saved", "Star", ColorRole.SURFACE)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Analytics Dashboard App",
            tagline = "Executive KPIs, revenue charts & multi-region telemetry",
            theme = AppThemePreset.OBSIDIAN_CYAN,
            arch = ArchitecturePattern.MVI_MODULAR,
            screenTitle = "Dashboard",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Telemetry Ops", "99.98% SLA", "Analytics", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.ROW_STATS, "MRR: $82.4K", "Active Users: 19.3K", "TrendingUp", ColorRole.SUCCESS),
                AppComponent(newId("c"), ComponentType.DATA_CHART, "API Throughput (req/s)", "48,62,79,88,92,95,99", "Analytics", ColorRole.PRIMARY)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "REST API Client App",
            tagline = "OkHttp REST integration, JSON parsing & Bearer auth",
            theme = AppThemePreset.EMERALD_CLOUD,
            arch = ArchitecturePattern.CLEAN_MVVM,
            screenTitle = "Explorer",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "REST Explorer", "OkHttp 4", "Bolt", ColorRole.SUCCESS),
                AppComponent(newId("c"), ComponentType.TEXT_INPUT, "API Endpoint URL", "https://api.github.com/users", "Link", ColorRole.SURFACE),
                AppComponent(newId("c"), ComponentType.PRIMARY_BUTTON, "Fetch Live JSON", "", "Bolt", ColorRole.SUCCESS, ComponentAction.CALL_ENDPOINT, "/api/v1/fetch")
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Offline-First Room App",
            tagline = "Local SQLite Room persistence, WorkManager sync & notes",
            theme = AppThemePreset.MONOCHROME_PRO,
            arch = ArchitecturePattern.CLEAN_MVVM,
            screenTitle = "Vault",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Offline Vault", "Room SQLite", "Storage", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.SWITCH_TOGGLE, "Background WorkManager Sync", "true", "Sync", ColorRole.SUCCESS),
                AppComponent(newId("c"), ComponentType.RECYCLER_LIST, "Cached Local Entities", "Task #1, Note #2, Draft #3", "Widgets", ColorRole.SURFACE)
            )
        ),
        quickTemplate(
            userId = userId,
            name = "Admin Control App",
            tagline = "Role-based user moderation, quotas, audit logs & flags",
            theme = AppThemePreset.SUNSET_CORAL,
            arch = ArchitecturePattern.FEATURE_FIRST,
            screenTitle = "Admin",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Platform Admin", "RBAC Guard", "Lock", ColorRole.ERROR),
                AppComponent(newId("c"), ComponentType.ROW_STATS, "Active Workers: 12", "Queue Depth: 3", "Analytics", ColorRole.WARNING),
                AppComponent(newId("c"), ComponentType.SWITCH_TOGGLE, "Maintenance Mode", "false", "Lock", ColorRole.ERROR)
            )
        ),
        finLedgerTemplate(userId)
    )

    private fun quickTemplate(
        userId: String,
        name: String,
        tagline: String,
        theme: AppThemePreset,
        arch: ArchitecturePattern,
        screenTitle: String,
        components: List<AppComponent>
    ): AppProject {
        val route = screenTitle.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "home" }
        val screen = AppScreen(
            screenId = route,
            title = screenTitle,
            route = route,
            iconName = "Dashboard",
            components = components,
            logicBlocks = listOf(
                LogicBlock(newId("blk"), BlockOpType.ON_SCREEN_CREATE, screenTitle),
                LogicBlock(newId("blk"), BlockOpType.FB_AUTH_CHECK, "requireCurrentUser()")
            )
        )
        val pkg = "com.bcf.${name.lowercase().replace(Regex("[^a-z0-9]"), "").take(18).ifBlank { "starter" }}"
        return AppProject(
            userId = userId,
            name = name,
            packageName = pkg,
            tagline = tagline,
            themePreset = theme,
            architecture = arch,
            cloudRegion = "asia-southeast1",
            scalingTier = ScalingTier.SERVERLESS_AUTO,
            deploymentStatus = DeploymentStatus.DRAFT,
            activeVersion = "v1.0.0",
            screens = listOf(screen),
            files = defaultFilesFor(pkg, name, listOf(screen)),
            backend = defaultBackendFor(name)
        )
    }

    private fun omniCartTemplate(userId: String): AppProject {
        val home = AppScreen(
            screenId = "storefront",
            title = "Storefront",
            route = "storefront",
            iconName = "ShoppingBag",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "OmniCart Pro", "Live Store", "ShoppingBag", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.HERO_BANNER, "Flash Drop • Up to 40% Off", "AI-curated electronics & creator gear", "AutoAwesome", ColorRole.PRIMARY, ComponentAction.NAVIGATE, "checkout"),
                AppComponent(newId("c"), ComponentType.CHIP_GROUP, "All, Audio, Wearables, Smart Home", "", "Category", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.ROW_STATS, "Orders: 1,420", "Revenue: $64.2K", "Analytics", ColorRole.SUCCESS),
                AppComponent(newId("c"), ComponentType.LIST_TILE, "Studio ANC Headphones", "$249 • In Stock • 4.9★", "Headphones", ColorRole.SURFACE, ComponentAction.NAVIGATE, "checkout", "firestore://products/anc_01"),
                AppComponent(newId("c"), ComponentType.LIST_TILE, "Apex Watch Ultra", "$399 • Same-Day Dispatch", "Watch", ColorRole.SURFACE, ComponentAction.NAVIGATE, "checkout", "firestore://products/watch_02"),
                AppComponent(newId("c"), ComponentType.PRIMARY_BUTTON, "Open Express Checkout", "", "ShoppingCart", ColorRole.PRIMARY, ComponentAction.NAVIGATE, "checkout"),
                AppComponent(newId("c"), ComponentType.BOTTOM_NAV, "Storefront, Checkout, Analytics", "", "Menu", ColorRole.SURFACE)
            )
        )
        val checkout = AppScreen(
            screenId = "checkout",
            title = "Checkout",
            route = "checkout",
            iconName = "ShoppingCart",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Express Checkout", "Encrypted", "Lock", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.TEXT_INPUT, "Shipping Address", "House 14, Road 9, Dhaka", "LocationOn", ColorRole.SURFACE, dataBinding = "firestore://orders/address"),
                AppComponent(newId("c"), ComponentType.SWITCH_TOGGLE, "Priority Same-Day Courier", "true", "LocalShipping", ColorRole.SUCCESS),
                AppComponent(newId("c"), ComponentType.STAT_METRIC, "$268.50", "Total including express delivery", "Payments", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.PRIMARY_BUTTON, "Place Cloud Order", "", "CheckCircle", ColorRole.SUCCESS, ComponentAction.SUBMIT_FIRESTORE, "orders")
            )
        )
        val analytics = AppScreen(
            screenId = "analytics",
            title = "Analytics",
            route = "analytics",
            iconName = "Analytics",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Merchant Console", "asia-southeast1", "Analytics", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.DATA_CHART, "7-Day Conversion Volume", "38,52,61,74,69,92,98", "TrendingUp", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.PROGRESS_BAR, "Monthly Fulfillment Target", "84", "Speed", ColorRole.SUCCESS)
            )
        )
        val pkg = "com.bcf.omnicart"
        return AppProject(
            userId = userId,
            name = "E-Commerce Full-Stack App",
            packageName = pkg,
            tagline = "Multi-vendor mobile storefront with checkout, orders & merchant analytics",
            themePreset = AppThemePreset.OBSIDIAN_CYAN,
            architecture = ArchitecturePattern.CLEAN_MVVM,
            cloudRegion = "asia-southeast1",
            scalingTier = ScalingTier.EDGE_GLOBAL,
            deploymentStatus = DeploymentStatus.STAGING,
            activeVersion = "v1.4.2",
            screens = listOf(home, checkout, analytics),
            files = defaultFilesFor(pkg, "OmniCart", listOf(home, checkout, analytics)),
            backend = BackendSpec(
                collections = listOf(
                    FirestoreCollectionSpec("products", "title, priceCents, stock, category", "Public Read / Admin Write"),
                    FirestoreCollectionSpec("orders", "userId, items, totalCents, status, createdAt", "Owner Read & Create")
                ),
                endpoints = listOf(
                    CloudEndpointSpec("POST", "/api/v1/orders/checkout", "Node.js TypeScript • Express", "Validates inventory & creates payment intent"),
                    CloudEndpointSpec("GET", "/api/v1/analytics/summary", "Node.js TypeScript • Express", "Aggregates daily GMV and conversion metrics")
                ),
                authGoogle = true,
                authEmail = false,
                appCheckEnabled = true,
                firestoreRules = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /products/{productId} {
      allow read: if request.auth != null;
      allow write: if false;
    }
    match /orders/{orderId} {
      allow read, create: if request.auth != null && request.resource.data.userId == request.auth.uid;
    }
  }
}""",
                envVars = listOf(
                    EnvVarSpec("STRIPE_WEBHOOK_SECRET", "whsec_live_***", true),
                    EnvVarSpec("CLOUD_REGION", "asia-southeast1", true)
                )
            )
        )
    }

    private fun finLedgerTemplate(userId: String): AppProject {
        val home = AppScreen(
            screenId = "ledger",
            title = "Ledger",
            route = "ledger",
            iconName = "AccountBalanceWallet",
            components = listOf(
                AppComponent(newId("c"), ComponentType.TOP_APP_BAR, "Full-Stack FinLedger", "Live Sync", "AccountBalance", ColorRole.SECONDARY),
                AppComponent(newId("c"), ComponentType.STAT_METRIC, "$124,580.40", "Net Operating Treasury • +9.2%", "TrendingUp", ColorRole.PRIMARY),
                AppComponent(newId("c"), ComponentType.TEXT_INPUT, "Quick Expense Note", "Cloud GPU Cluster - $140", "Edit", ColorRole.SURFACE),
                AppComponent(newId("c"), ComponentType.PRIMARY_BUTTON, "Record Ledger Entry", "", "Add", ColorRole.PRIMARY, ComponentAction.SUBMIT_FIRESTORE, "transactions")
            )
        )
        val pkg = "com.bcf.finledger"
        return AppProject(
            userId = userId,
            name = "Full-Stack Cloud App",
            packageName = pkg,
            tagline = "Complete Android + Node.js/TS + PostgreSQL + Firestore full-stack starter",
            themePreset = AppThemePreset.INDIGO_PULSE,
            architecture = ArchitecturePattern.MVI_MODULAR,
            cloudRegion = "europe-west1",
            scalingTier = ScalingTier.SERVERLESS_AUTO,
            deploymentStatus = DeploymentStatus.DEPLOYED,
            activeVersion = "v2.1.0",
            screens = listOf(home),
            files = defaultFilesFor(pkg, "FinLedger", listOf(home)),
            backend = defaultBackendFor("FinLedger")
        )
    }

    fun defaultFilesFor(pkg: String, appName: String, screens: List<AppScreen>): List<ProjectFile> {
        val primaryScreen = screens.firstOrNull()
        val uiCode = if (primaryScreen != null) {
            generateComposeScreenCode(appName, primaryScreen)
        } else {
            "// Add components in the Visual Builder"
        }
        return listOf(
            ProjectFile(
                fileId = newId("file"),
                path = "app/src/main/java/${pkg.replace('.', '/')}/MainActivity.kt",
                language = "kotlin",
                layer = CodeLayer.UI,
                content = """package $pkg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}
"""
            ),
            ProjectFile(
                fileId = newId("file"),
                path = "app/src/main/java/${pkg.replace('.', '/')}/ui/${primaryScreen?.title?.replace(" ", "") ?: "Home"}Screen.kt",
                language = "kotlin",
                layer = CodeLayer.UI,
                content = uiCode
            ),
            ProjectFile(
                fileId = newId("file"),
                path = "app/src/main/java/${pkg.replace('.', '/')}/viewmodel/AppViewModel.kt",
                language = "kotlin",
                layer = CodeLayer.VIEWMODEL,
                content = """package $pkg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun syncCloudState() {
        viewModelScope.launch {
            _isLoading.value = true
            // Synchronize with Cloud Firestore repository
            _isLoading.value = false
        }
    }
}
"""
            ),
            ProjectFile(
                fileId = newId("file"),
                path = "app/src/main/java/${pkg.replace('.', '/')}/data/CloudRepository.kt",
                language = "kotlin",
                layer = CodeLayer.DATA,
                content = """package $pkg.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class CloudRepository(private val db: FirebaseFirestore) {
    suspend fun saveRecord(collection: String, uid: String, payload: Map<String, Any>) {
        db.collection("users").document(uid).collection(collection)
            .add(payload + mapOf("createdAt" to FieldValue.serverTimestamp()))
            .await()
    }
}
"""
            ),
            ProjectFile(
                fileId = newId("file"),
                path = "backend/functions/index.ts",
                language = "typescript",
                layer = CodeLayer.BACKEND,
                content = """import { onRequest } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";

initializeApp();

export const healthCheck = onRequest({ region: "asia-southeast1" }, (req, res) => {
  res.json({ status: "healthy", service: "$appName", timestamp: Date.now() });
});
"""
            ),
            ProjectFile(
                fileId = newId("file"),
                path = "firestore.rules",
                language = "rules",
                layer = CodeLayer.SECURITY,
                content = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{collection}/{docId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
"""
            )
        )
    }

    fun defaultBackendFor(appName: String): BackendSpec = BackendSpec(
        collections = listOf(
            FirestoreCollectionSpec("records", "userId, title, status, createdAt, updatedAt", "Owner Only (request.auth.uid)"),
            FirestoreCollectionSpec("analytics", "userId, eventType, value, createdAt", "Owner Read & Create")
        ),
        endpoints = listOf(
            CloudEndpointSpec("POST", "/api/v1/${appName.lowercase().replace(" ", "")}/sync", "Cloud Run • Auto-Scale", "Processes transactional cloud sync")
        ),
        authGoogle = true,
        authEmail = false,
        appCheckEnabled = true,
        firestoreRules = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{col}/{docId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}""",
        envVars = listOf(
            EnvVarSpec("APP_ENV", "production", true),
            EnvVarSpec("APP_CHECK_ENFORCED", "true", true)
        )
    )

    fun generateHardenedRules(collections: List<FirestoreCollectionSpec>): String {
        val matchBlocks = collections.ifEmpty {
            listOf(FirestoreCollectionSpec("records", "userId, title, createdAt", "Owner Only"))
        }.joinToString("\n\n") { col ->
            val safeName = col.name.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "items" }
            """    match /users/{userId}/$safeName/{docId} {
      // Policy: ${col.accessRule} | Fields: ${col.fieldsSummary}
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }"""
        }
        return """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;
    }

$matchBlocks
  }
}
"""
    }

    fun generateFullStackFiles(
        pkg: String,
        appName: String,
        architecture: ArchitecturePattern,
        cloudRegion: String,
        screens: List<AppScreen>,
        collections: List<FirestoreCollectionSpec>,
        endpoints: List<CloudEndpointSpec>,
        rules: String
    ): List<ProjectFile> {
        val pkgPath = pkg.replace('.', '/')
        val result = mutableListOf<ProjectFile>()

        val navRoutes = screens.joinToString("\n") { scr ->
            val safeClass = scr.title.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Main" }
            """            composable("${scr.route}") { ${safeClass}Screen(onNavigate = { route -> navController.navigate(route) }) }"""
        }

        result += ProjectFile(
            fileId = newId("file"),
            path = "app/src/main/java/$pkgPath/MainActivity.kt",
            language = "kotlin",
            layer = CodeLayer.UI,
            content = """package $pkg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import $pkg.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "${screens.firstOrNull()?.route ?: "home"}"
            ) {
$navRoutes
            }
        }
    }
}
"""
        )

        screens.forEach { scr ->
            val safeClass = scr.title.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Main" }
            result += ProjectFile(
                fileId = newId("file"),
                path = "app/src/main/java/$pkgPath/ui/${safeClass}Screen.kt",
                language = "kotlin",
                layer = CodeLayer.UI,
                content = generateComposeScreenCode(appName, scr)
            )
        }

        val collectionSyncMethods = collections.joinToString("\n\n") { col ->
            val fnName = col.name.replace(Regex("[^A-Za-z0-9]"), "")
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Items" }
            """    fun sync$fnName(uid: String, payload: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.saveToCollection("${col.name}", uid, payload)
            _isLoading.value = false
        }
    }"""
        }

        result += ProjectFile(
            fileId = newId("file"),
            path = "app/src/main/java/$pkgPath/viewmodel/AppViewModel.kt",
            language = "kotlin",
            layer = CodeLayer.VIEWMODEL,
            content = """package $pkg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import $pkg.data.CloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Architecture: ${architecture.displayName}
class AppViewModel(private val repository: CloudRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

$collectionSyncMethods
}
"""
        )

        result += ProjectFile(
            fileId = newId("file"),
            path = "app/src/main/java/$pkgPath/data/CloudRepository.kt",
            language = "kotlin",
            layer = CodeLayer.DATA,
            content = """package $pkg.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudRepository(private val db: FirebaseFirestore) {
    suspend fun saveToCollection(collection: String, uid: String, payload: Map<String, Any>) {
        db.collection("users")
            .document(uid)
            .collection(collection)
            .add(payload + mapOf("createdAt" to FieldValue.serverTimestamp()))
            .await()
    }
}
"""
        )

        val endpointExports = endpoints.ifEmpty {
            listOf(CloudEndpointSpec("GET", "/api/v1/health", "Cloud Functions v2", "Service health check"))
        }.joinToString("\n\n") { ep ->
            val fnName = ep.path.split("/").lastOrNull { it.isNotBlank() }
                ?.replace(Regex("[^A-Za-z0-9]"), "")
                ?.ifBlank { "handler" } ?: "handler"
            """export const $fnName = onRequest({ region: "$cloudRegion" }, async (req, res) => {
  // ${ep.method} ${ep.path} - ${ep.description}
  res.json({ ok: true, service: "$appName", endpoint: "${ep.path}", timestamp: Date.now() });
});"""
        }

        result += ProjectFile(
            fileId = newId("file"),
            path = "backend/functions/index.ts",
            language = "typescript",
            layer = CodeLayer.BACKEND,
            content = """import { onRequest } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";

initializeApp();

$endpointExports
"""
        )

        result += ProjectFile(
            fileId = newId("file"),
            path = "firestore.rules",
            language = "rules",
            layer = CodeLayer.SECURITY,
            content = rules
        )

        return result
    }
}

