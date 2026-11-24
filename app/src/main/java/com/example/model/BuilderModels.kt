package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppThemePreset(
    val displayName: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long
) {
    OBSIDIAN_CYAN("Neon Obsidian", 0xFF38BDF8, 0xFF8B5CF6, 0xFF10B981),
    EMERALD_CLOUD("Emerald Cloud", 0xFF10B981, 0xFF06B6D4, 0xFF34D399),
    INDIGO_PULSE("Violet Pulse", 0xFF8B5CF6, 0xFFEC4899, 0xFF38BDF8),
    SUNSET_CORAL("Sunset Coral", 0xFFF97316, 0xFFF43F5E, 0xFFFACC15),
    MONOCHROME_PRO("Monochrome Pro", 0xFFE2E8F0, 0xFF94A3B8, 0xFF38BDF8);

    companion object {
        fun safeValueOf(raw: String?): AppThemePreset =
            entries.firstOrNull { it.name == raw } ?: OBSIDIAN_CYAN
    }
}

enum class ArchitecturePattern(val displayName: String, val summary: String) {
    CLEAN_MVVM("Clean MVVM", "Domain + Repository + StateFlow ViewModel"),
    MVI_MODULAR("MVI Modular", "Unidirectional intents & immutable reducers"),
    FEATURE_FIRST("Feature-First", "Self-contained feature packages & Hilt DI");

    companion object {
        fun safeValueOf(raw: String?): ArchitecturePattern =
            entries.firstOrNull { it.name == raw } ?: CLEAN_MVVM
    }
}

enum class ScalingTier(val displayName: String, val spec: String) {
    SERVERLESS_AUTO("Serverless Auto", "0 -> 1,000 instances • Pay-per-use"),
    EDGE_GLOBAL("Edge Global CDN", "Multi-region edge cache • <35ms latency"),
    HIGH_MEMORY("High-Memory Cluster", "Dedicated 8GB Cloud Run • AI workloads");

    companion object {
        fun safeValueOf(raw: String?): ScalingTier =
            entries.firstOrNull { it.name == raw } ?: SERVERLESS_AUTO
    }
}

enum class DeploymentStatus(val label: String) {
    DRAFT("Draft"),
    STAGING("Staging"),
    DEPLOYED("Live");

    companion object {
        fun safeValueOf(raw: String?): DeploymentStatus =
            entries.firstOrNull { it.name == raw } ?: DRAFT
    }
}

enum class ComponentCategory(val label: String) {
    LAYOUT("Containers & Nav"),
    DISPLAY("Display & Media"),
    INPUT("Inputs & Controls"),
    DATA("Data & Advanced")
}

enum class ComponentType(
    val displayName: String,
    val category: ComponentCategory,
    val defaultText: String,
    val defaultSubtitle: String
) {
    // Containers & Navigation
    TOP_APP_BAR("Toolbar / AppBar", ComponentCategory.LAYOUT, "Dashboard", "Cloud Synced"),
    LINEAR_LAYOUT("Linear Layout", ComponentCategory.LAYOUT, "Linear Container", "Horizontal / Vertical flow"),
    VERTICAL_LAYOUT("Vertical Layout", ComponentCategory.LAYOUT, "Column Container", "Stacked child elements"),
    HORIZONTAL_LAYOUT("Horizontal Layout", ComponentCategory.LAYOUT, "Row Container", "Side-by-side items"),
    FRAME_LAYOUT("Frame Layout", ComponentCategory.LAYOUT, "Overlay Frame", "Layered Z-stack container"),
    CONSTRAINT_LAYOUT("Constraint Layout", ComponentCategory.LAYOUT, "Constraint Canvas", "Anchored responsive bounds"),
    SCROLL_VIEW("Scroll View", ComponentCategory.LAYOUT, "Scroll Container", "Vertical scrollable region"),
    TAB_LAYOUT("TabLayout", ComponentCategory.LAYOUT, "Overview, Feed, Settings", "0"),
    VIEW_PAGER("ViewPager2", ComponentCategory.LAYOUT, "Swipe Pages", "Page 1, Page 2, Page 3"),
    FLOATING_PANEL("Bottom Sheet", ComponentCategory.LAYOUT, "Action Sheet", "Tap to expand options"),
    HERO_BANNER("Hero Banner", ComponentCategory.LAYOUT, "Welcome Back", "Monitor metrics & real-time sync"),
    CARD_CONTAINER("Card Container", ComponentCategory.LAYOUT, "Active Workspace", "Tap to explore module details"),
    ROW_STATS("2-Column Metrics", ComponentCategory.LAYOUT, "Revenue: $24.8K", "Orders: 1,284 (+14%)"),
    BOTTOM_NAV("Bottom Navigation", ComponentCategory.LAYOUT, "Home, Analytics, Cloud, Profile", ""),
    NAV_DRAWER("Navigation Drawer", ComponentCategory.LAYOUT, "Dashboard, Orders, Settings, Help", "Slide-out menu"),
    DIVIDER("Section Divider", ComponentCategory.LAYOUT, "Overview", ""),
    SPACER("Vertical Spacer", ComponentCategory.LAYOUT, "16", ""),

    // Display & Media
    HEADLINE("Text / Headline", ComponentCategory.DISPLAY, "Real-Time Analytics", ""),
    BODY_TEXT("Body Paragraph", ComponentCategory.DISPLAY, "All services operational across multi-region clusters.", ""),
    BADGE_CHIP("Status Badge", ComponentCategory.DISPLAY, "Production Ready", "v2.4"),
    STAT_METRIC("KPI Stat Card", ComponentCategory.DISPLAY, "$48,920", "Monthly Recurring Revenue • +18.4%"),
    AVATAR_ROW("User Profile Row", ComponentCategory.DISPLAY, "Lead Architect", "Online • Verified"),
    IMAGE_BANNER("Image Component", ComponentCategory.DISPLAY, "Featured Showcase", "Adaptive visual container"),
    VIDEO_PLAYER("Video Component", ComponentCategory.DISPLAY, "Product Demo Stream", "1080p • Media3 ExoPlayer"),
    LOTTIE_ANIMATION("Lottie Animation", ComponentCategory.DISPLAY, "success_pulse.json", "Loop • 60 FPS Vector"),
    PROGRESS_BAR("Progress Bar", ComponentCategory.DISPLAY, "Cloud Quota Usage", "72"),

    // Inputs & Controls
    TEXT_INPUT("Text Field", ComponentCategory.INPUT, "Project Title", "Enter value..."),
    PASSWORD_FIELD("Password Field", ComponentCategory.INPUT, "Account Password", "••••••••"),
    PRIMARY_BUTTON("Primary Button", ComponentCategory.INPUT, "Deploy Changes", ""),
    OUTLINED_BUTTON("Secondary Button", ComponentCategory.INPUT, "Preview Schema", ""),
    IMAGE_BUTTON("Image Button", ComponentCategory.INPUT, "Scan QR", "Quick camera action"),
    FAB_BUTTON("Floating Action Button", ComponentCategory.INPUT, "New Record", ""),
    CHECKBOX("Checkbox", ComponentCategory.INPUT, "Accept Terms & Privacy", "true"),
    RADIO_BUTTON("Radio Group", ComponentCategory.INPUT, "Standard, Express, Priority", "Standard"),
    SWITCH_TOGGLE("Switch Toggle", ComponentCategory.INPUT, "Enable Real-time Sync", "true"),
    SLIDER_CONTROL("Slider Control", ComponentCategory.INPUT, "Max Auto-Scale Instances", "65"),
    RATING_BAR("RatingBar", ComponentCategory.INPUT, "Rate Experience", "4.5"),
    DATE_PICKER("Calendar / Date", ComponentCategory.INPUT, "Select Date", "2026-11-24"),
    SPINNER_DROPDOWN("Spinner Dropdown", ComponentCategory.INPUT, "Category", "Electronics, Fashion, Food, Books"),
    CHIP_GROUP("Filter Chips", ComponentCategory.INPUT, "All, Active, Pending, Archived", ""),

    // Data & Advanced
    LIST_TILE("List Item", ComponentCategory.DATA, "Order #BCF-9024", "Shipped • Express Courier"),
    RECYCLER_LIST("Recycler / List View", ComponentCategory.DATA, "Dynamic Data Feed", "Order #101, Order #102, Order #103"),
    DATA_CHART("Analytics Chart", ComponentCategory.DATA, "Weekly Throughput", "42,68,55,89,76,94,88"),
    WEB_VIEW("WebView", ComponentCategory.DATA, "https://app.bcfbuilder.dev/docs", "JS Enabled • Safe Browsing"),
    CUSTOM_COMPONENT("Custom Composable", ComponentCategory.DATA, "CustomWidget()", "Modular Jetpack Compose block");

    companion object {
        fun safeValueOf(raw: String?): ComponentType =
            entries.firstOrNull { it.name == raw } ?: CARD_CONTAINER
    }
}

