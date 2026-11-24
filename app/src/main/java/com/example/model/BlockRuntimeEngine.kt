package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class LogcatEntry(
    val timestamp: String,
    val level: String, // INFO, DEBUG, WARN
    val tag: String,
    val message: String
) {
    fun formatLine(): String = "$timestamp [$level/$tag] $message"
}

data class BlockExecutionResult(
    val variables: Map<String, String>,
    val sharedPrefs: Map<String, String>,
    val dynamicTexts: Map<String, String>,
    val hiddenComponentIds: Set<String>,
    val toastMessage: String?,
    val dialogTitle: String?,
    val dialogMessage: String?,
    val navigateToRoute: String?,
    val navigateBack: Boolean,
    val logs: List<LogcatEntry>
)

object BlockRuntimeEngine {

    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private fun nowStamp(): String = timeFmt.format(Date())

    fun executeBlocks(
        project: AppProject,
        screen: AppScreen,
        blocksToRun: List<LogicBlock>,
        currentVariables: Map<String, String>,
        currentSharedPrefs: Map<String, String>,
        currentDynamicTexts: Map<String, String>,
        currentHiddenIds: Set<String>,
        triggerLabel: String
    ): BlockExecutionResult {
        val vars = currentVariables.toMutableMap()
        val prefs = currentSharedPrefs.toMutableMap()
        val texts = currentDynamicTexts.toMutableMap()
        val hidden = currentHiddenIds.toMutableSet()
        val logs = mutableListOf<LogcatEntry>()

        var toastMessage: String? = null
        var dialogTitle: String? = null
        var dialogMessage: String? = null
        var navigateRoute: String? = null
        var navigateBack = false

        logs += LogcatEntry(nowStamp(), "INFO", screen.route, "Trigger '$triggerLabel' (${blocksToRun.count { it.enabled }} active block(s))")

        var skipNextIfFalse = false
        var insideElse = false

        fun runList(list: List<LogicBlock>, depth: Int = 0) {
            if (depth > 4) {
                logs += LogcatEntry(nowStamp(), "WARN", "Runtime", "Stopped recursion at depth 4")
                return
            }
            list.filter { it.enabled }.forEach { block ->
                val raw1 = block.arg1.ifBlank { block.opType.defaultArg1 }
                val raw2 = block.arg2.ifBlank { block.opType.defaultArg2 }
                val a1 = interpolateVars(raw1, vars, prefs)
                val a2 = interpolateVars(raw2, vars, prefs)

                if (block.opType == BlockOpType.ELSE_BRANCH) {
                    insideElse = !skipNextIfFalse
                    return@forEach
                }
                if (skipNextIfFalse && !insideElse) {
                    logs += LogcatEntry(nowStamp(), "DEBUG", "Flow", "Skipped ${block.opType.title} (condition false)")
                    skipNextIfFalse = false
                    return@forEach
                }
                insideElse = false

                when (block.opType) {
                    BlockOpType.ON_APP_START,
                    BlockOpType.ON_SCREEN_CREATE,
                    BlockOpType.ON_BUTTON_CLICK,
                    BlockOpType.ON_TEXT_CHANGE,
                    BlockOpType.ON_TIMER,
                    BlockOpType.ON_NETWORK_RESPONSE,
                    BlockOpType.ON_FIREBASE_EVENT -> {
                        logs += LogcatEntry(nowStamp(), "INFO", "Event", "${block.opType.title}: $a1")
                    }

                    BlockOpType.IF_CONDITION -> {
                        val eval = evaluateCondition(a1, vars)
                        skipNextIfFalse = !eval
                        logs += LogcatEntry(nowStamp(), "DEBUG", "If", "($a1) => $eval")
                    }

                    BlockOpType.ELSE_BRANCH -> Unit

                    BlockOpType.REPEAT_LOOP -> {
                        val count = a1.toIntOrNull()?.coerceIn(1, 20) ?: 3
                        val iterVar = raw2.ifBlank { "idx" }
                        for (i in 0 until count) {
                            vars[iterVar] = i.toString()
                        }
                        logs += LogcatEntry(nowStamp(), "DEBUG", "Loop", "Repeated $count times (var '$iterVar')")
                    }

                    BlockOpType.SWITCH_CASE -> {
                        val cur = vars[raw1] ?: ""
                        logs += LogcatEntry(nowStamp(), "DEBUG", "Switch", "$raw1='$cur' checked against $a2")
                    }

                    BlockOpType.LOGIC_AND_OR -> {
                        val eval = evaluateCondition(a1, vars)
                        vars["lastCondition"] = eval.toString()
                        logs += LogcatEntry(nowStamp(), "DEBUG", "Logic", "$a1 => $eval")
                    }

                    BlockOpType.TRY_CATCH -> {
                        logs += LogcatEntry(nowStamp(), "INFO", "TryCatch", "Guarded execution of '$a1'")
                    }

                    BlockOpType.CREATE_VAR, BlockOpType.SET_VAR -> {
                        val computed = evaluateExpressionOrLiteral(a2, vars)
                        vars[raw1] = computed
                        logs += LogcatEntry(nowStamp(), "INFO", "Var", "$raw1 = $computed")
                    }

                    BlockOpType.LIST_OP -> {
                        val existing = vars[raw1].orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                        val itemMatch = Regex("\\(([^)]*)\\)").find(a2)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                        if (a2.startsWith("add", ignoreCase = true) && itemMatch.isNotBlank()) {
                            existing += itemMatch
                        } else if (a2.startsWith("clear", ignoreCase = true)) {
                            existing.clear()
                        } else if (a2.isNotBlank()) {
                            existing += a2
                        }
                        vars[raw1] = existing.joinToString(", ")
                        logs += LogcatEntry(nowStamp(), "INFO", "List", "$raw1 => [${vars[raw1]}]")
                    }

                    BlockOpType.LIST_FILTER -> {
                        val existing = vars[raw1].orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val sorted = if (a2.contains("desc", ignoreCase = true)) existing.sortedDescending() else existing.sorted()
                        vars[raw1] = sorted.joinToString(", ")
                        logs += LogcatEntry(nowStamp(), "INFO", "ListFilter", "$raw1 => [${vars[raw1]}]")
                    }

                    BlockOpType.MATH_CALC -> {
                        val value = evalMath(a2, vars)
                        val formatted = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
                        vars[raw1] = formatted
                        logs += LogcatEntry(nowStamp(), "INFO", "Math", "$raw1 = $a2 => $formatted")
                    }

                    BlockOpType.MATH_RANDOM -> {
                        val rangeParts = a2.split("..").mapNotNull { it.trim().toIntOrNull() }
                        val rnd = if (rangeParts.size == 2 && rangeParts[0] <= rangeParts[1]) {
                            Random.nextInt(rangeParts[0], rangeParts[1] + 1)
                        } else {
                            Random.nextInt(1, 101)
                        }
                        vars[raw1] = rnd.toString()
                        logs += LogcatEntry(nowStamp(), "INFO", "Random", "$raw1 = $rnd")
                    }

                    BlockOpType.TEXT_JOIN -> {
                        val clean = a2.trim().removeSurrounding("\"")
                        vars[raw1] = clean
                        logs += LogcatEntry(nowStamp(), "INFO", "TextJoin", "$raw1 = \"$clean\"")
                    }

                    BlockOpType.TEXT_SPLIT -> {
                        val src = vars[raw1] ?: ""
                        val parts = src.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        vars["${raw1}_count"] = parts.size.toString()
                        vars["${raw1}_first"] = parts.firstOrNull().orEmpty()
                        logs += LogcatEntry(nowStamp(), "INFO", "TextSplit", "$raw1 split into ${parts.size} item(s)")
                    }

                    BlockOpType.UI_TOAST -> {
                        toastMessage = a1.trim().removeSurrounding("\"")
                        logs += LogcatEntry(nowStamp(), "INFO", "Toast", toastMessage.orEmpty())
                    }

                    BlockOpType.UI_SET_TEXT -> {
                        val resolvedComponent = findTargetComponent(screen, raw1)
                        val newText = a2.trim().removeSurrounding("\"")
                        if (resolvedComponent != null) {
                            texts[resolvedComponent.componentId] = newText
                            logs += LogcatEntry(nowStamp(), "INFO", "View", "Set '${resolvedComponent.text}' text -> \"$newText\"")
                        } else {
                            texts[raw1] = newText
                            logs += LogcatEntry(nowStamp(), "WARN", "View", "Target '$raw1' set in text cache -> \"$newText\"")
                        }
                    }

                    BlockOpType.UI_SET_COLOR_VISIBILITY -> {
                        val resolvedComponent = findTargetComponent(screen, raw1)
                        val targetId = resolvedComponent?.componentId ?: raw1
                        val hide = a2.contains("false", ignoreCase = true) || a2.contains("gone", ignoreCase = true) || a2.contains("hide", ignoreCase = true)
                        if (hide) hidden.add(targetId) else hidden.remove(targetId)
                        logs += LogcatEntry(nowStamp(), "INFO", "Visibility", "Widget '$raw1' visible=${!hide}")
                    }

                    BlockOpType.UI_NAVIGATE -> {
                        navigateRoute = a1.trim()
                        logs += LogcatEntry(nowStamp(), "INFO", "Nav", "Navigate -> '$navigateRoute'")
                    }

                    BlockOpType.UI_CLOSE_SCREEN -> {
                        navigateBack = true
                        logs += LogcatEntry(nowStamp(), "INFO", "Nav", "Finish / Close current screen")
                    }

                    BlockOpType.SHARED_PREFS_SAVE -> {
                        val cleanVal = a2.trim().removeSurrounding("\"")
                        prefs[raw1] = cleanVal
                        vars[raw1] = cleanVal
                        logs += LogcatEntry(nowStamp(), "INFO", "SharedPrefs", "Saved [$raw1] = '$cleanVal'")
                    }

                    BlockOpType.SHARED_PREFS_GET -> {
                        val fallback = a2.trim().removeSurrounding("\"")
                        val loaded = prefs[raw1] ?: fallback
                        vars[raw1] = loaded
                        logs += LogcatEntry(nowStamp(), "INFO", "SharedPrefs", "Loaded [$raw1] => '$loaded'")
                    }

                    BlockOpType.DIALOG_SHOW -> {
                        dialogTitle = a1.trim().removeSurrounding("\"")
                        dialogMessage = a2.trim().removeSurrounding("\"")
                        logs += LogcatEntry(nowStamp(), "INFO", "AlertDialog", "Opened dialog '$dialogTitle'")
                    }

                    BlockOpType.TIMER_SCHEDULE -> {
                        logs += LogcatEntry(nowStamp(), "INFO", "Timer", "Scheduled '$a2' after ${a1}ms")
                    }

                    BlockOpType.TTS_SPEAK -> {
                        toastMessage = "TTS Speaking: $a1"
                        logs += LogcatEntry(nowStamp(), "INFO", "TTS", "Speaking: \"$a1\"")
                    }

                    BlockOpType.ANDROID_SHARE -> {
                        toastMessage = "Share Intent -> $a1"
                        logs += LogcatEntry(nowStamp(), "INFO", "Intent", "ACTION_VIEW / SHARE -> $a1")
                    }

                    BlockOpType.ANDROID_PERMISSION -> {
                        logs += LogcatEntry(nowStamp(), "INFO", "Permission", "Runtime check requested: android.permission.$a1")
                    }

                    BlockOpType.ANDROID_NOTIFICATION -> {
                        toastMessage = "Notification: $a1 — $a2"
                        logs += LogcatEntry(nowStamp(), "INFO", "Notification", "$a1: $a2")
                    }

                    BlockOpType.ANDROID_VIBRATE_CLIPBOARD -> {
                        toastMessage = "Clipboard / Haptic: $a1"
                        logs += LogcatEntry(nowStamp(), "INFO", "Device", "Vibrate / Clipboard '$a1'")
                    }

                    BlockOpType.ANDROID_PICK_MEDIA -> {
                        toastMessage = "Photo Picker contract ($a1)"
                        logs += LogcatEntry(nowStamp(), "INFO", "MediaPicker", "Launched zero-permission PickVisualMedia")
                    }

                    BlockOpType.HTTP_GET -> {
                        vars["lastHttpResponse"] = "{\"ok\":true,\"url\":\"$a1\",\"status\":200}"
                        logs += LogcatEntry(nowStamp(), "INFO", "Network", "HTTP GET $a1 -> 200 OK (simulated in sandbox)")
                    }

                    BlockOpType.HTTP_POST -> {
                        vars["lastHttpResponse"] = "{\"ok\":true,\"posted\":true}"
                        logs += LogcatEntry(nowStamp(), "INFO", "Network", "HTTP POST $a1 body=$a2 -> 200 OK")
                    }

                    BlockOpType.JSON_PARSE -> {
                        vars[a2.ifBlank { "parsedField" }] = "parsed_value"
                        logs += LogcatEntry(nowStamp(), "DEBUG", "JSON", "Extracted '$a2' from '$a1'")
                    }

                    BlockOpType.FB_AUTH_CHECK -> {
                        vars["uid"] = project.userId.ifBlank { "preview_uid" }
                        logs += LogcatEntry(nowStamp(), "INFO", "FirebaseAuth", "Verified session uid=${vars["uid"]}")
                    }

                    BlockOpType.FB_FIRESTORE_WRITE -> {
                        logs += LogcatEntry(nowStamp(), "INFO", "Firestore", "Queued write to collection '$a1' payload='$a2'")
                    }

                    BlockOpType.FB_STORAGE_UPLOAD -> {
                        logs += LogcatEntry(nowStamp(), "INFO", "Storage", "Queued upload -> $a1")
                    }

                    BlockOpType.FB_FCM_TOPIC -> {
                        logs += LogcatEntry(nowStamp(), "INFO", "FCM", "Subscribed to topic '$a1'")
                    }

                    BlockOpType.CALL_MORE_BLOCK -> {
                        val mbName = raw1.trim()
                        val mb = project.backend.customMoreBlocks.firstOrNull {
                            it.name.equals(mbName, ignoreCase = true) || it.blockId == mbName
                        }
                        if (mb != null) {
                            vars["param"] = a2
                            val mbBody = screen.logicBlocks.filter {
                                it.targetWidgetId == "moreblock:${mb.name}" || it.targetWidgetId == mb.blockId
                            }
                            logs += LogcatEntry(nowStamp(), "INFO", "MoreBlock", "Calling ${mb.name}($a2) [${mbBody.size} block(s)]")
                            runList(mbBody, depth + 1)
                        } else {
                            logs += LogcatEntry(nowStamp(), "WARN", "MoreBlock", "MoreBlock '$mbName' not found")
                        }
                    }
                }
            }
        }

        runList(blocksToRun)

        return BlockExecutionResult(
            variables = vars,
            sharedPrefs = prefs,
            dynamicTexts = texts,
            hiddenComponentIds = hidden,
            toastMessage = toastMessage,
            dialogTitle = dialogTitle,
            dialogMessage = dialogMessage,
            navigateToRoute = navigateRoute,
            navigateBack = navigateBack,
            logs = logs
        )
    }

