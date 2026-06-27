package com.salesinventory.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue800 = Color(0xFF1565C0)
val Blue900 = Color(0xFF0D47A1)
val Blue100 = Color(0xFFD1E4FF)
val Blue50 = Color(0xFFE3F2FD)
val Amber700 = Color(0xFFF57F17)
val Amber50 = Color(0xFFFFF8E1)
val Green700 = Color(0xFF2E7D32)
val Green50 = Color(0xFFE8F5E9)
val Red700 = Color(0xFFD32F2F)
val Red50 = Color(0xFFFFEBEE)
val Grey100 = Color(0xFFF5F5F5)
val Grey400 = Color(0xFFBDBDBD)
val Grey600 = Color(0xFF757575)

val AppColorScheme = lightColorScheme(
    primary = Blue800,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    secondary = Color(0xFF1E88E5),
    onSecondary = Color.White,
    secondaryContainer = Blue50,
    onSecondaryContainer = Blue900,
    tertiary = Amber700,
    onTertiary = Color.White,
    tertiaryContainer = Amber50,
    onTertiaryContainer = Color(0xFF3E2723),
    error = Red700,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Color(0xFFB71C1C),
    background = Grey100,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Grey600,
    outline = Grey400
)

@Composable
fun SalesInventoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
