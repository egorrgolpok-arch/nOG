package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BlackWhiteColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = StarkWhite,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = StarkWhite,
    surface = CardGray,
    onSurface = StarkWhite,
    surfaceVariant = DeepGray,
    onSurfaceVariant = TextGray,
    outline = BorderGray,
    error = AlertRed
)

private val LightBlackWhiteColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    secondary = DeepGray,
    onSecondary = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = StarkWhite,
    onSurface = PureBlack,
    surfaceVariant = StarkWhite,
    onSurfaceVariant = TextGray,
    outline = BorderGray,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We default to dark theme for maximum black and white contrast look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BlackWhiteColorScheme else LightBlackWhiteColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
