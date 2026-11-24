package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ApiExecutionResult
import com.example.model.ApiRequestSpec
import com.example.model.AppProject
import com.example.model.ScalingTier
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun BackendApiStudioScreen(
    project: AppProject?,
    initialTab: Int = 0, // 0 = Backend Builder, 1 = API Manager
    apiResult: ApiExecutionResult?,
    apiRunning: Boolean,
    onUpdateCloudInfra: (String, ScalingTier) -> Unit,
    onUpdateSecurityToggles: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onAddEndpoint: (String, String, String, String) -> Unit,
    onGenerateNodeBackend: () -> Unit,
    onSyncAndroidGradle: () -> Unit,
    onSaveApiRequest: (ApiRequestSpec) -> Unit,
    onExecuteApiRequest: (ApiRequestSpec) -> Unit,
    onGenerateKotlinClient: (ApiRequestSpec) -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select or create a project to manage Backend & APIs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Node.js TS Backend") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("REST API Studio") })
        }

        if (activeTab == 0) {
            BackendBuilderTab(
                project = project,
                onUpdateCloudInfra = onUpdateCloudInfra,
                onUpdateSecurityToggles = onUpdateSecurityToggles,
                onAddEndpoint = onAddEndpoint,
                onGenerateNodeBackend = onGenerateNodeBackend,
                onSyncAndroidGradle = onSyncAndroidGradle
            )
        } else {
            ApiManagerTab(
                project = project,
                apiResult = apiResult,
                apiRunning = apiRunning,
                onSaveApiRequest = onSaveApiRequest,
                onExecuteApiRequest = onExecuteApiRequest,
                onGenerateKotlinClient = onGenerateKotlinClient
            )
        }
    }
}

