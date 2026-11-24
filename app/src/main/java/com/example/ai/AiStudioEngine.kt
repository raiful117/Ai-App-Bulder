package com.example.ai

import android.util.Log
import com.example.model.AiGenerationMode
import com.example.model.AiProviderSettings
import com.example.model.AiProviderType
import com.example.model.AiReviewReport
import com.example.model.AppComponent
import com.example.model.AppProject
import com.example.model.AppScreen
import com.example.model.AppThemePreset
import com.example.model.ArchitecturePattern
import com.example.model.BackendSpec
import com.example.model.BlockOpType
import com.example.model.CloudEndpointSpec
import com.example.model.CodeLayer
import com.example.model.ColorRole
import com.example.model.ComponentAction
import com.example.model.ComponentType
import com.example.model.DeploymentStatus
import com.example.model.EnvVarSpec
import com.example.model.FirestoreCollectionSpec
import com.example.model.LogicBlock
import com.example.model.PendingFilePatch
import com.example.model.ProjectFile
import com.example.model.ProjectGenerators
import com.example.model.ScalingTier
import com.example.model.StarterBlueprints
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "AiStudioEngine"
private const val AI_TIMEOUT_MS = 60_000L
private const val DEFAULT_MODEL = "gemini-3.8-flash"
private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"

class AiStudioEngine {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(55, TimeUnit.SECONDS)
        .build()

    private val componentSchema = Schema.obj(
        properties = mapOf(
            "type" to Schema.enumeration(ComponentType.entries.map { it.name }, "Visual UI component type"),
            "text" to Schema.string("Primary title, label, or content text"),
            "subtitle" to Schema.string("Secondary text, hint, comma-separated chart values/tabs, or boolean string"),
            "iconName" to Schema.string("Material icon keyword e.g. Home, Bolt, ShoppingCart, Analytics, Person, Lock, Star"),
            "colorRole" to Schema.enumeration(ColorRole.entries.map { it.name }, "Visual color emphasis"),
            "action" to Schema.enumeration(ComponentAction.entries.map { it.name }, "Tap action behavior"),
            "actionTarget" to Schema.string("Target screen route, Firestore collection, or message"),
            "dataBinding" to Schema.string("Optional Firestore/API data path e.g. firestore://orders/total")
        ),
        optionalProperties = listOf("subtitle", "iconName", "actionTarget", "dataBinding")
    )

