package ru.localdialog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF193C37)
val Moss = Color(0xFF42685D)
val Lime = Color(0xFFCDE8A8)
val Paper = Color(0xFFF6F7F3)
val Mist = Color(0xFFE9EEE8)
val Terracotta = Color(0xFFC57453)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Lime,
    onPrimaryContainer = Ink,
    secondary = Moss,
    onSecondary = Color.White,
    secondaryContainer = Mist,
    onSecondaryContainer = Ink,
    tertiary = Terracotta,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF65736C),
    outline = Color(0xFFD4DDD5),
)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = Color(0xFF35584C),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFA7C5B6),
    background = Color(0xFF111C19),
    onBackground = Color(0xFFEAF1EA),
    surface = Color(0xFF1A2924),
    onSurface = Color(0xFFEAF1EA),
    surfaceVariant = Color(0xFF283A33),
    onSurfaceVariant = Color(0xFFAABBB0),
    outline = Color(0xFF40534A),
)

@Composable
fun LocalDialogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
