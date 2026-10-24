package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Default Brand Palette: Cyan
val CyanPrimary = Color(0xFF00E5FF)
val CyanOnPrimary = Color(0xFF00363D)
val CyanPrimaryContainer = Color(0xFF004F58)
val CyanOnPrimaryContainer = Color(0xFF8CF4FF)

val ElectricBlue = Color(0xFF2979FF)
val ElectricBlueContainer = Color(0xFF0D47A1)

// Dark Theme Surfaces
val DarkSurface = Color(0xFF0B101B)
val DarkSurfaceVariant = Color(0xFF151D2C)
val DarkSurfaceContainer = Color(0xFF1B2436)
val DarkSurfaceContainerHigh = Color(0xFF232E42)
val DarkBackground = Color(0xFF070B12)
val DarkOnBackground = Color(0xFFE2E8F0)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)

// Status Colors
val StatusConnected = Color(0xFF10B981) // Emerald Green
val StatusConnectedContainer = Color(0xFF064E3B)
val StatusConnecting = Color(0xFFF59E0B) // Amber
val StatusConnectingContainer = Color(0xFF78350F)
val StatusDisconnected = Color(0xFF64748B) // Slate
val StatusError = Color(0xFFEF4444)

// Light Theme Surfaces
val LightSurface = Color(0xFFF8FAFC)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightBackground = Color(0xFFF1F5F9)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)

enum class AppThemeColor(
    val title: String,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val isLightSwatch: Boolean = false,
    val isDarkSwatch: Boolean = false
) {
    MONOCHROME_WHITE(
        title = "Белая (Монохром)",
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF0A0A0A),
        primaryContainer = Color(0xFF262626),
        onPrimaryContainer = Color(0xFFFFFFFF),
        isLightSwatch = true,
        isDarkSwatch = false
    ),
    MONOCHROME_BLACK(
        title = "Чёрная (Монохром)",
        primary = Color(0xFF0F172A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE2E8F0),
        onPrimaryContainer = Color(0xFF0F172A),
        isLightSwatch = false,
        isDarkSwatch = true
    ),
    CYAN("Неоновый циан", Color(0xFF00E5FF), Color(0xFF00363D), Color(0xFF004F58), Color(0xFF8CF4FF)),
    EMERALD("Изумруд", Color(0xFF10B981), Color(0xFF003822), Color(0xFF005234), Color(0xFF6EE7B7)),
    BLUE("Электрик синий", Color(0xFF3B82F6), Color(0xFF002B73), Color(0xFF003FA8), Color(0xFF93C5FD)),
    PURPLE("Кибер пурпур", Color(0xFFA855F7), Color(0xFF38006B), Color(0xFF5800A0), Color(0xFFD8B4FE)),
    AMBER("Янтарь", Color(0xFFF59E0B), Color(0xFF452B00), Color(0xFF633F00), Color(0xFFFDE68A)),
    CORAL("Коралл", Color(0xFFFF5722), Color(0xFF5C1100), Color(0xFF821D00), Color(0xFFFFCCBC)),
    CRIMSON("Рубиновый", Color(0xFFE11D48), Color(0xFF4C0519), Color(0xFF881337), Color(0xFFFECDD3)),
    MINT("Мятный фреш", Color(0xFF14B8A6), Color(0xFF042F2E), Color(0xFF0F766E), Color(0xFF99F6E4)),
    ROSE("Розовый неон", Color(0xFFEC4899), Color(0xFF500724), Color(0xFF831843), Color(0xFFFBCFE8)),
    GOLD("Золото", Color(0xFFEAB308), Color(0xFF422006), Color(0xFF713F12), Color(0xFFFEF08A));

    fun compatibleWith(isDarkTheme: Boolean): AppThemeColor {
        return when {
            isDarkTheme && this.isDarkSwatch -> MONOCHROME_WHITE
            !isDarkTheme && this.isLightSwatch -> MONOCHROME_BLACK
            else -> this
        }
    }
}

enum class ThemeMode(val title: String) {
    DARK("Тёмная"),
    LIGHT("Светлая"),
    SYSTEM("Системная")
}

enum class CornerStyle(val title: String, val radiusDp: Int) {
    EXPRESSIVE("Плавное (24 dp)", 24),
    ROUNDED("Стандарт (18 dp)", 18),
    COMPACT("Строгое (10 dp)", 10)
}

enum class SpeedUnit(val title: String) {
    BYTES("МБ/с (Байты)"),
    BITS("Мбит/с (Биты)")
}
