package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.VpnViewModel
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.CornerStyle
import com.example.ui.theme.SpeedUnit
import com.example.ui.theme.ThemeMode

private val DNS_OPTIONS = listOf(
    "Cloudflare (1.1.1.1)",
    "Google DNS (8.8.8.8)",
    "Yandex DNS (77.88.8.8)",
    "AdGuard DNS (Без рекламы)"
)

private val PROTOCOL_OPTIONS = listOf(
    "Авто (Рекомендуется)",
    "VLESS Reality (Максимальная маскировка)",
    "Hysteria 2 (Игры и нестабильная сеть)",
    "VLESS WebSocket (LTE/Обход)"
)

@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    val isDarkActive = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemIsDark
    }

    // Filter color palettes:
    // - Dark theme active -> hide black palette (!palette.isDarkSwatch)
    // - Light theme active -> hide light/white palette (!palette.isLightSwatch)
    val availablePalettes = remember(isDarkActive) {
        AppThemeColor.entries.filter { palette ->
            if (isDarkActive) !palette.isDarkSwatch else !palette.isLightSwatch
        }
    }

    val effectiveSelectedColor = settings.themeColor.compatibleWith(isDarkActive)

    var showDnsDialog by remember { mutableStateOf(false) }
    var showProtocolDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header_appearance", contentType = "header") {
            SettingsCategoryHeader(title = "Оформление и кастомизация")
        }

        item(key = "card_appearance", contentType = "card") {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Цветовая палитра акцентов",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (settings.dynamicColor) {
                                    "Активны системные цвета Material You"
                                } else {
                                    "Выбрано: ${effectiveSelectedColor.title}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = availablePalettes,
                            key = { it.name },
                            contentType = { "color_swatch" }
                        ) { colorOption ->
                            val isSelected = !settings.dynamicColor && effectiveSelectedColor == colorOption
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(colorOption.primary)
                                    .clickable { viewModel.setThemeColor(colorOption) }
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape
                                    )
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбрано: ${colorOption.title}",
                                        tint = colorOption.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Режим интерфейса",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setThemeMode(mode, systemIsDark) },
                                label = { Text(mode.title, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            ThemeMode.DARK -> Icons.Default.DarkMode
                                            ThemeMode.LIGHT -> Icons.Default.LightMode
                                            ThemeMode.SYSTEM -> Icons.Default.ColorLens
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Стиль скругления углов",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CornerStyle.entries.forEach { style ->
                            val isSelected = settings.cornerStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCornerStyle(style) },
                                label = { Text(style.title, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.RoundedCorner,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item(key = "card_display_tuning", contentType = "card") {
            SettingsCard {
                if (isDarkActive) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Contrast,
                        title = "Чистый чёрный (AMOLED)",
                        subtitle = "Отключает фоновые оттенки для абсолютного чёрного (#000000) и экономии энергии OLED экранов",
                        checked = settings.amoledMode,
                        onCheckedChange = { viewModel.setAmoledMode(it) }
                    )

                    SettingsDivider()
                }

                SettingsSwitchItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Динамические цвета (Material You)",
                    subtitle = "Извлекать цвета интерфейса из обоев устройства (Android 12+)",
                    checked = settings.dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            }
        }

        item(key = "header_interface", contentType = "header") {
            SettingsCategoryHeader(title = "Интерфейс и графики")
        }

        item(key = "card_interface", contentType = "card") {
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Единицы измерения скорости",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Текущий формат: ${settings.speedUnit.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpeedUnit.entries.forEach { unit ->
                            val isSelected = settings.speedUnit == unit
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSpeedUnit(unit) },
                                label = { Text(unit.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Icons.Default.Timeline,
                    title = "Живой график скорости",
                    subtitle = "Отображать пульсирующий график трафика в реальном времени при активном VPN",
                    checked = settings.showLiveGraph,
                    onCheckedChange = { viewModel.setShowLiveGraph(it) }
                )

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    title = "Тактильный отклик (Вибрация)",
                    subtitle = "Приятная виброотдача при включении и переключении режимов VPN",
                    checked = settings.hapticFeedback,
                    onCheckedChange = { viewModel.setHapticFeedback(it) }
                )
            }
        }

        item(key = "header_routing", contentType = "header") {
            SettingsCategoryHeader(title = "Маршрутизация и правила")
        }

        item(key = "card_routing", contentType = "card") {
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.AltRoute,
                    title = "Обход российских сервисов",
                    subtitle = "Сайты .ru, Yandex, VK, Ozon, Госуслуги и банки работают напрямую на максимальной скорости",
                    checked = settings.bypassRussianDomains,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(bypassRussianDomains = checked) }
                    }
                )
            }
        }

        item(key = "header_security", contentType = "header") {
            SettingsCategoryHeader(title = "Безопасность и сеть")
        }

        item(key = "card_security", contentType = "card") {
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "Kill Switch",
                    subtitle = "Блокировать сетевой трафик устройства при случайном обрыве VPN соединения",
                    checked = settings.killSwitch,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(killSwitch = checked) }
                    }
                )

                SettingsDivider()

                SettingsClickableItem(
                    icon = Icons.Default.Dns,
                    title = "DNS Сервер",
                    subtitle = settings.dnsProvider,
                    onClick = { showDnsDialog = true }
                )

                SettingsDivider()

                SettingsClickableItem(
                    icon = Icons.Default.Tune,
                    title = "Предпочитаемый протокол",
                    subtitle = settings.protocolMode,
                    onClick = { showProtocolDialog = true }
                )
            }
        }

        item(key = "header_behavior", contentType = "header") {
            SettingsCategoryHeader(title = "Поведение приложения")
        }

        item(key = "card_behavior", contentType = "card") {
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Авто-подключение",
                    subtitle = "Автоматически подключать быстрый сервер при открытии приложения",
                    checked = settings.autoConnect,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(autoConnect = checked) }
                    }
                )

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления в шторке",
                    subtitle = "Отображать постоянный статус и скорость соединения в уведомлениях",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(notificationsEnabled = checked) }
                    }
                )
            }
        }

        item(key = "card_about", contentType = "about") {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Twelve VPN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Версия 1.2 • Нативный Xray Core с VLESS Reality и Hysteria",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("Выберите DNS-сервер") },
            text = {
                Column {
                    DNS_OPTIONS.forEach { dns ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings { it.copy(dnsProvider = dns) }
                                    showDnsDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = settings.dnsProvider == dns,
                                onClick = {
                                    viewModel.updateSettings { it.copy(dnsProvider = dns) }
                                    showDnsDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = dns, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDnsDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showProtocolDialog) {
        AlertDialog(
            onDismissRequest = { showProtocolDialog = false },
            title = { Text("Предпочитаемый протокол") },
            text = {
                Column {
                    PROTOCOL_OPTIONS.forEach { proto ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings { it.copy(protocolMode = proto) }
                                    showProtocolDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = settings.protocolMode == proto,
                                onClick = {
                                    viewModel.updateSettings { it.copy(protocolMode = proto) }
                                    showProtocolDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = proto, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProtocolDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
