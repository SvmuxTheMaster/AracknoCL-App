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

private val DarkColorScheme =
  darkColorScheme(
    primary = ForestGreenPrimary,
    secondary = MossGreenAccent,
    tertiary = SageLeafTertiary,
    background = JungleDarkBackground,
    surface = FoliageDarkSurface,
    onPrimary = CrispDewDropWhite,
    onSecondary = CrispDewDropWhite,
    onBackground = CrispDewDropWhite,
    onSurface = CrispDewDropWhite,
    surfaceVariant = BarkDarkCard,
    onSurfaceVariant = FernMutedGray
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForestGreenPrimary,
    secondary = MossGreenAccent,
    tertiary = SageLeafTertiary,
    background = Color(0xFFF4F9F5), // Soft herbal light mist
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E2822),
    onSurface = Color(0xFF1E2822),
    surfaceVariant = Color(0xFFE2F0E6),
    onSurfaceVariant = Color(0xFF4A5F52)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