    private fun findTargetComponent(screen: AppScreen, ref: String): AppComponent? {
        val clean = ref.trim()
        return screen.components.firstOrNull {
            it.componentId == clean ||
                it.text.equals(clean, ignoreCase = true) ||
                it.type.name.equals(clean, ignoreCase = true) ||
                it.type.displayName.equals(clean, ignoreCase = true)
        }
    }

    private fun interpolateVars(
        input: String,
        vars: Map<String, String>,
        prefs: Map<String, String>
    ): String {
        var out = input
        vars.forEach { (k, v) ->
            if (k.isNotBlank()) {
                out = out.replace("\$$k", v).replace("\${$k}", v)
            }
        }
        prefs.forEach { (k, v) ->
            if (k.isNotBlank()) {
                out = out.replace("\$pref_$k", v)
            }
        }
        return out
    }

    private fun evaluateExpressionOrLiteral(expr: String, vars: Map<String, String>): String {
        val trimmed = expr.trim().removeSurrounding("\"")
        val hasOperator = listOf("+", "-", "*", "/").any { op ->
            trimmed.contains(" $op ") || Regex("[0-9A-Za-z_]$op[0-9A-Za-z_]").containsMatchIn(trimmed)
        }
        return if (hasOperator && !trimmed.contains("http")) {
            val res = evalMath(trimmed, vars)
            if (res.isNaN()) trimmed
            else if (res % 1.0 == 0.0) res.toLong().toString()
            else String.format(Locale.US, "%.2f", res)
        } else {
            vars[trimmed] ?: trimmed
        }
    }

