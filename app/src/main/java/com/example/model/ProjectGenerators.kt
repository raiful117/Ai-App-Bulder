package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ProjectGenerators {

    fun generateAndroidStudioGradleFiles(project: AppProject): List<ProjectFile> {
        val cfg = project.backend.androidConfig
        val pkg = project.packageName.ifBlank { "com.bcf.app" }
        val permissionsXml = cfg.permissions.distinct().joinToString("\n") {
            "    <uses-permission android:name=\"$it\" />"
        }
        val extraDeps = cfg.dependencies.distinct().joinToString("\n") {
            "    implementation(\"$it\")"
        }
        val navRoutes = project.screens.joinToString("\n") { scr ->
            val cls = scr.title.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Main" }
            "        composable(\"${scr.route}\") { ${cls}Screen(onNavigate = { r -> navController.navigate(r) }) }"
        }

        val layoutFiles = project.screens.map { scr ->
            val safeRoute = scr.route.lowercase().replace(Regex("[^a-z0-9_]"), "_").ifBlank { "main" }
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "app/src/main/res/layout/activity_${safeRoute}.xml",
                language = "xml",
                layer = CodeLayer.UI,
                content = generateAndroidXmlLayout(scr)
            )
        }

        val resourceFiles = generateResourceXmlFiles(project)

        return listOf(
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "settings.gradle.kts",
                language = "kotlin",
                layer = CodeLayer.CONFIG,
                content = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "${project.name.replace("\"", "")}"
