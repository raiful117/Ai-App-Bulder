package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppProject

private data class LibraryCatalogItem(
    val title: String,
    val coordinate: String,
    val description: String
)

private data class PermissionCatalogItem(
    val label: String,
    val manifestName: String,
    val description: String
)

@Composable
fun ProjectConfigScreen(
    project: AppProject?,
    onToggleLibrary: (String) -> Unit,
    onTogglePermission: (String) -> Unit,
    onUpsertStringResource: (String, String) -> Unit,
    onRemoveStringResource: (String) -> Unit,
    onUpsertColorResource: (String, String) -> Unit,
    onRemoveColorResource: (String) -> Unit,
    onSyncGradleAndManifest: () -> Unit
) {
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a project to configure Libraries, Permissions & Resources.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val libraries = remember {
        listOf(
            LibraryCatalogItem("Material Design 3", "androidx.compose.material3:material3", "Core M3 Compose UI components"),
            LibraryCatalogItem("Navigation Compose", "androidx.navigation:navigation-compose:2.8.9", "Type-safe Jetpack Compose routing"),
            LibraryCatalogItem("Room Local Database", "androidx.room:room-ktx:2.7.0", "Offline SQLite persistence & DAO"),
            LibraryCatalogItem("Firebase Auth", "com.google.firebase:firebase-auth:23.2.0", "Google & Email authentication"),
            LibraryCatalogItem("Cloud Firestore", "com.google.firebase:firebase-firestore:25.1.2", "Real-time NoSQL cloud sync"),
            LibraryCatalogItem("Firebase Storage", "com.google.firebase:firebase-storage:21.0.1", "Cloud media & file uploads"),
            LibraryCatalogItem("Firebase Messaging (FCM)", "com.google.firebase:firebase-messaging:24.1.0", "Push notifications"),
            LibraryCatalogItem("Retrofit + OkHttp", "com.squareup.retrofit2:retrofit:2.11.0", "REST API client & interceptors"),
            LibraryCatalogItem("Coil Image Loading", "io.coil-kt.coil3:coil-compose:3.1.0", "Async Compose image caching"),
            LibraryCatalogItem("Lottie Compose", "com.airbnb.android:lottie-compose:6.6.2", "Vector JSON animations"),
            LibraryCatalogItem("CameraX Core", "androidx.camera:camera-camera2:1.4.1", "Modern Android camera pipeline"),
            LibraryCatalogItem("WorkManager", "androidx.work:work-runtime-ktx:2.10.0", "Reliable background scheduling")
        )
    }

    val permissions = remember {
        listOf(
            PermissionCatalogItem("Internet Access", "android.permission.INTERNET", "Required for API calls & Firebase sync"),
            PermissionCatalogItem("Network State", "android.permission.ACCESS_NETWORK_STATE", "Check online/offline connectivity"),
            PermissionCatalogItem("Camera", "android.permission.CAMERA", "Capture photos or scan QR codes"),
            PermissionCatalogItem("Microphone / Audio", "android.permission.RECORD_AUDIO", "Voice notes or speech recognition"),
            PermissionCatalogItem("Fine Location", "android.permission.ACCESS_FINE_LOCATION", "GPS navigation & nearby maps"),
            PermissionCatalogItem("Notifications (API 33+)", "android.permission.POST_NOTIFICATIONS", "Show push & local notifications"),
            PermissionCatalogItem("Vibrator Haptics", "android.permission.VIBRATE", "Haptic feedback for alerts"),
            PermissionCatalogItem("Biometric Unlock", "android.permission.USE_BIOMETRIC", "Fingerprint / Face unlock")
        )
    }

    var subTab by remember { mutableIntStateOf(0) }
    var strKey by remember { mutableStateOf("") }
    var strValue by remember { mutableStateOf("") }
    var colorKey by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#38BDF8") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    label = { Text("Libraries (${project.backend.androidConfig.dependencies.size})") },
                    modifier = Modifier.testTag("config_tab_libs")
                )
                FilterChip(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    label = { Text("Permissions (${project.backend.androidConfig.permissions.size})") },
                    modifier = Modifier.testTag("config_tab_perms")
                )
                FilterChip(
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    label = { Text("Strings & Colors") },
                    modifier = Modifier.testTag("config_tab_res")
                )
                OutlinedButton(
                    onClick = onSyncGradleAndManifest,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sync Files")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (subTab) {
                0 -> {
                    item {
                        Text(
                            "Toggle dependencies in app/build.gradle.kts for '${project.name}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(libraries, key = { it.coordinate }) { lib ->
                        val enabled = project.backend.androidConfig.dependencies.any {
                            it.startsWith(lib.coordinate.substringBeforeLast(':'))
                        }
                        Surface(
                            onClick = { onToggleLibrary(lib.coordinate) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lib.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(lib.coordinate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(lib.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = enabled, onCheckedChange = { onToggleLibrary(lib.coordinate) })
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Play Policy Tip: For gallery image picking, use zero-permission Android Photo Picker (PickVisualMedia) instead of broad media storage permissions.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    items(permissions, key = { it.manifestName }) { perm ->
                        val enabled = perm.manifestName in project.backend.androidConfig.permissions
                        Surface(
                            onClick = { onTogglePermission(perm.manifestName) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = enabled, onCheckedChange = { onTogglePermission(perm.manifestName) })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(perm.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(perm.manifestName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(perm.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("res/values/strings.xml", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = strKey,
                                        onValueChange = { strKey = it },
                                        label = { Text("Key") },
                                        placeholder = { Text("checkout_cta") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = strValue,
                                        onValueChange = { strValue = it },
                                        label = { Text("Text") },
                                        placeholder = { Text("Pay Now") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Button(
                                        onClick = {
                                            if (strKey.isNotBlank()) {
                                                onUpsertStringResource(strKey, strValue)
                                                strKey = ""
                                                strValue = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, "Add string")
                                    }
                                }
                            }
                        }
                    }
                    items(project.backend.stringResources.entries.toList(), key = { "str_${it.key}" }) { (k, v) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("@string/$k", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(v, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onRemoveStringResource(k) }) {
                                    Icon(Icons.Filled.DeleteOutline, "Remove string", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("res/values/colors.xml", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = colorKey,
                                        onValueChange = { colorKey = it },
                                        label = { Text("Color Name") },
                                        placeholder = { Text("accentGold") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = colorHex,
                                        onValueChange = { colorHex = it },
                                        label = { Text("Hex") },
                                        placeholder = { Text("#F59E0B") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (colorKey.isNotBlank()) {
                                                onUpsertColorResource(colorKey, colorHex)
                                                colorKey = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Filled.Palette, "Add color")
                                    }
                                }
                            }
                        }
                    }
                    items(project.backend.colorResources.entries.toList(), key = { "clr_${it.key}" }) { (k, hex) ->
                        val parsedColor = remember(hex) {
                            runCatching {
                                val clean = hex.removePrefix("#")
                                val longVal = when (clean.length) {
                                    6 -> ("FF$clean").toLong(16)
                                    8 -> clean.toLong(16)
                                    else -> 0xFF38BDF8L
                                }
                                Color(longVal)
                            }.getOrElse { Color(0xFF38BDF8) }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(parsedColor))
                                    Column {
                                        Text("@color/$k", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text(hex, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { onRemoveColorResource(k) }) {
                                    Icon(Icons.Filled.DeleteOutline, "Remove color", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
