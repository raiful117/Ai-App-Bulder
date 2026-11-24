package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppComponent
import com.example.model.AppThemePreset
import com.example.model.ColorRole
import com.example.model.ComponentType
import com.example.viewmodel.PreviewRuntimeState

fun resolveComponentIcon(name: String): ImageVector = when (name.lowercase()) {
    "home" -> Icons.Filled.Home
    "bolt", "flash" -> Icons.Filled.Bolt
    "autoawesome", "ai", "spark" -> Icons.Filled.AutoAwesome
    "shoppingcart", "cart" -> Icons.Filled.ShoppingCart
    "shoppingbag", "store", "bag" -> Icons.Filled.ShoppingBag
    "analytics", "chart", "trendingup" -> Icons.Filled.Analytics
    "person", "profile", "account" -> Icons.Filled.Person
    "lock", "security" -> Icons.Filled.Lock
    "favorite", "health", "heart" -> Icons.Filled.Favorite
    "accountbalance", "wallet", "finance" -> Icons.Filled.AccountBalance
    "star" -> Icons.Filled.Star
    "edit" -> Icons.Filled.Edit
    "add" -> Icons.Filled.Add
    "checkcircle", "check" -> Icons.Filled.CheckCircle
    "notifications", "bell" -> Icons.Filled.Notifications
    "dashboard" -> Icons.Filled.Dashboard
    else -> Icons.Filled.Widgets
}

