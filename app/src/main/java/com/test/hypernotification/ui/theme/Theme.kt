package com.test.hypernotification.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 颜色定义 - 适配MIUI风格
private val MiuiBlue = Color(0xFF2196F3)
private val MiuiDarkBlue = Color(0xFF1976D2)
private val MiuiLightBlue = Color(0xFFBBDEFB)
private val MiuiOrange = Color(0xFFFF6D00)

private val DarkColorScheme = darkColorScheme(
    primary = MiuiBlue,
    onPrimary = Color.White,
    primaryContainer = MiuiDarkBlue,
    onPrimaryContainer = Color.White,
    secondary = MiuiOrange,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

private val LightColorScheme = lightColorScheme(
    primary = MiuiBlue,
    onPrimary = Color.White,
    primaryContainer = MiuiLightBlue,
    onPrimaryContainer = MiuiDarkBlue,
    secondary = MiuiOrange,
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF757575)
)

@Composable
fun HyperNotificationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}