enum class ComponentAction(val label: String) {
    NONE("No Action"),
    NAVIGATE("Navigate to Screen"),
    SUBMIT_FIRESTORE("Write to Firestore"),
    CALL_ENDPOINT("Call Cloud API"),
    TRIGGER_AI("Run AI Task"),
    SHOW_SNACKBAR("Show Toast / Alert");

    companion object {
        fun safeValueOf(raw: String?): ComponentAction =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class ColorRole(val label: String) {
    PRIMARY("Neon Blue"),
    SECONDARY("Electric Violet"),
    SURFACE("Obsidian Surface"),
    SUCCESS("Emerald"),
    WARNING("Amber"),
    ERROR("Coral Red");

    companion object {
        fun safeValueOf(raw: String?): ColorRole =
            entries.firstOrNull { it.name == raw } ?: PRIMARY
    }
}

enum class CodeLayer(val label: String) {
    UI("UI / Compose"),
    VIEWMODEL("ViewModel / Logic"),
    DATA("Data / Repo"),
    BACKEND("Node.js / Cloud"),
    SECURITY("Security Rules"),
    CONFIG("Gradle & Manifest");

    companion object {
        fun safeValueOf(raw: String?): CodeLayer =
            entries.firstOrNull { it.name == raw } ?: UI
    }
}

enum class BlockCategory(val label: String, val accentHex: Long) {
    EVENTS("Events", 0xFFF59E0B),
    LOGIC("Control", 0xFF38BDF8),
    VARIABLES("Variables", 0xFFEC4899),
    MATH("Math", 0xFF10B981),
    TEXT("Text", 0xFF14B8A6),
    UI("View / UI", 0xFF8B5CF6),
    COMPONENTS("Components", 0xFF0EA5E9),
    ANDROID("Android", 0xFF6366F1),
    INTERNET("Network", 0xFF06B6D4),
    FIREBASE("Firebase", 0xFFF97316),
    MORE_BLOCK("MoreBlock", 0xFFE11D48);

    companion object {
        fun safeValueOf(raw: String?): BlockCategory =
            entries.firstOrNull { it.name == raw } ?: EVENTS
    }
}

enum class BlockOpType(
    val category: BlockCategory,
    val title: String,
    val defaultArg1: String,
    val defaultArg2: String = ""
) {
    // Events
    ON_APP_START(BlockCategory.EVENTS, "When App Started", "initConfig()"),
    ON_SCREEN_CREATE(BlockCategory.EVENTS, "When Screen Created", "loadInitialData()"),
    ON_BUTTON_CLICK(BlockCategory.EVENTS, "When Button Clicked", "btnPrimary"),
    ON_TEXT_CHANGE(BlockCategory.EVENTS, "When Text Changed", "inputSearch"),
    ON_TIMER(BlockCategory.EVENTS, "Every Timer Tick (ms)", "5000"),
    ON_NETWORK_RESPONSE(BlockCategory.EVENTS, "When HTTP Response", "apiResult"),
    ON_FIREBASE_EVENT(BlockCategory.EVENTS, "When Firestore Snapshot", "records"),

    // Control & Logic
    IF_CONDITION(BlockCategory.LOGIC, "If Condition", "isValid == true"),
    ELSE_BRANCH(BlockCategory.LOGIC, "Else Branch", ""),
    REPEAT_LOOP(BlockCategory.LOGIC, "Repeat N Times", "3", "idx"),
    SWITCH_CASE(BlockCategory.LOGIC, "Switch / When", "status", "ACTIVE -> showHome()"),
    LOGIC_AND_OR(BlockCategory.LOGIC, "Compare (AND / OR / NOT)", "a > 0 && b != null"),
    TRY_CATCH(BlockCategory.LOGIC, "Try / Catch Guard", "apiCall()", "Show Error Toast"),

    // Variables & Lists
    CREATE_VAR(BlockCategory.VARIABLES, "Create State Variable", "counter", "0"),
    SET_VAR(BlockCategory.VARIABLES, "Set Variable", "counter", "counter + 1"),
    LIST_OP(BlockCategory.VARIABLES, "List / Map Operation", "items", "add(newItem)"),
    LIST_FILTER(BlockCategory.VARIABLES, "Filter / Sort List", "items", "sorted()"),

    // Math
    MATH_CALC(BlockCategory.MATH, "Calculate Expression", "total", "price * qty"),
    MATH_RANDOM(BlockCategory.MATH, "Random Number", "otpCode", "1000..9999"),

    // Text
    TEXT_JOIN(BlockCategory.TEXT, "Join / Format Text", "greeting", "\"Hello, \$userName\""),
    TEXT_SPLIT(BlockCategory.TEXT, "Split / Replace Text", "rawInput", "trim().split(\",\")"),

    // View / UI
    UI_TOAST(BlockCategory.UI, "Show Toast / Snackbar", "Saved successfully!"),
    UI_SET_TEXT(BlockCategory.UI, "Set Widget Text", "titleView", "\"Updated\""),
    UI_SET_COLOR_VISIBILITY(BlockCategory.UI, "Set Visibility / Color", "statusCard", "visible = true"),
    UI_NAVIGATE(BlockCategory.UI, "Open Screen (Intent)", "dashboard"),
    UI_CLOSE_SCREEN(BlockCategory.UI, "Finish / Back Screen", "popBackStack()"),

    // Sketchware Components
    SHARED_PREFS_SAVE(BlockCategory.COMPONENTS, "SharedPrefs Save Key", "user_token", "active"),
    SHARED_PREFS_GET(BlockCategory.COMPONENTS, "SharedPrefs Load Key", "user_token", "guest"),
    DIALOG_SHOW(BlockCategory.COMPONENTS, "Show AlertDialog", "Confirm Action", "Are you sure you want to proceed?"),
    TIMER_SCHEDULE(BlockCategory.COMPONENTS, "Timer Delay (ms)", "1500", "refreshData"),
    TTS_SPEAK(BlockCategory.COMPONENTS, "Speak Text (TTS)", "Welcome to BCF Builder"),

    // Android Hardware
    ANDROID_SHARE(BlockCategory.ANDROID, "Share / Open URL Intent", "https://bcfbuilder.dev"),
    ANDROID_PERMISSION(BlockCategory.ANDROID, "Request Runtime Permission", "CAMERA"),
    ANDROID_NOTIFICATION(BlockCategory.ANDROID, "Show Local Notification", "Order Update", "Your order shipped"),
    ANDROID_VIBRATE_CLIPBOARD(BlockCategory.ANDROID, "Vibrate / Copy Clipboard", "copiedText"),
    ANDROID_PICK_MEDIA(BlockCategory.ANDROID, "Open Photo Picker / Camera", "PickVisualMedia"),

    // Network
    HTTP_GET(BlockCategory.INTERNET, "HTTP GET Request", "https://api.example.com/items"),
    HTTP_POST(BlockCategory.INTERNET, "HTTP POST JSON", "https://api.example.com/orders", "{\"qty\":1}"),
    JSON_PARSE(BlockCategory.INTERNET, "Parse JSON Response", "responseBody", "data.items"),

    // Firebase
    FB_AUTH_CHECK(BlockCategory.FIREBASE, "Firebase Auth Session", "requireCurrentUser()"),
    FB_FIRESTORE_WRITE(BlockCategory.FIREBASE, "Firestore Add Document", "orders", "payload"),
    FB_STORAGE_UPLOAD(BlockCategory.FIREBASE, "Firebase Storage Upload", "uploads/avatar.jpg"),
    FB_FCM_TOPIC(BlockCategory.FIREBASE, "Subscribe FCM Topic", "promotions"),

    // Custom MoreBlock
    CALL_MORE_BLOCK(BlockCategory.MORE_BLOCK, "Call Custom MoreBlock", "validateForm", "input");

    companion object {
        fun safeValueOf(raw: String?): BlockOpType =
            entries.firstOrNull { it.name == raw } ?: ON_SCREEN_CREATE
    }
}

@Keep
data class LogicBlock(
    val blockId: String = "",
    val opType: BlockOpType = BlockOpType.ON_SCREEN_CREATE,
    val arg1: String = "",
    val arg2: String = "",
    val enabled: Boolean = true,
    val targetWidgetId: String = "",
    val eventTrigger: String = "onCreate"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "blockId" to blockId,
        "opType" to opType.name,
        "arg1" to arg1,
        "arg2" to arg2,
        "enabled" to enabled,
        "targetWidgetId" to targetWidgetId,
        "eventTrigger" to eventTrigger
    )

    companion object {
        fun fromMap(map: Map<*, *>): LogicBlock = LogicBlock(
            blockId = (map["blockId"] as? String).orEmpty(),
            opType = BlockOpType.safeValueOf(map["opType"] as? String),
            arg1 = (map["arg1"] as? String).orEmpty(),
            arg2 = (map["arg2"] as? String).orEmpty(),
            enabled = (map["enabled"] as? Boolean) ?: true,
            targetWidgetId = (map["targetWidgetId"] as? String).orEmpty(),
            eventTrigger = (map["eventTrigger"] as? String).orEmpty().ifBlank { "onCreate" }
        )
    }
}

enum class SketchComponentType(
    val displayName: String,
    val androidClass: String,
    val events: List<String>,
    val accentHex: Long
) {
    INTENT("Intent", "android.content.Intent", listOf("onLaunch"), 0xFF38BDF8),
    SHARED_PREFS("SharedPreferences", "android.content.SharedPreferences", listOf("onSaved", "onLoaded"), 0xFF10B981),
    TIMER("Timer / Handler", "java.util.Timer", listOf("onTimerTick"), 0xFFF59E0B),
    DIALOG("AlertDialog", "androidx.appcompat.app.AlertDialog", listOf("onPositiveClick", "onNegativeClick"), 0xFF8B5CF6),
    FIREBASE_AUTH("FirebaseAuth", "com.google.firebase.auth.FirebaseAuth", listOf("onAuthSuccess", "onAuthError"), 0xFFF97316),
    FIREBASE_DB("FirebaseDB", "com.google.firebase.firestore.FirebaseFirestore", listOf("onChildAdded", "onSnapshotError"), 0xFFF97316),
    REQUEST_NETWORK("RequestNetwork", "okhttp3.OkHttpClient", listOf("onResponse", "onErrorResponse"), 0xFF06B6D4),
    CAMERA_PICKER("MediaPicker / Camera", "androidx.activity.result.PickVisualMedia", listOf("onMediaPicked"), 0xFFEC4899),
    TEXT_TO_SPEECH("TextToSpeech", "android.speech.tts.TextToSpeech", listOf("onTtsReady", "onSpeakDone"), 0xFF6366F1),
    VIBRATOR("Vibrator", "android.os.Vibrator", listOf("onVibrate"), 0xFF14B8A6);

    companion object {
        fun safeValueOf(raw: String?): SketchComponentType =
            entries.firstOrNull { it.name == raw } ?: INTENT
    }
}

@Keep
data class SketchComponent(
    val componentId: String = "",
    val name: String = "spData",
    val type: SketchComponentType = SketchComponentType.SHARED_PREFS,
    val configNote: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "componentId" to componentId,
        "name" to name,
        "type" to type.name,
        "configNote" to configNote
    )

