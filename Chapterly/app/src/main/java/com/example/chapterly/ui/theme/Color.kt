package com.example.chapterly.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val LightPrimary = Color(0xFF3E4FC7)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightSecondary = Color(0xFF646C8E)
private val LightBackground = Color(0xFFF5F3FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFE9E6F2)
private val LightOnSurface = Color(0xFF1C1B24)
private val LightOnSurfaceVariant = Color(0xFF5A5868)

private val DarkPrimary = Color(0xFF7C8CFF)
private val DarkOnPrimary = Color(0xFF10133A)
private val DarkSecondary = Color(0xFFB8BFEB)
private val DarkBackground = Color(0xFF0B0E1C)
private val DarkSurface = Color(0xFF161A2D)
private val DarkSurfaceVariant = Color(0xFF232942)
private val DarkOnSurface = Color(0xFFE6E8F7)
private val DarkOnSurfaceVariant = Color(0xFFA6ABCA)

internal val AppLightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        secondary = LightSecondary,
        background = LightBackground,
        surface = LightSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurface = LightOnSurface,
        onSurfaceVariant = LightOnSurfaceVariant,
    )

internal val AppDarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        secondary = DarkSecondary,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurface = DarkOnSurface,
        onSurfaceVariant = DarkOnSurfaceVariant,
    )