include(":app")
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "build.gradle.kts",
                language = "kotlin",
                layer = CodeLayer.CONFIG,
                content = """plugins {
    id("com.android.application") version "8.9.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "gradle.properties",
                language = "properties",
                layer = CodeLayer.CONFIG,
                content = """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "app/build.gradle.kts",
                language = "kotlin",
                layer = CodeLayer.CONFIG,
                content = """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "$pkg"
    compileSdk = ${cfg.targetSdk}

    defaultConfig {
        applicationId = "$pkg"
        minSdk = ${cfg.minSdk}
        targetSdk = ${cfg.targetSdk}
        versionCode = ${cfg.versionCode}
        versionName = "${cfg.versionName}"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
$extraDeps
}
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "app/src/main/AndroidManifest.xml",
                language = "xml",
                layer = CodeLayer.CONFIG,
                content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
$permissionsXml

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "app/src/main/java/${pkg.replace('.', '/')}/navigation/AppNavGraph.kt",
                language = "kotlin",
                layer = CodeLayer.UI,
                content = """package $pkg.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import $pkg.ui.*

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "${project.screens.firstOrNull()?.route ?: "home"}"
    ) {
$navRoutes
    }
}
"""
            )
        ) + resourceFiles + layoutFiles
    }

    fun generateAndroidXmlLayout(screen: AppScreen): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        sb.appendLine("""<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"""")
        sb.appendLine("""    xmlns:app="http://schemas.android.com/apk/res-auto"""")
        sb.appendLine("""    android:layout_width="match_parent"""")
        sb.appendLine("""    android:layout_height="match_parent"""")
        sb.appendLine("""    android:fillViewport="true">""")
        sb.appendLine("""    <LinearLayout""")
        sb.appendLine("""        android:id="@+id/root_${screen.route}"""")
        sb.appendLine("""        android:layout_width="match_parent"""")
        sb.appendLine("""        android:layout_height="wrap_content"""")
        sb.appendLine("""        android:orientation="vertical"""")
        sb.appendLine("""        android:padding="16dp">""")

        screen.components.forEachIndexed { idx, comp ->
            val id = comp.componentId.replace(Regex("[^A-Za-z0-9_]"), "_").ifBlank { "view_$idx" }
            val safeText = escapeXml(comp.text)
            val widthAttr = if (comp.widthMode == "WRAP_CONTENT") "wrap_content" else "match_parent"
            val widgetTag = when (comp.type) {
                ComponentType.PRIMARY_BUTTON, ComponentType.OUTLINED_BUTTON -> "com.google.android.material.button.MaterialButton"
                ComponentType.TEXT_INPUT, ComponentType.PASSWORD_FIELD -> "com.google.android.material.textfield.TextInputEditText"
                ComponentType.SWITCH_TOGGLE -> "com.google.android.material.materialswitch.MaterialSwitch"
                ComponentType.CHECKBOX -> "CheckBox"
                ComponentType.RADIO_BUTTON -> "RadioGroup"
                ComponentType.SLIDER_CONTROL -> "SeekBar"
                ComponentType.PROGRESS_BAR -> "ProgressBar"
                ComponentType.IMAGE_BANNER -> "ImageView"
                ComponentType.IMAGE_BUTTON -> "ImageButton"
                ComponentType.RECYCLER_LIST -> "androidx.recyclerview.widget.RecyclerView"
                ComponentType.WEB_VIEW -> "WebView"
                ComponentType.TAB_LAYOUT -> "com.google.android.material.tabs.TabLayout"
                ComponentType.VIEW_PAGER -> "androidx.viewpager2.widget.ViewPager2"
                ComponentType.RATING_BAR -> "RatingBar"
                ComponentType.SPINNER_DROPDOWN -> "Spinner"
                ComponentType.FAB_BUTTON -> "com.google.android.material.floatingactionbutton.FloatingActionButton"
                ComponentType.TOP_APP_BAR -> "com.google.android.material.appbar.MaterialToolbar"
                else -> "TextView"
            }
            sb.appendLine("""        <$widgetTag""")
            sb.appendLine("""            android:id="@+id/$id"""")
            sb.appendLine("""            android:layout_width="$widthAttr"""")
            sb.appendLine("""            android:layout_height="wrap_content"""")
            sb.appendLine("""            android:layout_marginBottom="10dp"""")
            if (widgetTag.endsWith("TextView") || widgetTag.endsWith("Button") || widgetTag.endsWith("CheckBox") || widgetTag.endsWith("Switch")) {
                sb.appendLine("""            android:text="$safeText"""")
            } else if (widgetTag.endsWith("TextInputEditText")) {
                sb.appendLine("""            android:hint="$safeText"""")
            }
            sb.appendLine("""            android:padding="${comp.paddingDp}dp" />""")
        }

        sb.appendLine("""    </LinearLayout>""")
        sb.appendLine("""</ScrollView>""")
        return sb.toString()
    }

    fun generateResourceXmlFiles(project: AppProject): List<ProjectFile> {
        val stringsBuilder = StringBuilder()
        stringsBuilder.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        stringsBuilder.appendLine("""<resources>""")
        stringsBuilder.appendLine("""    <string name="app_name">${escapeXml(project.name.ifBlank { "BCF App" })}</string>""")
        project.backend.stringResources.forEach { (k, v) ->
            val safeKey = k.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "str_item" }
            if (safeKey != "app_name") {
                stringsBuilder.appendLine("""    <string name="$safeKey">${escapeXml(v)}</string>""")
            }
        }
        stringsBuilder.appendLine("""</resources>""")

        val colorsBuilder = StringBuilder()
        colorsBuilder.appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        colorsBuilder.appendLine("""<resources>""")
        project.backend.colorResources.forEach { (k, hex) ->
            val safeKey = k.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "color_item" }
            val safeHex = if (hex.startsWith("#")) hex else "#$hex"
            colorsBuilder.appendLine("""    <color name="$safeKey">$safeHex</color>""")
        }
        colorsBuilder.appendLine("""</resources>""")

        return listOf(
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "app/src/main/res/values/strings.xml",
                language = "xml",
                layer = CodeLayer.CONFIG,
                content = stringsBuilder.toString()
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "app/src/main/res/values/colors.xml",
                language = "xml",
                layer = CodeLayer.CONFIG,
                content = colorsBuilder.toString()
            )
        )
    }

    private fun escapeXml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    fun generateNodeTsBackendFiles(project: AppProject): List<ProjectFile> {
        val b = project.backend
        val slug = project.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "bcf-app" }
        val routeHandlers = b.endpoints.ifEmpty {
            listOf(CloudEndpointSpec("GET", "/api/v1/health", "Node.js TypeScript • Express", "Health check"))
        }.joinToString("\n\n") { ep ->
            val method = ep.method.lowercase()
            val authGuard = if (ep.authRequired && b.jwtEnabled) "requireJwt, " else ""
            """router.$method("${ep.path}", $authGuard(req, res) => {
  // ${ep.description}
  res.json({
    ok: true,
    service: "$slug",
    method: "${ep.method}",
    path: "${ep.path}",
    timestamp: new Date().toISOString()
  });
});"""
        }

        val envLines = b.envVars.joinToString("\n") { "${it.key}=${it.valueHint}" }

        return listOf(
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "backend/package.json",
                language = "json",
                layer = CodeLayer.BACKEND,
                content = """{
  "name": "$slug-backend",
  "version": "${project.activeVersion.removePrefix("v")}",
  "private": true,
  "scripts": {
    "dev": "tsx watch src/server.ts",
    "build": "tsc",
    "start": "node dist/server.js"
  },
  "dependencies": {
    "cors": "^2.8.5",
    "dotenv": "^16.4.7",
    "express": "^4.21.2",
    "express-rate-limit": "^7.5.0",
    "helmet": "^8.0.0",
    "jsonwebtoken": "^9.0.2",
    "zod": "^3.24.2"
  },
  "devDependencies": {
    "@types/express": "^5.0.0",
    "@types/jsonwebtoken": "^9.0.7",
    "tsx": "^4.19.2",
    "typescript": "^5.7.3"
  }
}
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "backend/tsconfig.json",
                language = "json",
                layer = CodeLayer.BACKEND,
                content = """{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "outDir": "dist",
    "rootDir": "src",
    "strict": true,
    "esModuleInterop": true
  },
  "include": ["src/**/*"]
}
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "backend/.env.example",
                language = "properties",
                layer = CodeLayer.BACKEND,
                content = """PORT=8080
