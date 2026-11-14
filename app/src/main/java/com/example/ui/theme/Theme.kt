package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class TunnelSemanticColors(
    val topBarContainer: Color,
    val onTopBar: Color,
    val signalGood: Color,
    val signalMedium: Color,
    val signalWeak: Color,
    val reconnectButton: Color,
    val segmentSelected: Color,
    val cardElevated: Color,
    val subtleDivider: Color,
)

val LocalTunnelColors = staticCompositionLocalOf {
    TunnelSemanticColors(
        topBarContainer = TealDeep,
        onTopBar = Color.White,
        signalGood = SignalEmerald,
        signalMedium = LatencyAmber,
        signalWeak = ReconnectCrimson,
        reconnectButton = ReconnectCrimson,
        segmentSelected = LavenderSelectedTint,
        cardElevated = SurfaceLight,
        subtleDivider = OutlineLight,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = TealDeep,
    onPrimary = Color.White,
    primaryContainer = TealSoftTint,
    onPrimaryContainer = TealDarker,
    secondary = TealMedium,
    onSecondary = Color.White,
    secondaryContainer = LavenderSelectedTint,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = SignalEmerald,
    onTertiary = Color.White,
    error = ReconnectCrimson,
    onError = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = TealBright,
    onPrimary = TealDarker,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealSoftTint,
    secondary = TealMedium,
    onSecondary = Color.White,
    secondaryContainer = LavenderSelectedDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = SignalEmeraldBright,
    onTertiary = Color(0xFF042914),
    error = ReconnectCrimsonDark,
    onError = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun CipherTunnelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        TunnelSemanticColors(
            topBarContainer = TealDarker,
            onTopBar = Color.White,
            signalGood = SignalEmeraldBright,
            signalMedium = Color(0xFFF59E0B),
            signalWeak = ReconnectCrimsonDark,
            reconnectButton = ReconnectCrimsonDark,
            segmentSelected = LavenderSelectedDark,
            cardElevated = SurfaceDark,
            subtleDivider = OutlineDark,
        )
    } else {
        TunnelSemanticColors(
            topBarContainer = TealDeep,
            onTopBar = Color.White,
            signalGood = SignalEmerald,
            signalMedium = LatencyAmber,
            signalWeak = ReconnectCrimson,
            reconnectButton = ReconnectCrimson,
            segmentSelected = LavenderSelectedTint,
            cardElevated = SurfaceLight,
            subtleDivider = OutlineLight,
        )
    }

    CompositionLocalProvider(LocalTunnelColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CipherTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