@Composable
fun PreviewComponentRenderer(
    component: AppComponent,
    themePreset: AppThemePreset,
    darkSurface: Boolean,
    interactive: Boolean,
    xrayMode: Boolean,
    runtimeState: PreviewRuntimeState,
    onComponentTap: () -> Unit,
    onInputChange: (String) -> Unit = {},
    onSwitchChange: (Boolean) -> Unit = {},
    onSliderChange: (Float) -> Unit = {},
    onChipSelect: (String) -> Unit = {},
    onBottomNavTabTap: (String) -> Unit = {}
) {
    if (interactive && component.componentId in runtimeState.hiddenComponentIds) {
        return
    }

    val displayText = if (interactive) {
        runtimeState.dynamicTexts[component.componentId] ?: component.text
    } else {
        component.text
    }

    val primary = Color(themePreset.primaryHex)
    val secondary = Color(themePreset.secondaryHex)
    val accent = when (component.colorRole) {
        ColorRole.PRIMARY -> primary
        ColorRole.SECONDARY -> secondary
        ColorRole.SUCCESS -> Color(0xFF10B981)
        ColorRole.WARNING -> Color(0xFFF59E0B)
        ColorRole.ERROR -> Color(0xFFF43F5E)
        ColorRole.SURFACE -> primary
    }

    val cardBg = if (darkSurface) Color(0xFF151E34) else Color(0xFFFFFFFF)
    val surfaceHigh = if (darkSurface) Color(0xFF1E2945) else Color(0xFFF1F5F9)
    val textPrimary = if (darkSurface) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (darkSurface) Color(0xFF94A3B8) else Color(0xFF475569)
    val borderColor = if (xrayMode) primary.copy(alpha = 0.8f) else if (darkSurface) Color(0xFF283556) else Color(0xFFE2E8F0)

    val wrapperMod = Modifier
        .fillMaxWidth()
        .then(
            if (xrayMode) {
                Modifier.border(1.dp, primary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            } else Modifier
        )
        .clickable { onComponentTap() }

    Box(modifier = wrapperMod) {
        when (component.type) {
            ComponentType.TOP_APP_BAR -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceHigh,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(resolveComponentIcon(component.iconName), null, tint = accent, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = displayText.ifBlank { "App Bar" },
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        if (component.subtitle.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = accent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = component.subtitle,
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            ComponentType.HERO_BANNER -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(component.cornerRadiusDp.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.28f), secondary.copy(alpha = 0.22f), cardBg)
                            )
                        )
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(component.cornerRadiusDp.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(resolveComponentIcon(component.iconName), null, tint = accent, modifier = Modifier.size(20.dp))
                            Text(
                                text = displayText.ifBlank { "Featured Banner" },
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        if (component.subtitle.isNotBlank()) {
                            Text(
                                text = component.subtitle,
                                color = textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            ComponentType.CARD_CONTAINER -> {
                Surface(
                    shape = RoundedCornerShape(component.cornerRadiusDp.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(resolveComponentIcon(component.iconName), null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayText, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (component.subtitle.isNotBlank()) {
                                Text(component.subtitle, color = textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            ComponentType.ROW_STATS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(component.text, component.subtitle.ifBlank { "Active: 99.9%" }).forEachIndexed { idx, label ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val parts = label.split(":", limit = 2)
                                Text(
                                    text = parts.getOrNull(0)?.trim().orEmpty(),
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = parts.getOrNull(1)?.trim() ?: label,
                                    color = if (idx == 0) accent else secondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            ComponentType.DIVIDER -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (component.text.isNotBlank()) {
                        Text(
                            text = component.text.uppercase(),
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f), color = borderColor)
                }
            }

            ComponentType.SPACER -> {
                val h = component.text.toIntOrNull()?.coerceIn(4, 48) ?: 16
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(h.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (xrayMode) {
                        Text("Spacer (${h}dp)", color = primary, fontSize = 10.sp)
                    }
                }
            }

            ComponentType.HEADLINE -> {
                Text(
                    text = displayText.ifBlank { "Headline" },
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            ComponentType.BODY_TEXT -> {
                Text(
                    text = displayText,
                    color = textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            ComponentType.BADGE_CHIP -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Text(displayText, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (component.subtitle.isNotBlank()) {
                                Text("• ${component.subtitle}", color = textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            ComponentType.STAT_METRIC -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayText, color = accent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            if (component.subtitle.isNotBlank()) {
                                Text(component.subtitle, color = textSecondary, fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = accent, modifier = Modifier.size(28.dp))
                    }
                }
            }

            ComponentType.AVATAR_ROW -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, null, tint = accent)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(component.text, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(component.subtitle, color = textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            ComponentType.IMAGE_BANNER -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.35f), secondary.copy(alpha = 0.35f))
                            )
                        )
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(component.text, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (component.subtitle.isNotBlank()) {
                            Text(component.subtitle, color = textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            ComponentType.PROGRESS_BAR -> {
                val pct = (component.subtitle.toIntOrNull() ?: 70).coerceIn(0, 100) / 100f
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(component.text, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${(pct * 100).toInt()}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accent,
                            trackColor = surfaceHigh
                        )
                    }
                }
            }

            ComponentType.TEXT_INPUT -> {
                val currentVal = runtimeState.inputValues[component.componentId] ?: ""
                OutlinedTextField(
                    value = currentVal,
                    onValueChange = { if (interactive) onInputChange(it) },
                    readOnly = !interactive,
                    enabled = interactive,
                    label = { Text(component.text) },
                    placeholder = { Text(component.subtitle.ifBlank { "Enter value..." }) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        disabledTextColor = textPrimary,
                        disabledBorderColor = borderColor,
                        disabledLabelColor = textSecondary
                    )
                )
            }

            ComponentType.PRIMARY_BUTTON -> {
                Button(
                    onClick = onComponentTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = if (accent == Color(0xFFE2E8F0) || accent == primary) Color(0xFF041C26) else Color.White
                    )
                ) {
                    Icon(resolveComponentIcon(component.iconName), null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(displayText.ifBlank { "Continue" }, fontWeight = FontWeight.Bold)
                }
            }

            ComponentType.OUTLINED_BUTTON -> {
                OutlinedButton(
                    onClick = onComponentTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent)
                ) {
                    Text(displayText.ifBlank { "Secondary Action" }, color = accent, fontWeight = FontWeight.SemiBold)
                }
            }

            ComponentType.FAB_BUTTON -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = onComponentTap,
                        shape = RoundedCornerShape(16.dp),
                        color = accent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, tint = Color(0xFF041C26))
                            Text(displayText.ifBlank { "Create" }, color = Color(0xFF041C26), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            ComponentType.SWITCH_TOGGLE -> {
                val checked = runtimeState.switchValues[component.componentId]
                    ?: (component.subtitle.lowercase() != "false")
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(component.text, color = textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = checked,
                            onCheckedChange = { if (interactive) onSwitchChange(it) else onComponentTap() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent)
                        )
                    }
                }
            }

            ComponentType.SLIDER_CONTROL -> {
                val sliderVal = runtimeState.sliderValues[component.componentId]
                    ?: (component.subtitle.toFloatOrNull()?.coerceIn(0f, 100f) ?: 60f)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(component.text, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${sliderVal.toInt()}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = sliderVal,
                            onValueChange = { if (interactive) onSliderChange(it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                    }
                }
            }

            ComponentType.CHIP_GROUP -> {
                val chips = component.text.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("All", "Active") }
                val activeChip = runtimeState.chipSelections[component.componentId] ?: chips.first()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { chip ->
                        val selected = chip == activeChip
                        FilterChip(
                            selected = selected,
                            onClick = { if (interactive) onChipSelect(chip) else onComponentTap() },
                            label = { Text(chip, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.22f),
                                selectedLabelColor = accent,
                                labelColor = textSecondary
                            )
                        )
                    }
                }
            }

            ComponentType.LIST_TILE -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(resolveComponentIcon(component.iconName), null, tint = accent, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(component.text, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (component.subtitle.isNotBlank()) {
                                Text(component.subtitle, color = textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = textSecondary)
                    }
                }
            }

            ComponentType.DATA_CHART -> {
                val points = component.subtitle.split(",").mapNotNull { it.trim().toFloatOrNull() }
                    .ifEmpty { listOf(42f, 68f, 55f, 89f, 76f, 94f) }
                val maxVal = (points.maxOrNull() ?: 100f).coerceAtLeast(1f)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(component.text, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("LIVE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp)
                        ) {
                            val count = points.size
                            val gap = 10.dp.toPx()
                            val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(8f)
                            points.forEachIndexed { idx, value ->
                                val barHeight = (value / maxVal) * size.height
                                val x = idx * (barWidth + gap)
                                val y = size.height - barHeight
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(accent, secondary.copy(alpha = 0.7f))),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            ComponentType.BOTTOM_NAV -> {
                val tabs = component.text.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .ifEmpty { listOf("Home", "Explore", "Profile") }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceHigh,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        tabs.take(4).forEachIndexed { idx, tabLabel ->
                            val isFirst = idx == 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (interactive) onBottomNavTabTap(tabLabel) else onComponentTap()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = resolveComponentIcon(tabLabel),
                                    contentDescription = null,
                                    tint = if (isFirst) accent else textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tabLabel,
                                    color = if (isFirst) accent else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            ComponentType.LINEAR_LAYOUT,
            ComponentType.VERTICAL_LAYOUT,
            ComponentType.HORIZONTAL_LAYOUT,
            ComponentType.FRAME_LAYOUT,
            ComponentType.CONSTRAINT_LAYOUT,
            ComponentType.SCROLL_VIEW -> {
                Surface(
                    shape = RoundedCornerShape(component.cornerRadiusDp.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${component.type.displayName} • ${component.text}",
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = component.widthMode,
                                color = textSecondary,
                                fontSize = 10.sp
                            )
                        }
                        if (component.subtitle.isNotBlank()) {
                            Text(component.subtitle, color = textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            ComponentType.PASSWORD_FIELD -> {
                val currentVal = runtimeState.inputValues[component.componentId] ?: ""
                OutlinedTextField(
                    value = currentVal,
                    onValueChange = { if (interactive) onInputChange(it) },
                    readOnly = !interactive,
                    enabled = interactive,
                    label = { Text(component.text) },
                    placeholder = { Text("••••••••") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        disabledTextColor = textPrimary,
                        disabledBorderColor = borderColor,
                        disabledLabelColor = textSecondary
                    )
                )
            }

            ComponentType.CHECKBOX -> {
                val checked = runtimeState.switchValues[component.componentId] ?: (component.subtitle.lowercase() != "false")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = checked,
                            onCheckedChange = { if (interactive) onSwitchChange(it) else onComponentTap() }
                        )
                        Text(component.text, color = textPrimary, fontSize = 14.sp)
                    }
                }
            }

            ComponentType.RADIO_BUTTON -> {
                val options = component.text.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Option A", "Option B") }
                val selectedOpt = runtimeState.chipSelections[component.componentId] ?: options.first()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        options.take(4).forEach { opt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (interactive) onChipSelect(opt) else onComponentTap() }
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = opt == selectedOpt,
                                    onClick = { if (interactive) onChipSelect(opt) else onComponentTap() }
                                )
                                Text(opt, color = textPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            ComponentType.IMAGE_BUTTON -> {
                OutlinedButton(
                    onClick = onComponentTap,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent)
                ) {
                    Icon(resolveComponentIcon(component.iconName), null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(component.text.ifBlank { "Icon Action" }, color = accent, fontWeight = FontWeight.SemiBold)
                }
            }

            ComponentType.RECYCLER_LIST -> {
                val rows = component.subtitle.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .ifEmpty { listOf("Item 1", "Item 2", "Item 3") }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(component.text, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        rows.take(4).forEach { rowLabel ->
                            Surface(shape = RoundedCornerShape(10.dp), color = surfaceHigh) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(rowLabel, color = textPrimary, fontSize = 13.sp)
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            ComponentType.TAB_LAYOUT -> {
                val tabs = component.text.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .ifEmpty { listOf("Overview", "Feed", "Settings") }
                val activeTab = runtimeState.chipSelections[component.componentId] ?: tabs.first()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceHigh,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabs.take(4).forEach { tab ->
                            val selected = tab == activeTab
                            Surface(
                                onClick = { if (interactive) onChipSelect(tab) else onComponentTap() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) accent else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (selected) Color(0xFF041C26) else textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            ComponentType.VIEW_PAGER -> {
                val pages = component.subtitle.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .ifEmpty { listOf("Page 1", "Page 2", "Page 3") }
                val activePage = runtimeState.chipSelections[component.componentId] ?: pages.first()
                Surface(
                    shape = RoundedCornerShape(component.cornerRadiusDp.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ViewCarousel, null, tint = accent, modifier = Modifier.size(18.dp))
                                Text(displayText, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(activePage, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pages.take(4).forEach { p ->
                                FilterChip(
                                    selected = p == activePage,
                                    onClick = { if (interactive) onChipSelect(p) else onComponentTap() },
                                    label = { Text(p, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            ComponentType.FLOATING_PANEL -> {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
                    color = surfaceHigh,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(textSecondary.copy(alpha = 0.5f))
                        )
                        Text(displayText, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (component.subtitle.isNotBlank()) {
                            Text(component.subtitle, color = textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            ComponentType.RATING_BAR -> {
                val initial = component.subtitle.toFloatOrNull()?.coerceIn(1f, 5f) ?: 4f
                val currentStars = (runtimeState.sliderValues[component.componentId] ?: initial).toInt().coerceIn(1, 5)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(displayText, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = if (star <= currentStars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Rate $star stars",
                                    tint = if (star <= currentStars) Color(0xFFF59E0B) else textSecondary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable {
                                            if (interactive) onSliderChange(star.toFloat()) else onComponentTap()
                                        }
                                )
                            }
                        }
                    }
                }
            }

            ComponentType.DATE_PICKER -> {
                val chosen = runtimeState.inputValues[component.componentId] ?: component.subtitle.ifBlank { "2026-11-24" }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(displayText, color = textSecondary, fontSize = 11.sp)
                            Text(chosen, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Icon(Icons.Filled.CalendarMonth, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                }
            }

            ComponentType.SPINNER_DROPDOWN -> {
                val options = component.subtitle.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .ifEmpty { listOf("Option 1", "Option 2", "Option 3") }
                val selectedOpt = runtimeState.chipSelections[component.componentId] ?: options.first()
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { if (interactive) expanded = true else onComponentTap() },
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(displayText, color = textSecondary, fontSize = 11.sp)
                                Text(selectedOpt, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = accent)
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onChipSelect(opt)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            ComponentType.NAV_DRAWER,
            ComponentType.WEB_VIEW,
            ComponentType.VIDEO_PLAYER,
            ComponentType.LOTTIE_ANIMATION,
            ComponentType.CUSTOM_COMPONENT -> {
                Surface(
                    shape = RoundedCornerShape(component.cornerRadiusDp.dp),
                    color = surfaceHigh,
                    border = BorderStroke(1.dp, secondary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(resolveComponentIcon(component.iconName), null, tint = secondary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(component.type.displayName, color = secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(displayText, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (component.subtitle.isNotBlank()) {
                                Text(component.subtitle, color = textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
