package com.example.model

object BlockCompiler {

    fun compileScreenBlocksToKotlin(
        packageName: String,
        screen: AppScreen,
        sketchComponents: List<SketchComponent> = emptyList(),
        customMoreBlocks: List<CustomMoreBlock> = emptyList()
    ): String {
        val safeClass = screen.title.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "Main" }
        val blocks = screen.logicBlocks.filter { it.enabled }
        val sb = StringBuilder()

        sb.appendLine("package $packageName.logic")
        sb.appendLine()
        sb.appendLine("import android.app.AlertDialog")
        sb.appendLine("import android.content.Context")
        sb.appendLine("import android.content.Intent")
        sb.appendLine("import android.content.SharedPreferences")
        sb.appendLine("import android.net.Uri")
        sb.appendLine("import android.os.Handler")
        sb.appendLine("import android.os.Looper")
        sb.appendLine("import android.speech.tts.TextToSpeech")
        sb.appendLine("import android.widget.Toast")
        sb.appendLine("import com.google.firebase.Firebase")
        sb.appendLine("import com.google.firebase.auth.auth")
        sb.appendLine("import com.google.firebase.firestore.FieldValue")
        sb.appendLine("import com.google.firebase.firestore.FirebaseFirestore")
        sb.appendLine("import kotlinx.coroutines.Dispatchers")
        sb.appendLine("import kotlinx.coroutines.tasks.await")
        sb.appendLine("import kotlinx.coroutines.withContext")
        sb.appendLine("import okhttp3.MediaType.Companion.toMediaType")
        sb.appendLine("import okhttp3.OkHttpClient")
        sb.appendLine("import okhttp3.Request")
        sb.appendLine("import okhttp3.RequestBody.Companion.toRequestBody")
        sb.appendLine("import java.util.Locale")
        sb.appendLine()
        sb.appendLine("/**")
        sb.appendLine(" * Auto-compiled by BCF Builder AI Visual Block Engine (Sketchware Pro + Studio)")
        sb.appendLine(" * Screen: ${screen.title} (route: ${screen.route})")
        sb.appendLine(" * Blocks compiled: ${blocks.size} | Components: ${sketchComponents.size} | MoreBlocks: ${customMoreBlocks.size}")
        sb.appendLine(" */")
        sb.appendLine("class ${safeClass}BlockController(")
        sb.appendLine("    private val context: Context,")
        sb.appendLine("    private val db: FirebaseFirestore? = null,")
        sb.appendLine("    private val httpClient: OkHttpClient = OkHttpClient()")
        sb.appendLine(") {")
        sb.appendLine("    private val stateMap = mutableMapOf<String, Any?>()")
        sb.appendLine("    private val sharedPrefs: SharedPreferences by lazy {")
        sb.appendLine("        context.getSharedPreferences(\"${safeClass.lowercase()}_prefs\", Context.MODE_PRIVATE)")
        sb.appendLine("    }")
        sb.appendLine("    private val mainHandler = Handler(Looper.getMainLooper())")
        if (sketchComponents.any { it.type == SketchComponentType.TEXT_TO_SPEECH } ||
            blocks.any { it.opType == BlockOpType.TTS_SPEAK }
        ) {
            sb.appendLine("    private var tts: TextToSpeech? = null")
        }
        if (sketchComponents.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("    // Non-UI Sketchware Components")
            sketchComponents.forEach { comp ->
                sb.appendLine("    // Component '${comp.name}' (${comp.type.androidClass}) ${if (comp.configNote.isNotBlank()) "- ${comp.configNote}" else ""}")
            }
        }
        sb.appendLine()

        val createBlocks = blocks.filter {
            it.targetWidgetId.isBlank() || it.eventTrigger == "onCreate" || it.eventTrigger == "onStart" || it.eventTrigger == "onResume"
        }
        val widgetGroups = blocks.filter {
            it.targetWidgetId.isNotBlank() &&
                !it.targetWidgetId.startsWith("moreblock:") &&
                it.eventTrigger !in listOf("onCreate", "onStart", "onResume")
        }.groupBy { it.targetWidgetId to it.eventTrigger }

        sb.appendLine("    suspend fun onScreenCreate(onNavigate: (String) -> Unit, onUiUpdate: (String, String) -> Unit) {")
        if (createBlocks.isEmpty()) {
            sb.appendLine("        // Add visual logic blocks in the Blocks workspace to generate onCreate logic.")
        } else {
            appendBlocks(sb, createBlocks, "        ")
        }
        sb.appendLine("    }")
        sb.appendLine()