    private val screenSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string("Short screen title e.g. Home, Catalog, Cart, Profile"),
            "route" to Schema.string("Lowercase route slug e.g. home, catalog"),
            "iconName" to Schema.string("Material icon name"),
            "components" to Schema.array(componentSchema, "Ordered list of 4 to 6 UI components for this screen")
        )
    )

    private val fullStackAppSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string("Short professional app name (max 40 chars)"),
            "packageName" to Schema.string("Valid reverse-DNS package name e.g. com.bcf.taskflow"),
            "tagline" to Schema.string("Concise 1-sentence value proposition"),
            "cloudRegion" to Schema.string("Preferred GCP/Firebase region e.g. asia-southeast1, us-central1"),
            "screens" to Schema.array(screenSchema, "2 to 3 cohesive app screens with rich UI components"),
            "collections" to Schema.array(
                Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string("Firestore collection name"),
                        "fieldsSummary" to Schema.string("Comma-separated typed fields"),
                        "accessRule" to Schema.string("Security policy summary")
                    )
                ),
                "2 to 3 Firestore collections"
            ),
            "endpoints" to Schema.array(
                Schema.obj(
                    properties = mapOf(
                        "method" to Schema.string("GET, POST, PUT, or DELETE"),
                        "path" to Schema.string("API route e.g. /api/v1/orders"),
                        "runtime" to Schema.string("Node.js TypeScript Express or Cloud Functions v2"),
                        "description" to Schema.string("Short purpose of endpoint")
                    )
                ),
                "1 to 2 backend endpoints"
            )
        )
    )

    private val screenComponentsSchema = Schema.obj(
        properties = mapOf(
            "components" to Schema.array(componentSchema, "4 to 7 well-structured UI components for this screen")
        )
    )

    private val targetedPatchSchema = Schema.obj(
        properties = mapOf(
            "planSummary" to Schema.string("Concise summary of the plan and targeted edits"),
            "patches" to Schema.array(
                Schema.obj(
                    properties = mapOf(
                        "path" to Schema.string("Relative project file path to create or update"),
                        "language" to Schema.string("kotlin, typescript, xml, json, rules, or sql"),
                        "layer" to Schema.enumeration(CodeLayer.entries.map { it.name }, "Architecture layer"),
                        "summary" to Schema.string("1-sentence explanation of change"),
                        "content" to Schema.string("Complete updated source file content")
                    )
                ),
                "1 to 3 targeted file patches"
            )
        )
    )

    private val logicBlocksSchema = Schema.obj(
        properties = mapOf(
            "blocks" to Schema.array(
                Schema.obj(
                    properties = mapOf(
                        "opType" to Schema.enumeration(BlockOpType.entries.map { it.name }, "Sketchware visual block operation type"),
                        "arg1" to Schema.string("Primary target/variable/title parameter"),
                        "arg2" to Schema.string("Secondary value/payload/expression parameter")
                    ),
                    optionalProperties = listOf("arg2")
                ),
                "Ordered sequence of 2 to 8 visual logic blocks"
            )
        )
    )

    private val reviewSchema = Schema.obj(
        properties = mapOf(
            "overallScore" to Schema.integer("Overall production readiness score 0-100"),
            "uiPolishScore" to Schema.integer("UI/UX & Material 3 quality score 0-100"),
            "securityScore" to Schema.integer("Zero-trust Firebase & backend security score 0-100"),
            "scalabilityScore" to Schema.integer("Cloud & architecture scalability score 0-100"),
            "summary" to Schema.string("2-3 sentence executive review of the app"),
            "strengths" to Schema.array(Schema.string(), "2 to 3 key architectural strengths"),
            "recommendations" to Schema.array(Schema.string(), "2 to 4 concrete improvements")
        )
    )

    private suspend fun generateGeminiWithFailover(
        preferredModel: String,
        prompt: String,
        schema: Schema?,
        maxTokens: Int = 4096
    ): String {
        if (runCatching { com.google.firebase.FirebaseApp.getInstance() }.isFailure) {
            throw IllegalStateException(
                "Firebase AI Logic requires google-services.json in the build. Configure an OpenAI-Compatible or Custom API endpoint & key in Settings -> AI Providers."
            )
        }
        val candidates = listOf(preferredModel.ifBlank { DEFAULT_MODEL }, FALLBACK_MODEL)
            .distinct()
        var lastError: Exception? = null

        for (modelId in candidates) {
            for (attempt in 0..1) {
                try {
                    val model = if (schema != null) {
                        Firebase.ai.generativeModel(
                            modelName = modelId,
                            generationConfig = generationConfig {
                                responseMimeType = "application/json"
                                responseSchema = schema
                                maxOutputTokens = maxTokens
                            }
                        )
                    } else {
                        Firebase.ai.generativeModel(modelName = modelId)
                    }

                    val text = withTimeout(AI_TIMEOUT_MS) {
                        val sb = StringBuilder()
                        model.generateContentStream(prompt).collect { chunk ->
                            sb.append(chunk.text.orEmpty())
                        }
                        sb.toString()
                    }
                    if (text.isBlank()) throw IllegalStateException("Model $modelId returned an empty response.")
                    return text
                } catch (e: CancellationException) {
                    if (e is TimeoutCancellationException) {
                        Log.w(TAG, "Model $modelId attempt $attempt timed out", e)
                        lastError = e
                    } else {
                        throw e
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Model $modelId attempt $attempt failed: ${e.message}", e)
                    lastError = e
                    if (!isTransientOverloadError(e)) {
                        // Try fallback model once even for non-transient errors if still on first model
                        break
                    }
                    delay(650L * (attempt + 1))
                }
            }
        }
        throw lastError ?: IllegalStateException("AI generation failed across all models.")
    }

    private fun isTransientOverloadError(e: Throwable): Boolean {
        val msg = generateSequence(e) { it.cause }
            .mapNotNull { it.message?.lowercase() }
            .joinToString(" ")
        return msg.contains("high demand") ||
            msg.contains("overloaded") ||
            msg.contains("unavailable") ||
            msg.contains("503") ||
            msg.contains("429") ||
            msg.contains("resource exhausted") ||
            msg.contains("rate limit") ||
            msg.contains("try again") ||
            msg.contains("timeout")
    }

    suspend fun generateFullStackApp(
        prompt: String,
        mode: AiGenerationMode,
        architecture: ArchitecturePattern,
        themePreset: AppThemePreset,
        userId: String,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): AppProject {
        val systemPrompt = """
            You are BCF Builder AI Architect designing an Android (Kotlin + Jetpack Compose + Material 3) & Node.js/Firebase full-stack app.
            Generation Mode: ${mode.label} (${mode.description})
            User request: "$prompt"
            Architecture: ${architecture.displayName} | Theme: ${themePreset.displayName}

            Generate a crisp JSON blueprint:
            - 2 to 3 cohesive screens with 4 to 6 components each (use TOP_APP_BAR, HERO_BANNER, STAT_METRIC, ROW_STATS, TEXT_INPUT, PASSWORD_FIELD, PRIMARY_BUTTON, LIST_TILE, RECYCLER_LIST, DATA_CHART, SWITCH_TOGGLE, CHIP_GROUP, BOTTOM_NAV).
            - Wire NAVIGATE actions cleanly between screen routes.
            - 2 to 3 Firestore collections and 1 to 2 backend API endpoints.
        """.trimIndent()

        val jsonText = if (providerSettings.provider == AiProviderType.GEMINI_FIREBASE) {
            generateGeminiWithFailover(
                preferredModel = providerSettings.modelName,
                prompt = systemPrompt,
                schema = fullStackAppSchema,
                maxTokens = 4096
            )
        } else {
            val schemaHint = """
                Return ONLY valid JSON with keys: name, packageName, tagline, cloudRegion, screens (array of {title, route, iconName, components: [{type, text, subtitle, iconName, colorRole, action, actionTarget}]}), collections ([{name, fieldsSummary, accessRule}]), endpoints ([{method, path, runtime, description}]).
            """.trimIndent()
            callExternalProvider(providerSettings, "$systemPrompt\n$schemaHint", expectJson = true)
        }

        val json = JSONObject(stripCodeFence(jsonText, "json"))
        val appName = json.optString("name", "BCF Generated App").take(80).ifBlank { "BCF Generated App" }
        val pkg = json.optString("packageName", "com.bcf.generated").take(120).ifBlank { "com.bcf.generated" }
        val tagline = json.optString("tagline", prompt.take(120)).take(180)
        val region = json.optString("cloudRegion", "asia-southeast1").take(40).ifBlank { "asia-southeast1" }

        val parsedScreens = parseScreens(json.optJSONArray("screens"))
        val screens = if (parsedScreens.isNotEmpty()) parsedScreens else StarterBlueprints.blankProject(userId, appName).screens
        val collections = parseCollections(json.optJSONArray("collections"))
        val endpoints = parseEndpoints(json.optJSONArray("endpoints"))
        val rules = StarterBlueprints.generateHardenedRules(collections)
        val baseFiles = StarterBlueprints.generateFullStackFiles(
            pkg = pkg,
            appName = appName,
            architecture = architecture,
            cloudRegion = region,
            screens = screens,
            collections = collections,
            endpoints = endpoints,
            rules = rules
        ).toMutableList()

        val draftProject = AppProject(
            userId = userId,
            name = appName,
            packageName = pkg,
            tagline = tagline,
            themePreset = themePreset,
            architecture = architecture,
            cloudRegion = region,
            scalingTier = ScalingTier.SERVERLESS_AUTO,
            deploymentStatus = DeploymentStatus.DRAFT,
            activeVersion = "v1.0.0",
            screens = screens,
            files = baseFiles,
            backend = BackendSpec(
                collections = collections,
                endpoints = endpoints,
                authGoogle = true,
                authEmail = mode == AiGenerationMode.FIREBASE_PROJECT || mode == AiGenerationMode.FULL_STACK,
                appCheckEnabled = true,
                firestoreRules = rules,
                envVars = listOf(
                    EnvVarSpec("APP_ENV", "production", true),
                    EnvVarSpec("CLOUD_REGION", region, true)
                )
            )
        )

        if (mode == AiGenerationMode.FULL_PROJECT || mode == AiGenerationMode.FULL_STACK) {
            ProjectGenerators.generateAndroidStudioGradleFiles(draftProject).forEach { gradleFile ->
                if (baseFiles.none { it.path.equals(gradleFile.path, ignoreCase = true) }) {
                    baseFiles += gradleFile
                }
            }
            ProjectGenerators.generateNodeTsBackendFiles(draftProject).forEach { beFile ->
                if (baseFiles.none { it.path.equals(beFile.path, ignoreCase = true) }) {
                    baseFiles += beFile
                }
            }
        } else if (mode == AiGenerationMode.BACKEND_ONLY) {
            ProjectGenerators.generateNodeTsBackendFiles(draftProject).forEach { beFile ->
                if (baseFiles.none { it.path.equals(beFile.path, ignoreCase = true) }) {
                    baseFiles += beFile
                }
            }
        }

        return draftProject.copy(files = baseFiles.take(60))
    }

    suspend fun generateTargetedProjectPatches(
        project: AppProject,
        instruction: String,
        mode: AiGenerationMode,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): Pair<String, List<PendingFilePatch>> {
        val fileListSummary = project.files.joinToString("\n") { "- ${it.path} (${it.layer.label}, ${it.content.lines().size} lines)" }
        val contextSnippets = project.files.take(4).joinToString("\n\n") { f ->
            "### ${f.path}\n```\n${f.content.take(1500)}\n```"
        }

        val prompt = """
            You are BCF Builder AI Project Agent operating in mode: ${mode.label}.
            Project: "${project.name}" (${project.packageName}), architecture: ${project.architecture.displayName}.
            Existing files:
            $fileListSummary

            Key snippets:
            $contextSnippets

            User instruction:
            "$instruction"

            Rules:
            - Do NOT replace unrelated files. Generate 1 to 3 targeted file patches (new or modified files) that integrate cleanly with package ${project.packageName}.
            - For GENERATE_TESTS, create Robolectric / JUnit tests under app/src/test/java/.
            - For BACKEND_ONLY, create/update files under backend/src/.
            - Return complete valid file content for each patched file.
        """.trimIndent()

        val jsonText = if (providerSettings.provider == AiProviderType.GEMINI_FIREBASE) {
            generateGeminiWithFailover(
                preferredModel = providerSettings.modelName,
                prompt = prompt,
                schema = targetedPatchSchema,
                maxTokens = 4096
            )
        } else {
            val schemaHint = """Return ONLY JSON: {"planSummary": "...", "patches": [{"path": "...", "language": "kotlin", "layer": "UI", "summary": "...", "content": "..."}]}"""
            callExternalProvider(providerSettings, "$prompt\n$schemaHint", expectJson = true)
        }

        val json = JSONObject(stripCodeFence(jsonText, "json"))
        val planSummary = json.optString("planSummary", "Targeted patches generated.")
        val arr = json.optJSONArray("patches") ?: JSONArray()
        val patches = (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val path = obj.optString("path").trim()
            val content = obj.optString("content")
            if (path.isBlank() || content.isBlank()) return@mapNotNull null
            val existing = project.files.firstOrNull { it.path.equals(path, ignoreCase = true) }
            PendingFilePatch(
                path = path,
                language = obj.optString("language", "kotlin").ifBlank { "kotlin" },
                layer = CodeLayer.safeValueOf(obj.optString("layer")),
                previousContent = existing?.content,
                newContent = content,
                summary = obj.optString("summary", "Updated $path"),
                isDestructive = path.contains("AndroidManifest") || path.contains("firestore.rules") || path.contains("build.gradle")
            )
        }
        return planSummary to patches
    }

    suspend fun generateScreenComponents(
        prompt: String,
        screenTitle: String,
        availableRoutes: List<String>,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): List<AppComponent> {
        val fullPrompt = """
            Generate a polished mobile UI layout for screen "$screenTitle" in an Android Jetpack Compose app.
            Goal: "$prompt"
            Available routes for NAVIGATE actions: ${availableRoutes.joinToString(", ")}
            Include a TOP_APP_BAR and 4 to 6 interactive/display components.
        """.trimIndent()

        val jsonText = if (providerSettings.provider == AiProviderType.GEMINI_FIREBASE) {
            generateGeminiWithFailover(
                preferredModel = providerSettings.modelName,
                prompt = fullPrompt,
                schema = screenComponentsSchema,
                maxTokens = 1024
            )
        } else {
            val schemaHint = """Return ONLY JSON: {"components":[{"type":"TOP_APP_BAR","text":"...","subtitle":"...","colorRole":"PRIMARY","action":"NONE","actionTarget":""}]}"""
            callExternalProvider(providerSettings, "$fullPrompt\n$schemaHint", expectJson = true)
        }

        val json = JSONObject(stripCodeFence(jsonText, "json"))
        return parseComponents(json.optJSONArray("components"))
    }

    suspend fun generateLogicBlocksFromPrompt(
        prompt: String,
        screenTitle: String,
        targetWidgetId: String,
        targetEvent: String,
        availableRoutes: List<String>,
        widgetNames: List<String>,
        moreBlockNames: List<String>,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): List<LogicBlock> {
        val fullPrompt = """
            You are Sketchware Pro / BCF Builder Visual Block Architect.
            Convert this user request (which may be in English or Bangla/Banglish) into an ordered sequence of visual LogicBlocks.
            Screen: "$screenTitle"
            Target Event: "$targetEvent" (widgetId="${targetWidgetId.ifBlank { "onCreate" }}")
            Available widget labels for UI_SET_TEXT / UI_SET_COLOR_VISIBILITY: ${widgetNames.joinToString(", ")}
            Available navigation routes for UI_NAVIGATE: ${availableRoutes.joinToString(", ")}
            Available custom MoreBlocks for CALL_MORE_BLOCK: ${moreBlockNames.joinToString(", ")}

            User instruction:
            "$prompt"

            Output 2 to 8 practical blocks using opTypes like CREATE_VAR, SET_VAR, IF_CONDITION, ELSE_BRANCH, REPEAT_LOOP, MATH_CALC, UI_SET_TEXT, UI_TOAST, DIALOG_SHOW, UI_NAVIGATE, SHARED_PREFS_SAVE, SHARED_PREFS_GET, HTTP_GET, FB_FIRESTORE_WRITE, CALL_MORE_BLOCK.
        """.trimIndent()

        val jsonText = if (providerSettings.provider == AiProviderType.GEMINI_FIREBASE) {
            generateGeminiWithFailover(
                preferredModel = providerSettings.modelName,
                prompt = fullPrompt,
                schema = logicBlocksSchema,
                maxTokens = 1024
            )
        } else {
            val schemaHint = """Return ONLY JSON: {"blocks":[{"opType":"UI_TOAST","arg1":"Saved!","arg2":""}]}"""
            callExternalProvider(providerSettings, "$fullPrompt\n$schemaHint", expectJson = true)
        }

        val json = JSONObject(stripCodeFence(jsonText, "json"))
        val arr = json.optJSONArray("blocks") ?: return emptyList()
        val normalizedTrigger = if (targetWidgetId.isBlank()) targetEvent.ifBlank { "onCreate" } else targetEvent.ifBlank { "onClick" }
        return (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val op = BlockOpType.safeValueOf(obj.optString("opType"))
            LogicBlock(
                blockId = StarterBlueprints.newId("blk"),
                opType = op,
                arg1 = obj.optString("arg1", op.defaultArg1).ifBlank { op.defaultArg1 },
                arg2 = obj.optString("arg2", op.defaultArg2),
                enabled = true,
                targetWidgetId = targetWidgetId,
                eventTrigger = normalizedTrigger
            )
        }
    }

    suspend fun refactorCodeFile(
        project: AppProject,
        file: ProjectFile,
        instruction: String,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): String {
        val prompt = """
            You are a senior Android/Cloud engineer working on "${project.name}" (${project.packageName}, architecture: ${project.architecture.displayName}).
            Update or expand `${file.path}` (${file.language}) per this instruction:
            "$instruction"

            Current file content:
            ```${file.language}
            ${file.content}
            ```

            Return ONLY the updated source code without markdown code fences or extra prose.
        """.trimIndent()

        val raw = if (providerSettings.provider == AiProviderType.GEMINI_FIREBASE) {
            generateGeminiWithFailover(
                preferredModel = providerSettings.modelName,
                prompt = prompt,
                schema = null,
                maxTokens = 4096
            )
        } else {
            callExternalProvider(providerSettings, prompt, expectJson = false)
        }

        if (raw.isBlank()) throw IllegalStateException("AI returned empty code.")
        return stripCodeFence(raw, file.language)
    }

    suspend fun explainOrDiagnoseCode(
        project: AppProject,
        file: ProjectFile?,
        question: String,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): String {
        val diagnostics = ProjectGenerators.runProjectDiagnostics(project).joinToString("\n") {
            "- [${it.severity}] ${it.filePath}: ${it.message}"
        }
        val prompt = """
            You are BCF Builder AI Engineer.
            Project: ${project.name} (${project.packageName})
            Diagnostics:
            $diagnostics
            ${if (file != null) "Focused File: ${file.path}\n```\n${file.content.take(3000)}\n```" else ""}

            Task: $question
            Provide a concise, actionable technical response.
        """.trimIndent()

        return if (providerSettings.provider == AiProviderType.GEMINI_FIREBASE) {
            generateGeminiWithFailover(
                preferredModel = providerSettings.modelName,
                prompt = prompt,
                schema = null,
                maxTokens = 2048
            ).ifBlank { "No explanation returned." }
        } else {
            callExternalProvider(providerSettings, prompt, expectJson = false)
        }
    }

    suspend fun runAppReview(
        project: AppProject,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): AiReviewReport {
        val screenSummary = project.screens.joinToString("\n") { s ->
            "- Screen '${s.title}' (${s.route}): ${s.components.size} components [${s.components.joinToString { it.type.name }}]"
        }
        val prompt = """
            Perform a rigorous senior architecture & UX review of this full-stack app project:
            Name: ${project.name}
            Tagline: ${project.tagline}
            Architecture: ${project.architecture.displayName}
            Cloud Region: ${project.cloudRegion}, Scaling: ${project.scalingTier.displayName}
            Auth Google: ${project.backend.authGoogle}, App Check: ${project.backend.appCheckEnabled}
            Collections: ${project.backend.collections.joinToString { it.name }}
            Endpoints: ${project.backend.endpoints.joinToString { "${it.method} ${it.path}" }}
            Code Files: ${project.files.size}
            Screens:
            $screenSummary

            Score overall, UI polish, security, and scalability (0-100), and provide concrete strengths & actionable recommendations.
        """.trimIndent()

        val jsonText = if (providerSettings.provider == AiProviderType.GEMINI_FIREBASE) {
            generateGeminiWithFailover(
                preferredModel = providerSettings.modelName,
                prompt = prompt,
                schema = reviewSchema,
                maxTokens = 1024
            )
        } else {
            val schemaHint = """Return ONLY JSON: {"overallScore":85,"uiPolishScore":85,"securityScore":85,"scalabilityScore":85,"summary":"...","strengths":["..."],"recommendations":["..."]}"""
            callExternalProvider(providerSettings, "$prompt\n$schemaHint", expectJson = true)
        }

        val json = JSONObject(stripCodeFence(jsonText, "json"))
        val strengthsArr = json.optJSONArray("strengths") ?: JSONArray()
        val recsArr = json.optJSONArray("recommendations") ?: JSONArray()

        return AiReviewReport(
            overallScore = json.optInt("overallScore", 85).coerceIn(0, 100),
            uiPolishScore = json.optInt("uiPolishScore", 86).coerceIn(0, 100),
            securityScore = json.optInt("securityScore", 84).coerceIn(0, 100),
            scalabilityScore = json.optInt("scalabilityScore", 88).coerceIn(0, 100),
            summary = json.optString("summary", "Architecture review completed."),
            strengths = (0 until strengthsArr.length()).map { strengthsArr.optString(it) },
            recommendations = (0 until recsArr.length()).map { recsArr.optString(it) }
        )
    }

    fun streamStudioAssistant(
        project: AppProject?,
        activeFile: ProjectFile?,
        userMessage: String,
        providerSettings: AiProviderSettings = AiProviderSettings()
    ): Flow<String> = flow {
        val contextHeader = if (project != null) {
            val diag = ProjectGenerators.runProjectDiagnostics(project).take(3).joinToString("; ") { "${it.severity}: ${it.message}" }
            """
            Active Project: ${project.name} (${project.packageName})
            Architecture: ${project.architecture.displayName}
            Screens (${project.screens.size}): ${project.screens.joinToString { "${it.title}(${it.components.size} UI, ${it.logicBlocks.size} blocks)" }}
            Files (${project.files.size}): ${project.files.take(12).joinToString { it.path }}
            Backend Collections: ${project.backend.collections.joinToString { it.name }}
            Endpoints: ${project.backend.endpoints.joinToString { "${it.method} ${it.path}" }}
            Active File: ${activeFile?.path ?: "none"}
            Diagnostics: $diag
            """.trimIndent()
        } else {
            "No active project selected yet."
        }

        val prompt = """
            You are BCF Builder AI Architect inside a full-stack Android IDE.
            $contextHeader

            User request:
            $userMessage

            Respond concisely with practical Kotlin/Compose, Sketchware logic blocks, Firebase, or Node.js backend guidance tailored to the active project.
        """.trimIndent()

        if (providerSettings.provider != AiProviderType.GEMINI_FIREBASE) {
            emit(callExternalProvider(providerSettings, prompt, expectJson = false))
            return@flow
        }

        if (runCatching { com.google.firebase.FirebaseApp.getInstance() }.isFailure) {
            throw IllegalStateException(
                "Firebase AI Logic requires google-services.json in the build. Configure an OpenAI-Compatible or Custom API endpoint & key in Settings -> AI Providers."
            )
        }

        val candidates = listOf(providerSettings.modelName.ifBlank { DEFAULT_MODEL }, FALLBACK_MODEL).distinct()
        var lastErr: Exception? = null
        for (modelId in candidates) {
            try {
                val sb = StringBuilder()
                val chatModel = Firebase.ai.generativeModel(modelName = modelId)
                withTimeout(AI_TIMEOUT_MS) {
                    chatModel.generateContentStream(prompt).collect { chunk ->
                        val delta = chunk.text.orEmpty()
                        if (delta.isNotEmpty()) {
                            sb.append(delta)
                            emit(sb.toString())
                        }
                    }
                }
                if (sb.isNotEmpty()) return@flow
                throw IllegalStateException("AI stream returned no content.")
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    lastErr = e
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.w(TAG, "Assistant stream failed on $modelId: ${e.message}", e)
                lastErr = e
                delay(500L)
            }
        }
        throw lastErr ?: IllegalStateException("AI assistant failed across all models.")
    }

    private suspend fun callExternalProvider(
        settings: AiProviderSettings,
        prompt: String,
        expectJson: Boolean
    ): String = withContext(Dispatchers.IO) {
        val endpoint = settings.customEndpointUrl.trim().ifBlank {
            if (settings.provider == AiProviderType.OPENAI_COMPATIBLE) {
                "https://api.openai.com/v1/chat/completions"
            } else ""
        }
        require(endpoint.startsWith("http")) {
            "Configure a valid AI endpoint URL in Settings -> AI Providers."
        }
        require(settings.customApiKey.isNotBlank()) {
            "Provide an API key in Settings -> AI Providers (or switch back to Gemini via Firebase AI Logic)."
        }

        val payload = JSONObject().apply {
            put("model", settings.modelName.ifBlank { settings.provider.defaultModel })
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", prompt))
                }
            )
            if (expectJson) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
        }.toString()

        val req = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${settings.customApiKey.trim()}")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Provider HTTP ${resp.code}: ${raw.take(240)}")
            }
            val root = JSONObject(raw)
            val choices = root.optJSONArray("choices")
            val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
            if (content.isBlank()) throw IllegalStateException("External AI provider returned empty content.")
            stripCodeFence(content, "json")
        }
    }

    private fun stripCodeFence(raw: String, lang: String): String =
        raw.trim()
            .removePrefix("```$lang")
            .removePrefix("```kotlin")
            .removePrefix("```typescript")
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

    private fun parseScreens(arr: JSONArray?): List<AppScreen> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val title = obj.optString("title", "Screen ${idx + 1}").ifBlank { "Screen ${idx + 1}" }
            val route = obj.optString("route", title.lowercase().replace(Regex("[^a-z0-9]"), ""))
                .ifBlank { "screen_$idx" }
            AppScreen(
                screenId = StarterBlueprints.newId("scr"),
                title = title,
                route = route,
                iconName = obj.optString("iconName", "Dashboard").ifBlank { "Dashboard" },
                components = parseComponents(obj.optJSONArray("components"))
            )
        }
    }

    private fun parseComponents(arr: JSONArray?): List<AppComponent> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val type = ComponentType.safeValueOf(obj.optString("type"))
            AppComponent(
                componentId = StarterBlueprints.newId("cmp"),
                type = type,
                text = obj.optString("text", type.defaultText).ifBlank { type.defaultText },
                subtitle = obj.optString("subtitle", type.defaultSubtitle),
                iconName = obj.optString("iconName", "Widgets").ifBlank { "Widgets" },
                colorRole = ColorRole.safeValueOf(obj.optString("colorRole")),
                action = ComponentAction.safeValueOf(obj.optString("action")),
                actionTarget = obj.optString("actionTarget", ""),
                dataBinding = obj.optString("dataBinding", "")
            )
        }
    }

    private fun parseCollections(arr: JSONArray?): List<FirestoreCollectionSpec> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            FirestoreCollectionSpec(
                name = obj.optString("name", "items"),
                fieldsSummary = obj.optString("fieldsSummary", "userId, title, createdAt"),
                accessRule = obj.optString("accessRule", "Owner Only")
            )
        }
    }

    private fun parseEndpoints(arr: JSONArray?): List<CloudEndpointSpec> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            CloudEndpointSpec(
                method = obj.optString("method", "POST"),
                path = obj.optString("path", "/api/v1/action"),
                runtime = obj.optString("runtime", "Node.js TypeScript • Express"),
                description = obj.optString("description", "Serverless handler")
            )
        }
    }
}
