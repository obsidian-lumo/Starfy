package com.example.model

import androidx.compose.runtime.Immutable
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.CornerStyle
import com.example.ui.theme.SpeedUnit
import com.example.ui.theme.ThemeMode

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

enum class ServerCategory(val title: String) {
    ALL("Все"),
    FAST("⚡ Быстрые"),
    GAMING("🎮 Для игр"),
    LTE("📶 LTE"),
    EUROPE("🇪🇺 Европа"),
    USA("🇺🇸 США")
}

@Immutable
data class VpnServer(
    val id: String,
    val name: String,
    val countryCode: String,
    val flag: String,
    val protocol: String,
    val network: String,
    val security: String,
    val address: String,
    val port: Int,
    val category: ServerCategory = ServerCategory.ALL,
    val trafficMultiplier: String = "x1",
    val pingMs: Int? = null,
    val isTestingPing: Boolean = false,
    val isSeparator: Boolean = false,
    val rawConfig: String? = null
)

@Immutable
data class SubscriptionInfo(
    val url: String = "",
    val title: String = "Нет активной подписки",
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val expireTimestamp: Long = 0L,
    val refillTimestamp: Long = 0L,
    val announcement: String = "",
    val supportUrl: String = "",
    val websiteUrl: String = "",
    val lastUpdated: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isConfigured: Boolean get() = url.isNotBlank()
    val usedBytes: Long get() = uploadBytes + downloadBytes
    val remainingBytes: Long get() = (totalBytes - usedBytes).coerceAtLeast(0L)
    val usageRatio: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

@Immutable
data class VpnTrafficStats(
    val durationSeconds: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val sessionDownloadedBytes: Long = 0L,
    val sessionUploadedBytes: Long = 0L,
    val currentIp: String = ""
)

@Immutable
data class AppSettings(
    val bypassRussianDomains: Boolean = true,
    val killSwitch: Boolean = false,
    val autoConnect: Boolean = false,
    val dnsProvider: String = "Cloudflare (1.1.1.1)",
    val protocolMode: String = "Авто (Рекомендуется)",
    val notificationsEnabled: Boolean = true,
    val themeColor: AppThemeColor = AppThemeColor.CYAN,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val amoledMode: Boolean = false,
    val dynamicColor: Boolean = false,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val speedUnit: SpeedUnit = SpeedUnit.BYTES,
    val hapticFeedback: Boolean = true,
    val showLiveGraph: Boolean = true
)