    companion object {
        fun fromMap(map: Map<*, *>): SketchComponent = SketchComponent(
            componentId = (map["componentId"] as? String).orEmpty(),
            name = (map["name"] as? String).orEmpty().ifBlank { "component" },
            type = SketchComponentType.safeValueOf(map["type"] as? String),
            configNote = (map["configNote"] as? String).orEmpty()
        )
    }
}

@Keep
data class CustomMoreBlock(
    val blockId: String = "",
    val name: String = "validateForm",
    val paramsSignature: String = "input: String",
    val description: String = "Reusable custom logic function"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "blockId" to blockId,
        "name" to name,
        "paramsSignature" to paramsSignature,
        "description" to description
    )

    companion object {
        fun fromMap(map: Map<*, *>): CustomMoreBlock = CustomMoreBlock(
            blockId = (map["blockId"] as? String).orEmpty(),
            name = (map["name"] as? String).orEmpty().ifBlank { "customAction" },
            paramsSignature = (map["paramsSignature"] as? String).orEmpty(),
            description = (map["description"] as? String).orEmpty()
        )
    }
}

enum class DatabaseEngine(val displayName: String) {
    FIRESTORE("Firebase Firestore"),
    REALTIME_DB("Firebase Realtime DB"),
    POSTGRESQL("PostgreSQL"),
    SQLITE("SQLite (Room)"),
    MYSQL("MySQL"),
    MONGODB("MongoDB Adapter");