DATABASE_URL=postgresql://postgres:password@localhost:5432/$slug
JWT_SECRET=replace_with_32_char_secret
$envLines
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "backend/src/middleware/auth.ts",
                language = "typescript",
                layer = CodeLayer.BACKEND,
                content = """import type { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

export function requireJwt(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header?.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Missing Bearer token" });
  }
  try {
    const token = header.slice(7);
    const payload = jwt.verify(token, process.env.JWT_SECRET || "dev-secret");
    (req as Request & { user?: unknown }).user = payload;
    next();
  } catch {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
}
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "backend/src/routes/api.ts",
                language = "typescript",
                layer = CodeLayer.BACKEND,
                content = """import { Router } from "express";
import { requireJwt } from "../middleware/auth.js";

export const router = Router();

$routeHandlers
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "backend/src/app.ts",
                language = "typescript",
                layer = CodeLayer.BACKEND,
                content = """import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { router as apiRouter } from "./routes/api.js";

export function createApp() {
  const app = express();
  app.use(helmet());
  app.use(cors());
  app.use(express.json({ limit: "2mb" }));
  ${if (b.rateLimitEnabled) "app.use(rateLimit({ windowMs: 60_000, max: 120 }));" else "// Rate limiting disabled"}
  app.get("/health", (_req, res) => res.json({ status: "ok" }));
  app.use(apiRouter);
  return app;
}
"""
            ),
            ProjectFile(
                fileId = StarterBlueprints.newId("file"),
                path = "backend/src/server.ts",
                language = "typescript",
                layer = CodeLayer.BACKEND,
                content = """import "dotenv/config";
import { createApp } from "./app.js";

const port = Number(process.env.PORT || 8080);
createApp().listen(port, () => {
  console.log("${project.name} backend listening on port", port);
});
"""
            )
        )
    }

    fun generateApiClientKotlin(packageName: String, spec: ApiRequestSpec): ProjectFile {
        val safeName = spec.name.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Custom" }
        return ProjectFile(
            fileId = StarterBlueprints.newId("file"),
            path = "app/src/main/java/${packageName.replace('.', '/')}/network/${safeName}Client.kt",
            language = "kotlin",
            layer = CodeLayer.DATA,
            content = """package $packageName.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ${safeName}Client(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun execute(token: String = ""): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val builder = Request.Builder().url("${spec.url}")
            if (token.isNotBlank()) builder.header("Authorization", "Bearer ${'$'}token")
            ${if (spec.method in listOf("POST", "PUT", "PATCH")) {
                "val body = \"\"\"${spec.bodyJson}\"\"\".toRequestBody(\"application/json\".toMediaType())\n            builder.method(\"${spec.method}\", body)"
            } else {
                "builder.method(\"${spec.method}\", null)"
            }}
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${'$'}{resp.code}")
                resp.body?.string().orEmpty()
            }
        }
    }
}
"""
        )
    }

    fun generateSqlMigration(projectName: String, tables: List<DatabaseTableSpec>): String {
        val sb = StringBuilder("-- Migration generated by BCF Builder AI for $projectName\n\n")
        tables.forEach { t ->
            sb.appendLine("CREATE TABLE IF NOT EXISTS ${t.name} (")
            val cols = t.columnsSummary.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (cols.isEmpty()) {
                sb.appendLine("  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),")
                sb.appendLine("  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()")
            } else {
                cols.forEachIndexed { idx, col ->
                    val comma = if (idx < cols.lastIndex) "," else ""
                    sb.appendLine("  $col$comma")
                }
            }
            sb.appendLine(");")
            if (t.indexSummary.isNotBlank()) {
                sb.appendLine("-- Suggested Index: ${t.indexSummary}")
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    fun runProjectDiagnostics(project: AppProject): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        val paths = project.files.map { it.path }

        if (project.packageName.count { it == '.' } < 1 || project.packageName.contains(" ")) {
            issues += DiagnosticIssue(
                id = "pkg_invalid",
                filePath = "app/build.gradle.kts",
                severity = DiagnosticSeverity.ERROR,
                message = "Invalid Android package name '${project.packageName}'. Must be reverse-DNS (e.g. com.company.app).",
                fixHint = "Update package name in Settings or Designer."
            )
        }
        if (project.screens.isEmpty()) {
            issues += DiagnosticIssue(
                id = "no_screens",
                filePath = "screens/",
                severity = DiagnosticSeverity.ERROR,
                message = "Project has 0 screens.",
                fixHint = "Add at least one screen in Visual Designer."
            )
        }
        if (paths.none { it.endsWith("AndroidManifest.xml") }) {
            issues += DiagnosticIssue(
                id = "missing_manifest",
                filePath = "app/src/main/AndroidManifest.xml",
                severity = DiagnosticSeverity.WARNING,
                message = "AndroidManifest.xml not yet generated in project files.",
                fixHint = "Tap 'Sync Android Studio Structure' in Build Center."
            )
        }
        if (paths.none { it.endsWith("app/build.gradle.kts") }) {
            issues += DiagnosticIssue(
                id = "missing_gradle",
                filePath = "app/build.gradle.kts",
                severity = DiagnosticSeverity.WARNING,
                message = "Gradle build configuration missing from file tree.",
                fixHint = "Generate Gradle & Manifest files before exporting Android Studio ZIP."
            )
        }
        if (project.backend.firebaseConfig.useFirestore && project.backend.firestoreRules.isBlank()) {
            issues += DiagnosticIssue(
                id = "missing_rules",
                filePath = "firestore.rules",
                severity = DiagnosticSeverity.ERROR,
                message = "Firestore is enabled without security rules.",
                fixHint = "Generate hardened owner-scoped firestore.rules."
            )
        }
        project.files.forEach { file ->
            if (file.content.contains("AIza") && file.layer != CodeLayer.CONFIG) {
                issues += DiagnosticIssue(
                    id = "secret_warn_${file.fileId}",
                    filePath = file.path,
                    severity = DiagnosticSeverity.WARNING,
                    message = "Possible hardcoded key pattern detected in source file.",
                    fixHint = "Move credentials to BuildConfig or server-side environment variables."
                )
            }
        }
        if (issues.isEmpty()) {
            issues += DiagnosticIssue(
                id = "all_clean",
                filePath = "Project Tree",
                severity = DiagnosticSeverity.INFO,
                message = "All checks passed (${project.files.size} files, ${project.screens.size} screens, zero critical errors).",
                fixHint = "Ready to export Android Studio ZIP or send to Build Worker."
            )
        }
        return issues
    }

    fun writeProjectZip(project: AppProject, outputStream: OutputStream, mode: String): Int {
        val baseFiles = project.files.toMutableList()
        if (mode == "ANDROID_STUDIO" || mode == "FULL_STACK") {
            generateAndroidStudioGradleFiles(project).forEach { gen ->
                if (baseFiles.none { it.path.equals(gen.path, ignoreCase = true) }) {
                    baseFiles += gen
                }
            }
        }
        if (mode == "BACKEND" || mode == "FULL_STACK") {
            generateNodeTsBackendFiles(project).forEach { gen ->
                if (baseFiles.none { it.path.equals(gen.path, ignoreCase = true) }) {
                    baseFiles += gen
                }
            }
        }
        val filtered = when (mode) {
            "SOURCE_ONLY" -> baseFiles.filter { it.path.endsWith(".kt") || it.path.endsWith(".java") || it.path.endsWith(".xml") }
            "BACKEND" -> baseFiles.filter { it.path.startsWith("backend/") || it.layer == CodeLayer.BACKEND }
            else -> baseFiles
        }

        val writtenPaths = mutableSetOf<String>()
        ZipOutputStream(outputStream).use { zos ->
            filtered.forEach { file ->
                val entryName = file.path.trimStart('/')
                if (entryName.isNotBlank() && writtenPaths.add(entryName)) {
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(file.content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            if (writtenPaths.add("README.md")) {
                val readme = """# ${project.name}

Generated by **BCF Builder AI** (`${project.packageName}`).
- Architecture: ${project.architecture.displayName}
- Screens: ${project.screens.size}
- Cloud Region: ${project.cloudRegion}
- Android SDK: minSdk ${project.backend.androidConfig.minSdk}, targetSdk ${project.backend.androidConfig.targetSdk}
"""
                zos.putNextEntry(ZipEntry("README.md"))
                zos.write(readme.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return writtenPaths.size
    }

    fun exportProjectJson(project: AppProject): String {
        val root = JSONObject()
        root.put("format", "bcf-builder-project-v1")
        root.put("name", project.name)
        root.put("packageName", project.packageName)
        root.put("tagline", project.tagline)
        root.put("themePreset", project.themePreset.name)
        root.put("architecture", project.architecture.name)
        root.put("cloudRegion", project.cloudRegion)
        root.put("scalingTier", project.scalingTier.name)
        root.put("activeVersion", project.activeVersion)
        root.put("screens", JSONArray(project.screens.map { JSONObject(it.toMap()) }))
        root.put("files", JSONArray(project.files.map { JSONObject(it.toMap()) }))
        root.put("backend", JSONObject(project.backend.toMap()))
        return root.toString(2)
    }

    fun importProjectJson(rawJson: String, userId: String): Pair<AppProject?, String> {
        return try {
            val obj = JSONObject(rawJson)
            val name = obj.optString("name", "Imported App").ifBlank { "Imported App" }
            val screensArr = obj.optJSONArray("screens") ?: JSONArray()
            val filesArr = obj.optJSONArray("files") ?: JSONArray()
            val screens = (0 until screensArr.length()).mapNotNull { idx ->
                val s = screensArr.optJSONObject(idx) ?: return@mapNotNull null
                AppScreen.fromMap(jsonToMap(s))
            }
            val files = (0 until filesArr.length()).mapNotNull { idx ->
                val f = filesArr.optJSONObject(idx) ?: return@mapNotNull null
                ProjectFile.fromMap(jsonToMap(f))
            }
            val backendObj = obj.optJSONObject("backend")
            val backend = if (backendObj != null) BackendSpec.fromMap(jsonToMap(backendObj)) else StarterBlueprints.defaultBackendFor(name)
            val project = AppProject(
                userId = userId,
                name = name,
                packageName = obj.optString("packageName", "com.bcf.imported").ifBlank { "com.bcf.imported" },
                tagline = obj.optString("tagline", "Imported BCF Builder AI project"),
                themePreset = AppThemePreset.safeValueOf(obj.optString("themePreset")),
                architecture = ArchitecturePattern.safeValueOf(obj.optString("architecture")),
                cloudRegion = obj.optString("cloudRegion", "asia-southeast1"),
                scalingTier = ScalingTier.safeValueOf(obj.optString("scalingTier")),
                deploymentStatus = DeploymentStatus.DRAFT,
                activeVersion = obj.optString("activeVersion", "v1.0.0"),
                screens = screens.ifEmpty { StarterBlueprints.blankProject(userId, name).screens },
                files = files.ifEmpty { StarterBlueprints.defaultFilesFor("com.bcf.imported", name, screens) },
                backend = backend
            )
            project to "Validated import: ${project.screens.size} screens, ${project.files.size} files."
        } catch (e: Exception) {
            null to "Invalid project JSON: ${e.localizedMessage ?: "Parse error"}"
        }
    }

    private fun jsonToMap(obj: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = jsonVal(obj.get(k))
        }
        return map
    }

    private fun jsonVal(v: Any?): Any? = when (v) {
        is JSONObject -> jsonToMap(v)
        is JSONArray -> (0 until v.length()).map { jsonVal(v.get(it)) }
        JSONObject.NULL -> null
        else -> v
    }
}
