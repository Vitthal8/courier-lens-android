package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LogisticsAmber,
    onPrimary = Color.Black,
    primaryContainer = LogisticsAmberDark,
    onPrimaryContainer = Color.White,
    secondary = ElectricCyanLight,
    onSecondary = Color.Black,
    secondaryContainer = CargoNavyLight,
    onSecondaryContainer = ElectricCyanLight,
    tertiary = StatusSuccess,
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = CargoNavy,
    onPrimary = Color.White,
    primaryContainer = LogisticsAmberContainer,
    onPrimaryContainer = CargoNavy,
    secondary = ElectricCyan,
    onSecondary = Color.White,
    secondaryContainer = ElectricCyanContainer,
    onSecondaryContainer = CargoNavy,
    tertiary = StatusSuccess,
    background = BackgroundLight,
    onBackground = CargoNavy,
    surface = SurfaceLight,
    onSurface = CargoNavy,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = CargoSlate
)

@Composable
fun CourierLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
