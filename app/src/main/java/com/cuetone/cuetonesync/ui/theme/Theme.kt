package com.cuetone.cuetonesync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CueToneColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextPrimary,
    primaryContainer = Selected,
    onPrimaryContainer = TextPrimary,
    secondary = Primary,
    onSecondary = TextPrimary,
    secondaryContainer = PrimarySoft,
    onSecondaryContainer = Primary,
    tertiary = Recording,
    onTertiary = TextPrimary,
    tertiaryContainer = RecordingSoft,
    onTertiaryContainer = Recording,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Border,
    error = Recording,
    onError = TextPrimary,
    errorContainer = RecordingSoft,
    onErrorContainer = Recording,
    scrim = AppBackground
)

@Composable
fun CueToneSyncTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CueToneColorScheme,
        typography = Typography,
        content = content
    )
}