        sb.appendLine("    suspend fun dispatchWidgetEvent(widgetId: String, event: String, onNavigate: (String) -> Unit, onUiUpdate: (String, String) -> Unit) {")
        if (widgetGroups.isEmpty()) {
            sb.appendLine("        // Click any widget in Designer -> '+ Blocks' or Events hub to attach logic.")
        } else {
            sb.appendLine("        when (widgetId to event) {")
            widgetGroups.forEach { (key, grpBlocks) ->
                val (wId, ev) = key
                sb.appendLine("            \"$wId\" to \"$ev\" -> {")
                appendBlocks(sb, grpBlocks, "                ")
                sb.appendLine("            }")
            }
            sb.appendLine("        }")
        }
        sb.appendLine("    }")

        if (customMoreBlocks.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("    // Custom Reusable MoreBlocks")
            customMoreBlocks.forEach { mb ->
                val fnName = mb.name.replace(Regex("[^A-Za-z0-9_]"), "").ifBlank { "customAction" }
                val mbBlocks = blocks.filter {
                    it.targetWidgetId == "moreblock:${mb.name}" || it.targetWidgetId == mb.blockId
                }
                sb.appendLine("    suspend fun $fnName(${mb.paramsSignature}, onNavigate: (String) -> Unit = {}, onUiUpdate: (String, String) -> Unit = { _, _ -> }) {")
                if (mbBlocks.isEmpty()) {
                    sb.appendLine("        // ${mb.description.ifBlank { "Custom reusable block body" }}")
                } else {
                    appendBlocks(sb, mbBlocks, "        ")
                }
                sb.appendLine("    }")
                sb.appendLine()
            }
        }