    companion object {
        fun safeValueOf(raw: String?): DatabaseEngine =
            entries.firstOrNull { it.name == raw } ?: FIRESTORE
    }
}

enum class AssetCategory(val label: String) {
    IMAGE("Images"),
    ICON("Icons / SVG"),
    FONT("Fonts"),
    LOTTIE("Lottie"),
    AUDIO("Audio"),
    VIDEO("Video");

    companion object {
        fun safeValueOf(raw: String?): AssetCategory =
            entries.firstOrNull { it.name == raw } ?: IMAGE
    }
}

@Keep
data class ProjectAsset(
    val assetId: String = "",
    val name: String = "",
    val category: AssetCategory = AssetCategory.IMAGE,
    val uriOrPath: String = "",
    val mimeType: String = "image/png"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "assetId" to assetId,
        "name" to name,
        "category" to category.name,
        "uriOrPath" to uriOrPath,
        "mimeType" to mimeType
    )

    companion object {
        fun fromMap(map: Map<*, *>): ProjectAsset = ProjectAsset(
            assetId = (map["assetId"] as? String).orEmpty(),
            name = (map["name"] as? String).orEmpty(),
            category = AssetCategory.safeValueOf(map["category"] as? String),
            uriOrPath = (map["uriOrPath"] as? String).orEmpty(),
            mimeType = (map["mimeType"] as? String).orEmpty().ifBlank { "image/png" }
        )
    }
}

@Keep
data class ApiRequestSpec(
    val requestId: String = "",
    val name: String = "Get Items",
    val method: String = "GET",
    val url: String = "https://httpbin.org/json",
    val headersText: String = "Accept: application/json",
    val queryParamsText: String = "",
    val bodyJson: String = "",
    val authType: String = "NONE", // NONE, BEARER, API_KEY
    val authValue: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "requestId" to requestId,
        "name" to name,
        "method" to method,
        "url" to url,
        "headersText" to headersText,
        "queryParamsText" to queryParamsText,
        "bodyJson" to bodyJson,
        "authType" to authType,
        "authValue" to authValue
    )

    companion object {
        fun fromMap(map: Map<*, *>): ApiRequestSpec = ApiRequestSpec(
            requestId = (map["requestId"] as? String).orEmpty(),
            name = (map["name"] as? String).orEmpty().ifBlank { "API Request" },
            method = (map["method"] as? String).orEmpty().ifBlank { "GET" },
            url = (map["url"] as? String).orEmpty(),
            headersText = (map["headersText"] as? String).orEmpty(),
            queryParamsText = (map["queryParamsText"] as? String).orEmpty(),
            bodyJson = (map["bodyJson"] as? String).orEmpty(),
            authType = (map["authType"] as? String).orEmpty().ifBlank { "NONE" },
            authValue = (map["authValue"] as? String).orEmpty()
        )
    }
}