    private fun evaluateCondition(expr: String, vars: Map<String, String>): Boolean {
        val clean = expr.trim()
        if (clean.equals("true", ignoreCase = true)) return true
        if (clean.equals("false", ignoreCase = true)) return false
        val operators = listOf("==", "!=", ">=", "<=", ">", "<")
        for (op in operators) {
            val parts = clean.split(op, limit = 2)
            if (parts.size == 2) {
                val leftRaw = parts[0].trim().removeSurrounding("\"")
                val rightRaw = parts[1].trim().removeSurrounding("\"")
                val leftVal = vars[leftRaw] ?: leftRaw
                val rightVal = vars[rightRaw] ?: rightRaw
                val leftNum = leftVal.toDoubleOrNull()
                val rightNum = rightVal.toDoubleOrNull()
                return when (op) {
                    "==" -> leftVal.equals(rightVal, ignoreCase = true)
                    "!=" -> !leftVal.equals(rightVal, ignoreCase = true)
                    ">" -> (leftNum ?: 0.0) > (rightNum ?: 0.0)
                    "<" -> (leftNum ?: 0.0) < (rightNum ?: 0.0)
                    ">=" -> (leftNum ?: 0.0) >= (rightNum ?: 0.0)
                    "<=" -> (leftNum ?: 0.0) <= (rightNum ?: 0.0)
                    else -> false
                }
            }
        }
        val varVal = vars[clean]
        return !varVal.isNullOrBlank() && !varVal.equals("false", ignoreCase = true) && varVal != "0"
    }

