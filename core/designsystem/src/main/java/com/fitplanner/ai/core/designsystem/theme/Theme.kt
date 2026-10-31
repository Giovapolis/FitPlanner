package com.fitplanner.ai.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = SignalOrange,
    onPrimary = PaperRaised,
    primaryContainer = SignalOrangeSoft,
    onPrimaryContainer = ColorTokens.OnPrimaryContainerLight,
    secondary = Moss,
    onSecondary = PaperRaised,
    secondaryContainer = MossSoft,
    onSecondaryContainer = ColorTokens.OnSecondaryContainerLight,
    tertiary = Steel,
    onTertiary = PaperRaised,
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperMuted,
    onSurfaceVariant = InkSoft,
    outline = ColorTokens.OutlineLight,
    error = Danger,
    onError = PaperRaised
)

private val DarkColors = darkColorScheme(
    primary = DarkOrange,
    onPrimary = ColorTokens.OnPrimaryContainerLight,
    primaryContainer = ColorTokens.OrangeContainerDark,
    onPrimaryContainer = ColorTokens.OrangeOnContainerDark,
    secondary = DarkMoss,
    onSecondary = ColorTokens.OnSecondaryContainerLight,
    secondaryContainer = ColorTokens.MossContainerDark,
    onSecondaryContainer = ColorTokens.MossOnContainerDark,
    tertiary = ColorTokens.SteelDark,
    onTertiary = ColorTokens.OnPrimaryContainerLight,
    background = DarkPaper,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.OnSurfaceVariantDark,
    outline = DarkOutline,
    error = ColorTokens.DarkError,
    onError = ColorTokens.OnPrimaryContainerLight
)

private object ColorTokens {
    val OnPrimaryContainerLight = androidx.compose.ui.graphics.Color(0xFF3A0B00)
    val OnSecondaryContainerLight = androidx.compose.ui.graphics.Color(0xFF0B2118)
    val OutlineLight = androidx.compose.ui.graphics.Color(0xFF7A776E)
    val OrangeContainerDark = androidx.compose.ui.graphics.Color(0xFF6E250F)
    val OrangeOnContainerDark = androidx.compose.ui.graphics.Color(0xFFFFD9CC)
    val MossContainerDark = androidx.compose.ui.graphics.Color(0xFF24463A)
    val MossOnContainerDark = androidx.compose.ui.graphics.Color(0xFFD6E8DE)
    val SteelDark = androidx.compose.ui.graphics.Color(0xFFB8C7CF)
    val OnSurfaceVariantDark = androidx.compose.ui.graphics.Color(0xFFC5CBC6)
    val DarkError = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
}

val FitShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

@Composable
fun FitPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = FitTypography,
        shapes = FitShapes,
        content = content
    )
}