@Composable
private fun BackendBuilderTab(
    project: AppProject,
    onUpdateCloudInfra: (String, ScalingTier) -> Unit,
    onUpdateSecurityToggles: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onAddEndpoint: (String, String, String, String) -> Unit,
    onGenerateNodeBackend: () -> Unit,
    onSyncAndroidGradle: () -> Unit
) {
    var showAddEndpoint by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf("POST") }
    var path by remember { mutableStateOf("/api/v1/orders") }
    var description by remember { mutableStateOf("Authenticated CRUD route") }
    val regions = remember { listOf("asia-southeast1", "us-central1", "europe-west1", "asia-south1") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Dns, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Backend & Android Project Codegen", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Generate real Node.js + TypeScript Express routes, Dockerfiles, and Android Studio Gradle/Manifest files directly inside your project.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onGenerateNodeBackend,
                            modifier = Modifier.weight(1f).testTag("generate_node_backend_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Node.js API")
                        }
                        OutlinedButton(
                            onClick = onSyncAndroidGradle,
                            modifier = Modifier.weight(1f).testTag("sync_gradle_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Sync, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Gradle & Manifest")
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Middleware & Auth Controls", style = MaterialTheme.typography.titleMedium)
                    }
                    BackendToggle("JWT Bearer Authentication", project.backend.jwtEnabled) {
                        onUpdateSecurityToggles(it, project.backend.rateLimitEnabled, project.backend.authGoogle, project.backend.authEmail, project.backend.appCheckEnabled)
                    }
                    BackendToggle("Rate Limiting & CORS Guard", project.backend.rateLimitEnabled) {
                        onUpdateSecurityToggles(project.backend.jwtEnabled, it, project.backend.authGoogle, project.backend.authEmail, project.backend.appCheckEnabled)
                    }
                    BackendToggle("Google Sign-In (Credential Manager)", project.backend.authGoogle) {
                        onUpdateSecurityToggles(project.backend.jwtEnabled, project.backend.rateLimitEnabled, it, project.backend.authEmail, project.backend.appCheckEnabled)
                    }
                    BackendToggle("Firebase App Check Enforcement", project.backend.appCheckEnabled) {
                        onUpdateSecurityToggles(project.backend.jwtEnabled, project.backend.rateLimitEnabled, project.backend.authGoogle, project.backend.authEmail, it)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cloud Region & Tier", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        regions.forEach { region ->
                            FilterChip(
                                selected = project.cloudRegion == region,
                                onClick = { onUpdateCloudInfra(region, project.scalingTier) },
                                label = { Text(region) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScalingTier.entries.forEach { tier ->
                            FilterChip(
                                selected = project.scalingTier == tier,
                                onClick = { onUpdateCloudInfra(project.cloudRegion, tier) },
                                label = { Text(tier.displayName) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("REST API Routes (${project.backend.endpoints.size})", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showAddEndpoint = true }, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Route")
                }
            }
        }

        items(project.backend.endpoints, key = { "${it.method}_${it.path}" }) { ep ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${ep.method}  ${ep.path}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("${ep.runtime} • Auth=${ep.authRequired} • ${ep.rateLimitRpm} rpm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    if (ep.description.isNotBlank()) {
                        Text(ep.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showAddEndpoint) {
        AlertDialog(
            onDismissRequest = { showAddEndpoint = false },
            title = { Text("Add REST Endpoint") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("GET", "POST", "PUT", "DELETE").forEach { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                        }
                    }
                    OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("Path") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddEndpoint(method, path, "Node.js TypeScript • Express", description)
                    showAddEndpoint = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEndpoint = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ApiManagerTab(
    project: AppProject,
    apiResult: ApiExecutionResult?,
    apiRunning: Boolean,
    onSaveApiRequest: (ApiRequestSpec) -> Unit,
    onExecuteApiRequest: (ApiRequestSpec) -> Unit,
    onGenerateKotlinClient: (ApiRequestSpec) -> Unit
) {
    val firstSaved = project.backend.apiRequests.firstOrNull()
    var reqName by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.name ?: "Test JSON Endpoint") }
    var method by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.method ?: "GET") }
    var url by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.url ?: "https://httpbin.org/json") }
    var headersText by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.headersText ?: "Accept: application/json") }
    var queryText by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.queryParamsText ?: "") }
    var authType by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.authType ?: "NONE") }
    var authValue by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.authValue ?: "") }
    var bodyJson by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.bodyJson ?: "{\n  \"hello\": \"bcf_builder\"\n}") }
    var activeRequestId by remember(firstSaved?.requestId) { mutableStateOf(firstSaved?.requestId ?: "") }

    val currentSpec = ApiRequestSpec(
        requestId = activeRequestId,
        name = reqName,
        method = method,
        url = url,
        headersText = headersText,
        queryParamsText = queryText,
        bodyJson = bodyJson,
        authType = authType,
        authValue = authValue
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (project.backend.apiRequests.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    project.backend.apiRequests.forEach { saved ->
                        FilterChip(
                            selected = activeRequestId == saved.requestId,
                            onClick = {
                                activeRequestId = saved.requestId
                                reqName = saved.name
                                method = saved.method
                                url = saved.url
                                headersText = saved.headersText
                                queryText = saved.queryParamsText
                                authType = saved.authType
                                authValue = saved.authValue
                                bodyJson = saved.bodyJson
                            },
                            label = { Text("${saved.method} ${saved.name}") }
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Api, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Live OkHttp Request Runner", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedTextField(value = reqName, onValueChange = { reqName = it }, label = { Text("Request Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("GET", "POST", "PUT", "DELETE").forEach { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                        }
                    }

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Endpoint URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("api_url_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NONE", "BEARER", "API_KEY").forEach { t ->
                            FilterChip(selected = authType == t, onClick = { authType = t }, label = { Text(t) })
                        }
                    }

                    if (authType != "NONE") {
                        OutlinedTextField(
                            value = authValue,
                            onValueChange = { authValue = it },
                            label = { Text(if (authType == "BEARER") "Bearer Token" else "x-api-key Value") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(value = queryText, onValueChange = { queryText = it }, label = { Text("Query Params (key=value per line)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = headersText, onValueChange = { headersText = it }, label = { Text("Headers (Key: Value per line)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())

                    if (method != "GET") {
                        OutlinedTextField(value = bodyJson, onValueChange = { bodyJson = it }, label = { Text("JSON Body") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onExecuteApiRequest(currentSpec) },
                            enabled = !apiRunning && url.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("run_api_request_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (apiRunning) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Send Request")
                        }
                        OutlinedButton(onClick = { onSaveApiRequest(currentSpec) }, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                        }
                        OutlinedButton(onClick = { onGenerateKotlinClient(currentSpec) }, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Kotlin")
                        }
                    }
                }
            }
        }

        if (apiResult != null) {
            item {
                val color = if (apiResult.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (apiResult.errorMessage != null) "Request Failed (${apiResult.latencyMs} ms)" else "HTTP ${apiResult.statusCode} • ${apiResult.latencyMs} ms",
                            style = MaterialTheme.typography.titleSmall,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                        if (apiResult.headersSummary.isNotBlank()) {
                            Text(apiResult.headersSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = apiResult.errorMessage ?: apiResult.responseBody,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackendToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
