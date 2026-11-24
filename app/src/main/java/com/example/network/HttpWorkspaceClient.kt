package com.example.network

import com.example.model.ApiExecutionResult
import com.example.model.ApiRequestSpec
import com.example.model.BuildJobRecord
import com.example.model.BuildTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class HttpWorkspaceClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun executeApiRequest(spec: ApiRequestSpec): ApiExecutionResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        try {
            val baseUrl = spec.url.trim()
            val parsedUrl = baseUrl.toHttpUrlOrNull()
                ?: return@withContext ApiExecutionResult(
                    statusCode = 0,
                    latencyMs = 0,
                    headersSummary = "",
                    responseBody = "",
                    isSuccess = false,
                    errorMessage = "Invalid URL: '$baseUrl'. Include https:// or http://"
                )

            val urlBuilder = parsedUrl.newBuilder()
            spec.queryParamsText.lines()
                .map { it.trim() }
                .filter { it.contains("=") }
                .forEach { line ->
                    val k = line.substringBefore("=").trim()
                    val v = line.substringAfter("=").trim()
                    if (k.isNotBlank()) urlBuilder.addQueryParameter(k, v)
                }

            val reqBuilder = Request.Builder().url(urlBuilder.build())

            spec.headersText.lines()
                .map { it.trim() }
                .filter { it.contains(":") }
                .forEach { line ->
                    val name = line.substringBefore(":").trim()
                    val value = line.substringAfter(":").trim()
                    if (name.isNotBlank()) reqBuilder.addHeader(name, value)
                }

            when (spec.authType) {
                "BEARER" -> if (spec.authValue.isNotBlank()) {
                    reqBuilder.header("Authorization", "Bearer ${spec.authValue.trim()}")
                }
                "API_KEY" -> if (spec.authValue.isNotBlank()) {
                    reqBuilder.header("X-API-Key", spec.authValue.trim())
                }
            }

            val method = spec.method.uppercase().ifBlank { "GET" }
            val body = if (method in listOf("POST", "PUT", "PATCH")) {
                spec.bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            } else {
                null
            }
            reqBuilder.method(method, body)

            client.newCall(reqBuilder.build()).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startMs
                val rawBody = response.body?.string().orEmpty()
                val headersText = response.headers.joinToString("\n") { "${it.first}: ${it.second}" }
                ApiExecutionResult(
                    statusCode = response.code,
                    latencyMs = elapsed,
                    headersSummary = headersText,
                    responseBody = rawBody.take(16_000),
                    isSuccess = response.isSuccessful
                )
            }
        } catch (e: Exception) {
            ApiExecutionResult(
                statusCode = 0,
                latencyMs = System.currentTimeMillis() - startMs,
                headersSummary = "",
                responseBody = "",
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "HTTP request failed"
            )
        }
    }

    suspend fun pingBuildWorker(workerBaseUrl: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = workerBaseUrl.trim().trimEnd('/')
            if (cleanBase.isBlank()) return@withContext Result.failure(IllegalArgumentException("Build Worker URL is not configured."))
            val reqBuilder = Request.Builder()
                .url("$cleanBase/api/health")
                .get()
            if (token.isNotBlank()) reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            client.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Result.failure(IllegalStateException("Worker returned HTTP ${resp.code}: $body"))
                } else {
                    Result.success(body.ifBlank { "Connected (HTTP ${resp.code})" })
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun triggerRemoteBuild(
        workerBaseUrl: String,
        token: String,
        projectName: String,
        packageName: String,
        target: BuildTarget,
        filesPayloadJson: String
    ): Result<BuildJobRecord> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = workerBaseUrl.trim().trimEnd('/')
            if (cleanBase.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("No BCF Build Worker URL configured. Configure your isolated Gradle worker in Build Center or Settings.")
                )
            }
            val bodyJson = JSONObject().apply {
                put("projectName", projectName)
                put("packageName", packageName)
                put("target", target.name)
                put("gradleTask", target.gradleTask)
                put("projectJson", filesPayloadJson)
            }.toString()

            val reqBuilder = Request.Builder()
                .url("$cleanBase/api/builds")
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            if (token.isNotBlank()) reqBuilder.header("Authorization", "Bearer ${token.trim()}")

            client.newCall(reqBuilder.build()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Build Worker HTTP ${resp.code}: $raw"))
                }
                val obj = JSONObject(raw)
                Result.success(
                    BuildJobRecord(
                        buildId = obj.optString("buildId", "bld-${System.currentTimeMillis()}"),
                        target = target,
                        status = obj.optString("status", "QUEUED"),
                        createdAtLabel = "Just now",
                        logs = obj.optString("logs", "Build job queued on isolated Gradle worker..."),
                        artifactUrl = obj.optString("artifactUrl", "")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollBuildStatus(workerBaseUrl: String, token: String, buildId: String, target: BuildTarget): Result<BuildJobRecord> =
        withContext(Dispatchers.IO) {
            try {
                val cleanBase = workerBaseUrl.trim().trimEnd('/')
                val reqBuilder = Request.Builder()
                    .url("$cleanBase/api/builds/$buildId")
                    .get()
                if (token.isNotBlank()) reqBuilder.header("Authorization", "Bearer ${token.trim()}")
                client.newCall(reqBuilder.build()).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("HTTP ${resp.code}: $raw"))
                    }
                    val obj = JSONObject(raw)
                    Result.success(
                        BuildJobRecord(
                            buildId = obj.optString("buildId", buildId),
                            target = target,
                            status = obj.optString("status", "RUNNING"),
                            createdAtLabel = "Updated",
                            logs = obj.optString("logs", ""),
                            artifactUrl = obj.optString("artifactUrl", "")
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun downloadBuildArtifact(
        workerBaseUrl: String,
        token: String,
        artifactPathOrUrl: String,
        outputStream: java.io.OutputStream
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = workerBaseUrl.trim().trimEnd('/')
            val fullUrl = if (artifactPathOrUrl.startsWith("http")) artifactPathOrUrl
            else "$cleanBase/${artifactPathOrUrl.trimStart('/')}"
            val reqBuilder = Request.Builder().url(fullUrl).get()
            if (token.isNotBlank()) reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Artifact download failed HTTP ${resp.code}"))
                }
                val bytes = resp.body?.byteStream()?.use { input ->
                    outputStream.use { out -> input.copyTo(out) }
                } ?: 0L
                Result.success(bytes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