@Keep
data class DatabaseTableSpec(
    val tableId: String = "",
    val engine: DatabaseEngine = DatabaseEngine.POSTGRESQL,
    val name: String = "users",
    val columnsSummary: String = "id UUID PK, email TEXT UNIQUE, role TEXT, created_at TIMESTAMPTZ",
    val relationsSummary: String = "1:N orders.user_id -> users.id",
    val indexSummary: String = "idx_users_email"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "tableId" to tableId,
        "engine" to engine.name,
        "name" to name,
        "columnsSummary" to columnsSummary,
        "relationsSummary" to relationsSummary,
        "indexSummary" to indexSummary
    )

    companion object {
        fun fromMap(map: Map<*, *>): DatabaseTableSpec = DatabaseTableSpec(
            tableId = (map["tableId"] as? String).orEmpty(),
            engine = DatabaseEngine.safeValueOf(map["engine"] as? String),
            name = (map["name"] as? String).orEmpty(),
            columnsSummary = (map["columnsSummary"] as? String).orEmpty(),
            relationsSummary = (map["relationsSummary"] as? String).orEmpty(),
            indexSummary = (map["indexSummary"] as? String).orEmpty()
        )
    }
}

@Keep
data class FirebaseSetupConfig(
    val firebaseProjectId: String = "",
    val androidAppId: String = "",
    val webApiKey: String = "",
    val storageBucket: String = "",
    val useAuthEmail: Boolean = true,
    val useAuthGoogle: Boolean = true,
    val useFirestore: Boolean = true,
    val useRealtimeDb: Boolean = false,
    val useStorage: Boolean = false,
    val useMessaging: Boolean = false,
    val useAnalytics: Boolean = true,
    val useCrashlytics: Boolean = false,
    val useAppCheck: Boolean = true,
    val useFunctions: Boolean = true,
    val validated: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "firebaseProjectId" to firebaseProjectId,
        "androidAppId" to androidAppId,
        "webApiKey" to webApiKey,
        "storageBucket" to storageBucket,
        "useAuthEmail" to useAuthEmail,
        "useAuthGoogle" to useAuthGoogle,
        "useFirestore" to useFirestore,
        "useRealtimeDb" to useRealtimeDb,
        "useStorage" to useStorage,
        "useMessaging" to useMessaging,
        "useAnalytics" to useAnalytics,
        "useCrashlytics" to useCrashlytics,
        "useAppCheck" to useAppCheck,
        "useFunctions" to useFunctions,
        "validated" to validated
    )

    companion object {
        fun fromMap(map: Map<*, *>?): FirebaseSetupConfig {
            if (map == null) return FirebaseSetupConfig()
            return FirebaseSetupConfig(
                firebaseProjectId = (map["firebaseProjectId"] as? String).orEmpty(),
                androidAppId = (map["androidAppId"] as? String).orEmpty(),
                webApiKey = (map["webApiKey"] as? String).orEmpty(),
                storageBucket = (map["storageBucket"] as? String).orEmpty(),
                useAuthEmail = (map["useAuthEmail"] as? Boolean) ?: true,
                useAuthGoogle = (map["useAuthGoogle"] as? Boolean) ?: true,
                useFirestore = (map["useFirestore"] as? Boolean) ?: true,
                useRealtimeDb = (map["useRealtimeDb"] as? Boolean) ?: false,
                useStorage = (map["useStorage"] as? Boolean) ?: false,
                useMessaging = (map["useMessaging"] as? Boolean) ?: false,
                useAnalytics = (map["useAnalytics"] as? Boolean) ?: true,
                useCrashlytics = (map["useCrashlytics"] as? Boolean) ?: false,
                useAppCheck = (map["useAppCheck"] as? Boolean) ?: true,
                useFunctions = (map["useFunctions"] as? Boolean) ?: true,
                validated = (map["validated"] as? Boolean) ?: false
            )
        }
    }
}

@Keep
data class AndroidBuildConfig(
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val permissions: List<String> = listOf("android.permission.INTERNET"),
    val dependencies: List<String> = listOf(
        "androidx.compose.material3:material3",
        "androidx.navigation:navigation-compose",
        "androidx.room:room-ktx"
    )
) {
    fun toMap(): Map<String, Any> = mapOf(
        "minSdk" to minSdk,
        "targetSdk" to targetSdk,
        "versionCode" to versionCode,
        "versionName" to versionName,
        "permissions" to permissions,
        "dependencies" to dependencies
    )

    companion object {
        fun fromMap(map: Map<*, *>?): AndroidBuildConfig {
            if (map == null) return AndroidBuildConfig()
            val perms = (map["permissions"] as? List<*>)?.mapNotNull { it as? String }
                ?: listOf("android.permission.INTERNET")
            val deps = (map["dependencies"] as? List<*>)?.mapNotNull { it as? String }
                ?: listOf("androidx.compose.material3:material3", "androidx.navigation:navigation-compose")
            return AndroidBuildConfig(
                minSdk = (map["minSdk"] as? Number)?.toInt() ?: 24,
                targetSdk = (map["targetSdk"] as? Number)?.toInt() ?: 36,
                versionCode = (map["versionCode"] as? Number)?.toInt() ?: 1,
                versionName = (map["versionName"] as? String).orEmpty().ifBlank { "1.0.0" },
                permissions = perms,
                dependencies = deps
            )
        }
    }
}

