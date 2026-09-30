package com.mapaurbano.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = CivicBlue,
    onPrimary = Color.White,
    primaryContainer = CivicBlueContainer,
    secondary = CivicOrange,
    background = AppBackground,
    surface = Color.White,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = CivicBlueDark,
    secondary = CivicOrange,
    background = DarkBackground,
  )

@Composable
fun MapaUrbanoTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    typography = AppTypography,
    content = content,
  )
}