        sb.appendLine("}")
        return sb.toString()
    }

    private fun appendBlocks(sb: StringBuilder, blocks: List<LogicBlock>, baseIndent: String) {
        var indent = baseIndent
        blocks.forEach { block ->
            val a1 = block.arg1.ifBlank { block.opType.defaultArg1 }
            val a2 = block.arg2.ifBlank { block.opType.defaultArg2 }
            when (block.opType) {
                BlockOpType.ON_APP_START,
                BlockOpType.ON_SCREEN_CREATE,
                BlockOpType.ON_BUTTON_CLICK,
                BlockOpType.ON_TEXT_CHANGE,
                BlockOpType.ON_TIMER,
                BlockOpType.ON_NETWORK_RESPONSE,
                BlockOpType.ON_FIREBASE_EVENT -> {
                    sb.appendLine("${indent}// Event Trigger: ${block.opType.title} -> $a1")
                }
                BlockOpType.IF_CONDITION -> {
                    sb.appendLine("${indent}if (${a1.ifBlank { "true" }}) {")
                    indent = "$baseIndent    "
                }
                BlockOpType.ELSE_BRANCH -> {
                    indent = baseIndent
                    sb.appendLine("${indent}} else {")
                    indent = "$baseIndent    "
                }
                BlockOpType.REPEAT_LOOP -> {
                    val count = a1.toIntOrNull()?.coerceIn(1, 1000) ?: 3
                    val iterName = a2.replace(Regex("[^A-Za-z0-9_]"), "").ifBlank { "idx" }
                    sb.appendLine("${indent}for ($iterName in 0 until $count) {")
                    sb.appendLine("${indent}    stateMap[\"$iterName\"] = $iterName")
                    sb.appendLine("${indent}}")
                }
                BlockOpType.SWITCH_CASE -> {
                    sb.appendLine("${indent}when (stateMap[\"$a1\"]) {")
                    sb.appendLine("${indent}    else -> { /* $a2 */ }")
                    sb.appendLine("${indent}}")
                }
                BlockOpType.LOGIC_AND_OR -> {
                    sb.appendLine("${indent}val conditionResult = ($a1)")
                    sb.appendLine("${indent}stateMap[\"lastCondition\"] = conditionResult")
                }
                BlockOpType.TRY_CATCH -> {
                    sb.appendLine("${indent}try {")
                    sb.appendLine("${indent}    // Guarded operation: $a1")
                    sb.appendLine("${indent}} catch (e: Exception) {")
                    sb.appendLine("${indent}    withContext(Dispatchers.Main) {")
                    sb.appendLine("${indent}        Toast.makeText(context, \"${a2.ifBlank { "Error" }.replace("\"", "\\\"")}: \${e.localizedMessage}\", Toast.LENGTH_SHORT).show()")
                    sb.appendLine("${indent}    }")
                    sb.appendLine("${indent}}")
                }
                BlockOpType.CREATE_VAR, BlockOpType.SET_VAR -> {
                    sb.appendLine("${indent}stateMap[\"$a1\"] = \"$a2\"")
                }
                BlockOpType.LIST_OP -> {
                    sb.appendLine("${indent}// List/Map op on '$a1': $a2")
                }
                BlockOpType.LIST_FILTER -> {
                    sb.appendLine("${indent}// Filter/Sort list '$a1' using: $a2")
                }
                BlockOpType.MATH_CALC -> {
                    sb.appendLine("${indent}stateMap[\"$a1\"] = \"$a2\"")
                }
                BlockOpType.MATH_RANDOM -> {
                    sb.appendLine("${indent}stateMap[\"$a1\"] = (${a2.ifBlank { "1..100" }}).random()")
                }
                BlockOpType.TEXT_JOIN -> {
                    sb.appendLine("${indent}stateMap[\"$a1\"] = $a2")
                }
                BlockOpType.TEXT_SPLIT -> {
                    sb.appendLine("${indent}val tokens = (stateMap[\"$a1\"]?.toString() ?: \"\").$a2")
                    sb.appendLine("${indent}stateMap[\"${a1}_tokens\"] = tokens")
                }
                BlockOpType.UI_TOAST -> {
                    sb.appendLine("${indent}withContext(Dispatchers.Main) {")
                    sb.appendLine("${indent}    Toast.makeText(context, \"${a1.replace("\"", "\\\"")}\", Toast.LENGTH_SHORT).show()")
                    sb.appendLine("${indent}}")
                }
                BlockOpType.UI_SET_TEXT -> {
                    val formattedVal = if (a2.startsWith("\"") && a2.endsWith("\"")) a2 else "\"${a2.replace("\"", "\\\"")}\""
                    sb.appendLine("${indent}onUiUpdate(\"$a1\", $formattedVal)")
                }
                BlockOpType.UI_SET_COLOR_VISIBILITY -> {
                    sb.appendLine("${indent}onUiUpdate(\"$a1\", \"$a2\")")
                }
                BlockOpType.UI_NAVIGATE -> {
                    sb.appendLine("${indent}withContext(Dispatchers.Main) { onNavigate(\"$a1\") }")
                }
                BlockOpType.UI_CLOSE_SCREEN -> {
                    sb.appendLine("${indent}withContext(Dispatchers.Main) { onNavigate(\"__back__\") }")
                }
                BlockOpType.SHARED_PREFS_SAVE -> {
                    sb.appendLine("${indent}sharedPrefs.edit().putString(\"$a1\", \"${a2.replace("\"", "\\\"")}\").apply()")
                    sb.appendLine("${indent}stateMap[\"$a1\"] = \"${a2.replace("\"", "\\\"")}\"")
                }
                BlockOpType.SHARED_PREFS_GET -> {
                    sb.appendLine("${indent}stateMap[\"$a1\"] = sharedPrefs.getString(\"$a1\", \"${a2.replace("\"", "\\\"")}\") ?: \"\"")
                }
                BlockOpType.DIALOG_SHOW -> {
                    sb.appendLine("${indent}withContext(Dispatchers.Main) {")
                    sb.appendLine("${indent}    AlertDialog.Builder(context)")
                    sb.appendLine("${indent}        .setTitle(\"${a1.replace("\"", "\\\"")}\")")
                    sb.appendLine("${indent}        .setMessage(\"${a2.replace("\"", "\\\"")}\")")
                    sb.appendLine("${indent}        .setPositiveButton(\"OK\", null)")
                    sb.appendLine("${indent}        .setNegativeButton(\"Cancel\", null)")
                    sb.appendLine("${indent}        .show()")
                    sb.appendLine("${indent}}")
                }
                BlockOpType.TIMER_SCHEDULE -> {
                    val delayMs = a1.toLongOrNull()?.coerceIn(100L, 60_000L) ?: 1500L
                    sb.appendLine("${indent}mainHandler.postDelayed({ /* $a2 */ }, ${delayMs}L)")
                }
                BlockOpType.TTS_SPEAK -> {
                    sb.appendLine("${indent}withContext(Dispatchers.Main) {")
                    sb.appendLine("${indent}    tts = tts ?: TextToSpeech(context) { status ->")
                    sb.appendLine("${indent}        if (status == TextToSpeech.SUCCESS) {")
                    sb.appendLine("${indent}            tts?.language = Locale.getDefault()")
                    sb.appendLine("${indent}            tts?.speak(\"${a1.replace("\"", "\\\"")}\", TextToSpeech.QUEUE_FLUSH, null, \"bcf_tts\")")
                    sb.appendLine("${indent}        }")
                    sb.appendLine("${indent}    }")
                    sb.appendLine("${indent}}")
                }
                BlockOpType.ANDROID_SHARE -> {
                    sb.appendLine("${indent}val intent = Intent(Intent.ACTION_VIEW, Uri.parse(\"$a1\"))")
                    sb.appendLine("${indent}context.startActivity(intent)")
                }
                BlockOpType.ANDROID_PERMISSION -> {
                    sb.appendLine("${indent}// Runtime permission check required for android.permission.$a1")
                }
                BlockOpType.ANDROID_NOTIFICATION -> {
                    sb.appendLine("${indent}// Dispatch notification: title='$a1', body='$a2'")
                }
                BlockOpType.ANDROID_VIBRATE_CLIPBOARD -> {
                    sb.appendLine("${indent}// Clipboard / Vibrator action for '$a1'")
                }
                BlockOpType.ANDROID_PICK_MEDIA -> {
                    sb.appendLine("${indent}// Launch zero-permission PickVisualMedia contract ($a1)")
                }
                BlockOpType.HTTP_GET -> {
                    sb.appendLine("${indent}val getResponse = withContext(Dispatchers.IO) {")
                    sb.appendLine("${indent}    val req = Request.Builder().url(\"$a1\").get().build()")
                    sb.appendLine("${indent}    httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }")
                    sb.appendLine("${indent}}")
                    sb.appendLine("${indent}stateMap[\"lastHttpResponse\"] = getResponse")
                }
                BlockOpType.HTTP_POST -> {
                    sb.appendLine("${indent}val postResponse = withContext(Dispatchers.IO) {")
                    sb.appendLine("${indent}    val body = \"${a2.replace("\"", "\\\"")}\".toRequestBody(\"application/json\".toMediaType())")
                    sb.appendLine("${indent}    val req = Request.Builder().url(\"$a1\").post(body).build()")
                    sb.appendLine("${indent}    httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }")
                    sb.appendLine("${indent}}")
                    sb.appendLine("${indent}stateMap[\"lastHttpResponse\"] = postResponse")
                }
                BlockOpType.JSON_PARSE -> {
                    sb.appendLine("${indent}// Parse JSON key '$a2' from stateMap[\"$a1\"]")
                }
                BlockOpType.FB_AUTH_CHECK -> {
                    sb.appendLine("${indent}val uid = Firebase.auth.currentUser?.uid ?: error(\"Sign-in required\")")
                    sb.appendLine("${indent}stateMap[\"uid\"] = uid")
                }
                BlockOpType.FB_FIRESTORE_WRITE -> {
                    sb.appendLine("${indent}val currentUid = Firebase.auth.currentUser?.uid")
                    sb.appendLine("${indent}if (db != null && currentUid != null) {")
                    sb.appendLine("${indent}    db.collection(\"users\").document(currentUid).collection(\"$a1\")")
                    sb.appendLine("${indent}        .add(mapOf(\"payload\" to \"$a2\", \"createdAt\" to FieldValue.serverTimestamp()))")
                    sb.appendLine("${indent}        .await()")
                    sb.appendLine("${indent}}")
                }
                BlockOpType.FB_STORAGE_UPLOAD -> {
                    sb.appendLine("${indent}// Upload selected asset to Firebase Storage path: $a1")
                }
                BlockOpType.FB_FCM_TOPIC -> {
                    sb.appendLine("${indent}// Subscribe Firebase Cloud Messaging topic: $a1")
                }
                BlockOpType.CALL_MORE_BLOCK -> {
                    val fnName = a1.replace(Regex("[^A-Za-z0-9_]"), "").ifBlank { "customAction" }
                    sb.appendLine("${indent}$fnName(${if (a2.isNotBlank()) "\"${a2.replace("\"", "\\\"")}\"" else ""}, onNavigate = onNavigate, onUiUpdate = onUiUpdate)")
                }
            }
        }
        if (indent.length > baseIndent.length) {
            sb.appendLine("${baseIndent}}")
        }
    }
}