@Keep
data class AppComponent(
    val componentId: String = "",
    val type: ComponentType = ComponentType.CARD_CONTAINER,
    val text: String = "",
    val subtitle: String = "",
    val iconName: String = "Widgets",
    val colorRole: ColorRole = ColorRole.PRIMARY,
    val action: ComponentAction = ComponentAction.NONE,
    val actionTarget: String = "",
    val dataBinding: String = "",
    val paddingDp: Int = 12,
    val cornerRadiusDp: Int = 16,
    val marginDp: Int = 0,
    val elevationDp: Int = 2,
    val widthMode: String = "MATCH_PARENT", // MATCH_PARENT, WRAP_CONTENT
    val gravity: String = "START" // START, CENTER, END
) {
    fun toMap(): Map<String, Any> = mapOf(
        "componentId" to componentId,
        "type" to type.name,
        "text" to text,
        "subtitle" to subtitle,
        "iconName" to iconName,
        "colorRole" to colorRole.name,
        "action" to action.name,
        "actionTarget" to actionTarget,
        "dataBinding" to dataBinding,
        "paddingDp" to paddingDp,
        "cornerRadiusDp" to cornerRadiusDp,
        "marginDp" to marginDp,
        "elevationDp" to elevationDp,
        "widthMode" to widthMode,
        "gravity" to gravity
    )

    companion object {
        fun fromMap(map: Map<*, *>): AppComponent = AppComponent(
            componentId = (map["componentId"] as? String).orEmpty(),
            type = ComponentType.safeValueOf(map["type"] as? String),
            text = (map["text"] as? String).orEmpty(),
            subtitle = (map["subtitle"] as? String).orEmpty(),
            iconName = (map["iconName"] as? String).orEmpty().ifBlank { "Widgets" },
            colorRole = ColorRole.safeValueOf(map["colorRole"] as? String),
            action = ComponentAction.safeValueOf(map["action"] as? String),
            actionTarget = (map["actionTarget"] as? String).orEmpty(),
            dataBinding = (map["dataBinding"] as? String).orEmpty(),
            paddingDp = (map["paddingDp"] as? Number)?.toInt() ?: 12,
            cornerRadiusDp = (map["cornerRadiusDp"] as? Number)?.toInt() ?: 16,
            marginDp = (map["marginDp"] as? Number)?.toInt() ?: 0,
            elevationDp = (map["elevationDp"] as? Number)?.toInt() ?: 2,
            widthMode = (map["widthMode"] as? String).orEmpty().ifBlank { "MATCH_PARENT" },
            gravity = (map["gravity"] as? String).orEmpty().ifBlank { "START" }
        )
    }
}

@Keep
data class AppScreen(
    val screenId: String = "",
    val title: String = "Home",
    val route: String = "home",
    val iconName: String = "Home",
    val components: List<AppComponent> = emptyList(),
    val logicBlocks: List<LogicBlock> = emptyList(),
    val navArguments: String = "",
    val deepLinkUri: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "screenId" to screenId,
        "title" to title,
        "route" to route,
        "iconName" to iconName,
        "components" to components.map { it.toMap() },
        "logicBlocks" to logicBlocks.map { it.toMap() },
        "navArguments" to navArguments,
        "deepLinkUri" to deepLinkUri
    )

    companion object {
        fun fromMap(map: Map<*, *>): AppScreen {
            val rawComponents = (map["components"] as? List<*>) ?: emptyList<Any>()
            val rawBlocks = (map["logicBlocks"] as? List<*>) ?: emptyList<Any>()
            return AppScreen(
                screenId = (map["screenId"] as? String).orEmpty(),
                title = (map["title"] as? String).orEmpty().ifBlank { "Screen" },
                route = (map["route"] as? String).orEmpty().ifBlank { "screen" },
                iconName = (map["iconName"] as? String).orEmpty().ifBlank { "Dashboard" },
                components = rawComponents.mapNotNull { (it as? Map<*, *>)?.let(AppComponent::fromMap) },
                logicBlocks = rawBlocks.mapNotNull { (it as? Map<*, *>)?.let(LogicBlock::fromMap) },
                navArguments = (map["navArguments"] as? String).orEmpty(),
                deepLinkUri = (map["deepLinkUri"] as? String).orEmpty()
            )
        }
    }
}

@Keep
data class ProjectFile(
    val fileId: String = "",
    val path: String = "",
    val language: String = "kotlin",
    val layer: CodeLayer = CodeLayer.UI,
    val content: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "fileId" to fileId,
        "path" to path,
        "language" to language,
        "layer" to layer.name,
        "content" to content
    )

    companion object {
        fun fromMap(map: Map<*, *>): ProjectFile = ProjectFile(
            fileId = (map["fileId"] as? String).orEmpty(),
            path = (map["path"] as? String).orEmpty(),
            language = (map["language"] as? String).orEmpty().ifBlank { "kotlin" },
            layer = CodeLayer.safeValueOf(map["layer"] as? String),
            content = (map["content"] as? String).orEmpty()
        )
    }
}

@Keep
data class FirestoreCollectionSpec(
    val name: String = "",
    val fieldsSummary: String = "",
    val accessRule: String = "Owner Only (request.auth.uid)"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "fieldsSummary" to fieldsSummary,
        "accessRule" to accessRule
    )

    companion object {
        fun fromMap(map: Map<*, *>): FirestoreCollectionSpec = FirestoreCollectionSpec(
            name = (map["name"] as? String).orEmpty(),
            fieldsSummary = (map["fieldsSummary"] as? String).orEmpty(),
            accessRule = (map["accessRule"] as? String).orEmpty().ifBlank { "Owner Only" }
        )
    }
}

@Keep
data class CloudEndpointSpec(
    val method: String = "POST",
    val path: String = "/api/v1/items",
    val runtime: String = "Node.js TypeScript • Express",
    val description: String = "",
    val authRequired: Boolean = true,
    val rateLimitRpm: Int = 60
) {
    fun toMap(): Map<String, Any> = mapOf(
        "method" to method,
        "path" to path,
        "runtime" to runtime,
        "description" to description,
        "authRequired" to authRequired,
        "rateLimitRpm" to rateLimitRpm
    )

    companion object {
        fun fromMap(map: Map<*, *>): CloudEndpointSpec = CloudEndpointSpec(
            method = (map["method"] as? String).orEmpty().ifBlank { "POST" },
            path = (map["path"] as? String).orEmpty().ifBlank { "/api/v1/action" },
            runtime = (map["runtime"] as? String).orEmpty().ifBlank { "Node.js TypeScript" },
            description = (map["description"] as? String).orEmpty(),
            authRequired = (map["authRequired"] as? Boolean) ?: true,
            rateLimitRpm = (map["rateLimitRpm"] as? Number)?.toInt() ?: 60
        )
    }
}

