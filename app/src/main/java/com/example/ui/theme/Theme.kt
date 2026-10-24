package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MyApplicationTheme(
    themeColor: AppThemeColor = AppThemeColor.CYAN,
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    amoledMode: Boolean = false,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDark
    }

    val effectiveColor = themeColor.compatibleWith(isDark)
    val context = LocalContext.current

    val colorScheme = remember(effectiveColor, isDark, dynamicColor, amoledMode) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) {
                    val dyn = dynamicDarkColorScheme(context)
                    if (amoledMode) {
                        dyn.copy(
                            background = Color.Black,
                            surface = Color.Black,
                            surfaceVariant = Color(0xFF121212)
                        )
                    } else dyn
                } else {
                    dynamicLightColorScheme(context)
                }
            }
            isDark && effectiveColor == AppThemeColor.MONOCHROME_WHITE -> {
                val bg = if (amoledMode) Color.Black else Color(0xFF080808)
                val surf = if (amoledMode) Color(0xFF050505) else Color(0xFF121212)
                val surfVar = Color(0xFF1C1C1C)
                darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF2A2A2A),
                    onPrimaryContainer = Color.White,
                    secondary = Color(0xFFE5E5E5),
                    onSecondary = Color.Black,
                    secondaryContainer = Color(0xFF262626),
                    onSecondaryContainer = Color.White,
                    background = bg,
                    onBackground = Color.White,
                    surface = surf,
                    onSurface = Color.White,
                    surfaceVariant = surfVar,
                    onSurfaceVariant = Color(0xFFA3A3A3),
                    outline = Color(0xFF404040),
                    outlineVariant = Color(0xFF262626),
                    error = StatusError
                )
            }
            !isDark && effectiveColor == AppThemeColor.MONOCHROME_BLACK -> {
                lightColorScheme(
                    primary = Color(0xFF0A0A0A),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE5E5E5),
                    onPrimaryContainer = Color(0xFF0A0A0A),
                    secondary = Color(0xFF262626),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFE5E5E5),
                    onSecondaryContainer = Color(0xFF0A0A0A),
                    background = Color(0xFFFAFAFA),
                    onBackground = Color(0xFF0A0A0A),
                    surface = Color.White,
                    onSurface = Color(0xFF0A0A0A),
                    surfaceVariant = Color(0xFFEDEDED),
                    onSurfaceVariant = Color(0xFF525252),
                    outline = Color(0xFFD4D4D4),
                    outlineVariant = Color(0xFFE5E5E5),
                    error = StatusError
                )
            }
            isDark -> {
                val bg = if (amoledMode) Color.Black else DarkBackground
                val surf = if (amoledMode) Color(0xFF070707) else DarkSurface
                val surfVar = if (amoledMode) Color(0xFF121212) else DarkSurfaceVariant
                darkColorScheme(
                    primary = effectiveColor.primary,
                    onPrimary = effectiveColor.onPrimary,
                    primaryContainer = effectiveColor.primaryContainer,
                    onPrimaryContainer = effectiveColor.onPrimaryContainer,
                    secondary = effectiveColor.primary,
                    onSecondary = effectiveColor.onPrimary,
                    secondaryContainer = effectiveColor.primaryContainer,
                    onSecondaryContainer = effectiveColor.onPrimaryContainer,
                    background = bg,
                    onBackground = DarkOnBackground,
                    surface = surf,
                    onSurface = DarkOnSurface,
                    surfaceVariant = surfVar,
                    onSurfaceVariant = DarkOnSurfaceVariant,
                    outline = DarkOutline,
                    outlineVariant = DarkSurfaceContainerHigh,
                    error = StatusError
                )
            }
            else -> {
                lightColorScheme(
                    primary = effectiveColor.primary,
                    onPrimary = effectiveColor.onPrimary,
                    primaryContainer = effectiveColor.primaryContainer,
                    onPrimaryContainer = effectiveColor.onPrimaryContainer,
                    secondary = effectiveColor.primary,
                    onSecondary = effectiveColor.onPrimary,
                    secondaryContainer = effectiveColor.primaryContainer,
                    onSecondaryContainer = effectiveColor.onPrimaryContainer,
                    background = LightBackground,
                    onBackground = LightOnBackground,
                    surface = LightSurface,
                    onSurface = LightOnSurface,
                    surfaceVariant = LightSurfaceVariant,
                    onSurfaceVariant = LightOnSurfaceVariant,
                    outline = LightOutline,
                    error = StatusError
                )
            }
        }
    }

    val shapes = remember(cornerStyle) {
        val base = cornerStyle.radiusDp
        Shapes(
            extraSmall = RoundedCornerShape((base / 3).coerceAtLeast(4).dp),
            small = RoundedCornerShape((base * 2 / 3).coerceAtLeast(8).dp),
            medium = RoundedCornerShape(base.dp),
            large = RoundedCornerShape((base + 4).dp),
            extraLarge = RoundedCornerShape((base + 8).dp)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