    private fun evalMath(rawExpr: String, vars: Map<String, String>): Double {
        var substituted = rawExpr.trim()
        vars.entries.sortedByDescending { it.key.length }.forEach { (k, v) ->
            if (k.isNotBlank() && v.toDoubleOrNull() != null) {
                substituted = substituted.replace(Regex("\\b${Regex.escape(k)}\\b"), v)
            }
        }
        return try {
            object {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < substituted.length) substituted[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    return if (pos < substituted.length) Double.NaN else x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        x = when {
                            eat('+'.code) -> x + parseTerm()
                            eat('-'.code) -> x - parseTerm()
                            else -> return x
                        }
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        x = when {
                            eat('*'.code) -> x * parseFactor()
                            eat('/'.code) -> {
                                val denom = parseFactor()
                                if (denom == 0.0) 0.0 else x / denom
                            }
                            else -> return x
                        }
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return -parseFactor()
                    val startPos = pos
                    val x: Double = if (eat('('.code)) {
                        val inner = parseExpression()
                        eat(')'.code)
                        inner
                    } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                        while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                        substituted.substring(startPos, pos).toDoubleOrNull() ?: 0.0
                    } else {
                        0.0
                    }
                    return x
                }
            }.parse()
        } catch (_: Exception) {
            Double.NaN
        }
    }
}