@Keep
data class EnvVarSpec(
    val key: String = "",
    val valueHint: String = "",
    val required: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "key" to key,
        "valueHint" to valueHint,
        "required" to required
    )

    companion object {
        fun fromMap(map: Map<*, *>): EnvVarSpec = EnvVarSpec(
            key = (map["key"] as? String).orEmpty(),
            valueHint = (map["valueHint"] as? String).orEmpty(),
            required = (map["required"] as? Boolean) ?: true
        )
    }
}

@Keep
data class BackendSpec(
    val collections: List<FirestoreCollectionSpec> = emptyList(),
    val endpoints: List<CloudEndpointSpec> = emptyList(),
    val authGoogle: Boolean = true,
    val authEmail: Boolean = false,
    val appCheckEnabled: Boolean = true,
    val firestoreRules: String = "",
    val envVars: List<EnvVarSpec> = emptyList(),
    val jwtEnabled: Boolean = true,
    val rateLimitEnabled: Boolean = true,
    val databaseTables: List<DatabaseTableSpec> = emptyList(),
    val apiRequests: List<ApiRequestSpec> = emptyList(),
    val assets: List<ProjectAsset> = emptyList(),
    val firebaseConfig: FirebaseSetupConfig = FirebaseSetupConfig(),
    val androidConfig: AndroidBuildConfig = AndroidBuildConfig(),
    val sketchComponents: List<SketchComponent> = emptyList(),
    val customMoreBlocks: List<CustomMoreBlock> = emptyList(),
    val stringResources: Map<String, String> = mapOf(
        "welcome_title" to "Welcome",
        "action_continue" to "Continue"
    ),
    val colorResources: Map<String, String> = mapOf(
        "colorPrimary" to "#38BDF8",
        "colorSecondary" to "#8B5CF6",
        "colorBackground" to "#0A0F1A"
    )
) {
    fun toMap(): Map<String, Any> = mapOf(
        "collections" to collections.map { it.toMap() },
        "endpoints" to endpoints.map { it.toMap() },
        "authGoogle" to authGoogle,
        "authEmail" to authEmail,
        "appCheckEnabled" to appCheckEnabled,
        "firestoreRules" to firestoreRules,
        "envVars" to envVars.map { it.toMap() },
        "jwtEnabled" to jwtEnabled,
        "rateLimitEnabled" to rateLimitEnabled,
        "databaseTables" to databaseTables.map { it.toMap() },
        "apiRequests" to apiRequests.map { it.toMap() },
        "assets" to assets.map { it.toMap() },
        "firebaseConfig" to firebaseConfig.toMap(),
        "androidConfig" to androidConfig.toMap(),
        "sketchComponents" to sketchComponents.map { it.toMap() },
        "customMoreBlocks" to customMoreBlocks.map { it.toMap() },
        "stringResources" to stringResources,
        "colorResources" to colorResources
    )

    companion object {
        fun fromMap(map: Map<*, *>?): BackendSpec {
            if (map == null) return BackendSpec()
            val cols = (map["collections"] as? List<*>) ?: emptyList<Any>()
            val eps = (map["endpoints"] as? List<*>) ?: emptyList<Any>()
            val envs = (map["envVars"] as? List<*>) ?: emptyList<Any>()
            val tables = (map["databaseTables"] as? List<*>) ?: emptyList<Any>()
            val apis = (map["apiRequests"] as? List<*>) ?: emptyList<Any>()
            val assetList = (map["assets"] as? List<*>) ?: emptyList<Any>()
            val sketchList = (map["sketchComponents"] as? List<*>) ?: emptyList<Any>()
            val moreList = (map["customMoreBlocks"] as? List<*>) ?: emptyList<Any>()
            val rawStrings = (map["stringResources"] as? Map<*, *>)
                ?.entries?.associate { (k, v) -> k.toString() to (v?.toString().orEmpty()) }
                ?: mapOf("welcome_title" to "Welcome", "action_continue" to "Continue")
            val rawColors = (map["colorResources"] as? Map<*, *>)
                ?.entries?.associate { (k, v) -> k.toString() to (v?.toString().orEmpty()) }
                ?: mapOf("colorPrimary" to "#38BDF8", "colorSecondary" to "#8B5CF6", "colorBackground" to "#0A0F1A")
            return BackendSpec(
                collections = cols.mapNotNull { (it as? Map<*, *>)?.let(FirestoreCollectionSpec::fromMap) },
                endpoints = eps.mapNotNull { (it as? Map<*, *>)?.let(CloudEndpointSpec::fromMap) },
                authGoogle = (map["authGoogle"] as? Boolean) ?: true,
                authEmail = (map["authEmail"] as? Boolean) ?: false,
                appCheckEnabled = (map["appCheckEnabled"] as? Boolean) ?: true,
                firestoreRules = (map["firestoreRules"] as? String).orEmpty(),
                envVars = envs.mapNotNull { (it as? Map<*, *>)?.let(EnvVarSpec::fromMap) },
                jwtEnabled = (map["jwtEnabled"] as? Boolean) ?: true,
                rateLimitEnabled = (map["rateLimitEnabled"] as? Boolean) ?: true,
                databaseTables = tables.mapNotNull { (it as? Map<*, *>)?.let(DatabaseTableSpec::fromMap) },
                apiRequests = apis.mapNotNull { (it as? Map<*, *>)?.let(ApiRequestSpec::fromMap) },
                assets = assetList.mapNotNull { (it as? Map<*, *>)?.let(ProjectAsset::fromMap) },
                firebaseConfig = FirebaseSetupConfig.fromMap(map["firebaseConfig"] as? Map<*, *>),
                androidConfig = AndroidBuildConfig.fromMap(map["androidConfig"] as? Map<*, *>),
                sketchComponents = sketchList.mapNotNull { (it as? Map<*, *>)?.let(SketchComponent::fromMap) },
                customMoreBlocks = moreList.mapNotNull { (it as? Map<*, *>)?.let(CustomMoreBlock::fromMap) },
                stringResources = rawStrings,
                colorResources = rawColors
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AppProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val packageName: String = "",
    val tagline: String = "",
    val themePreset: AppThemePreset = AppThemePreset.OBSIDIAN_CYAN,
    val architecture: ArchitecturePattern = ArchitecturePattern.CLEAN_MVVM,
    val cloudRegion: String = "asia-southeast1",
    val scalingTier: ScalingTier = ScalingTier.SERVERLESS_AUTO,
    val deploymentStatus: DeploymentStatus = DeploymentStatus.DRAFT,
    val activeVersion: String = "v1.0.0",
    val screens: List<AppScreen> = emptyList(),
    val files: List<ProjectFile> = emptyList(),
    val backend: BackendSpec = BackendSpec(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name.take(80).ifBlank { "Untitled App" },
        "packageName" to packageName.take(120).ifBlank { "com.bcf.app" },
        "tagline" to tagline.take(180),
        "themePreset" to themePreset.name,
        "architecture" to architecture.name,
        "cloudRegion" to cloudRegion.take(40).ifBlank { "asia-southeast1" },
        "scalingTier" to scalingTier.name,
        "deploymentStatus" to deploymentStatus.name,
        "activeVersion" to activeVersion.take(24).ifBlank { "v1.0.0" },
        "screens" to screens.take(25).map { it.toMap() },
        "files" to files.take(60).map { it.toMap() },
        "backend" to backend.toMap(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toDesignUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.take(80).ifBlank { "Untitled App" },
        "packageName" to packageName.take(120).ifBlank { "com.bcf.app" },
        "tagline" to tagline.take(180),
        "themePreset" to themePreset.name,
        "architecture" to architecture.name,
        "screens" to screens.take(25).map { it.toMap() },
        "files" to files.take(60).map { it.toMap() },
        "backend" to backend.toMap(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toCloudUpdateMap(): Map<String, Any> = mapOf(
        "cloudRegion" to cloudRegion.take(40).ifBlank { "asia-southeast1" },
        "scalingTier" to scalingTier.name,
        "deploymentStatus" to deploymentStatus.name,
        "activeVersion" to activeVersion.take(24).ifBlank { "v1.0.0" },
        "backend" to backend.toMap(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DeploymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val versionTag: String = "v1.0.0",
    val environment: String = "PRODUCTION",
    val cloudRegion: String = "asia-southeast1",
    val status: String = "SUCCEEDED",
    val endpointUrl: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class AiGenerationMode(val label: String, val description: String, val targetsExistingProject: Boolean = false) {
    QUICK_GENERATE("Quick Generate", "Fast multi-screen MVP + Compose UI"),
    FULL_PROJECT("Full Project Generate", "Complete modular Android Studio project"),
    PLAN_FIRST("Plan First", "Architecture plan + module breakdown before build"),
    UI_ONLY("UI Only", "Screens, layouts, theme & Compose components"),
    BACKEND_ONLY("Backend Only", "Node.js TypeScript REST API & DB schema", targetsExistingProject = true),
    FIREBASE_PROJECT("Firebase Project", "Auth, Firestore, Storage, FCM & Rules"),
    FULL_STACK("Full Stack Project", "Android + Backend + DB + Security + Tests"),
    ADD_FEATURE("Add Feature", "Targeted incremental edit preserving existing files", targetsExistingProject = true),
    FIX_PROJECT("Fix Existing Project", "Diagnose & patch missing imports or errors", targetsExistingProject = true),
    UPGRADE_PROJECT("Upgrade Project", "Enhance architecture, Compose M3 & scalability", targetsExistingProject = true),
    REFACTOR_PROJECT("Refactor Project", "Clean modularization & ViewModel cleanup", targetsExistingProject = true),
    EXPLAIN_CODE("Explain Code", "Detailed architectural walkthrough", targetsExistingProject = true),
    GENERATE_TESTS("Generate Tests", "Unit & Robolectric test suite for active project", targetsExistingProject = true)
}

enum class AiProviderType(val displayName: String, val defaultModel: String) {
    GEMINI_FIREBASE("Gemini (Firebase AI Logic)", "gemini-3.8-flash"),
    OPENAI_COMPATIBLE("OpenAI-Compatible API", "gpt-4o-mini"),
    CUSTOM_ENDPOINT("Custom AI Endpoint", "custom-model")
}

data class AiProviderSettings(
    val provider: AiProviderType = AiProviderType.GEMINI_FIREBASE,
    val modelName: String = "gemini-3.8-flash",
    val customEndpointUrl: String = "",
    val customApiKey: String = "",
    val buildWorkerUrl: String = "",
    val buildWorkerToken: String = "",
    val autosaveEnabled: Boolean = true,
    val editorFontSizeSp: Int = 12
)

enum class AgentStepStatus { PENDING, RUNNING, DONE, REQUIRES_CONFIRM, FAILED }

data class AgentStep(
    val stepId: String,
    val phase: String,
    val detail: String,
    val status: AgentStepStatus
)

data class PendingFilePatch(
    val path: String,
    val language: String,
    val layer: CodeLayer,
    val previousContent: String?,
    val newContent: String,
    val summary: String,
    val isDestructive: Boolean = false
)

enum class DiagnosticSeverity { ERROR, WARNING, INFO }

data class DiagnosticIssue(
    val id: String,
    val filePath: String,
    val severity: DiagnosticSeverity,
    val message: String,
    val fixHint: String
)

enum class BuildTarget(val label: String, val gradleTask: String) {
    DEBUG_APK("Debug APK", "assembleDebug"),
    RELEASE_APK("Release APK", "assembleRelease"),
    APP_BUNDLE("Android App Bundle (AAB)", "bundleRelease")
}

data class BuildJobRecord(
    val buildId: String,
    val target: BuildTarget,
    val status: String, // QUEUED, RUNNING, SUCCEEDED, FAILED
    val createdAtLabel: String,
    val logs: String,
    val artifactUrl: String = ""
)

data class ApiExecutionResult(
    val statusCode: Int,
    val latencyMs: Long,
    val headersSummary: String,
    val responseBody: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class AiReviewReport(
    val overallScore: Int,
    val uiPolishScore: Int,
    val securityScore: Int,
    val scalabilityScore: Int,
    val summary: String,
    val strengths: List<String>,
    val recommendations: List<String>
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val content: String
)
