package com.matejdro.pebblenotificationcenter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val primaryLight = Color(0xFF904B40)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFDAD4)
private val onPrimaryContainerLight = Color(0xFF73342A)
private val secondaryLight = Color(0xFF775651)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFFFDAD4)
private val onSecondaryContainerLight = Color(0xFF5D3F3B)
private val tertiaryLight = Color(0xFF705C2E)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFFBDFA6)
private val onTertiaryContainerLight = Color(0xFF564419)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFFF8F6)
private val onBackgroundLight = Color(0xFF231918)
private val surfaceLight = Color(0xFFFFF8F6)
private val onSurfaceLight = Color(0xFF231918)
private val surfaceVariantLight = Color(0xFFF5DDDA)
private val onSurfaceVariantLight = Color(0xFF534341)
private val outlineLight = Color(0xFF857370)
private val outlineVariantLight = Color(0xFFD8C2BE)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF392E2C)
private val inverseOnSurfaceLight = Color(0xFFFFEDEA)
private val inversePrimaryLight = Color(0xFFFFB4A8)
private val surfaceDimLight = Color(0xFFE8D6D3)
private val surfaceBrightLight = Color(0xFFFFF8F6)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFFF0EE)
private val surfaceContainerLight = Color(0xFFFCEAE7)
private val surfaceContainerHighLight = Color(0xFFF7E4E1)
private val surfaceContainerHighestLight = Color(0xFFF1DFDC)

private val primaryDark = Color(0xFFFFB4A8)
private val onPrimaryDark = Color(0xFF561E16)
private val primaryContainerDark = Color(0xFF73342A)
private val onPrimaryContainerDark = Color(0xFFFFDAD4)
private val secondaryDark = Color(0xFFE7BDB6)
private val onSecondaryDark = Color(0xFF442925)
private val secondaryContainerDark = Color(0xFF5D3F3B)
private val onSecondaryContainerDark = Color(0xFFFFDAD4)
private val tertiaryDark = Color(0xFFDEC48C)
private val onTertiaryDark = Color(0xFF3E2E04)
private val tertiaryContainerDark = Color(0xFF564419)
private val onTertiaryContainerDark = Color(0xFFFBDFA6)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF1A1110)
private val onBackgroundDark = Color(0xFFF1DFDC)
private val surfaceDark = Color(0xFF1A1110)
private val onSurfaceDark = Color(0xFFF1DFDC)
private val surfaceVariantDark = Color(0xFF534341)
private val onSurfaceVariantDark = Color(0xFFD8C2BE)
private val outlineDark = Color(0xFFA08C89)
private val outlineVariantDark = Color(0xFF534341)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFF1DFDC)
private val inverseOnSurfaceDark = Color(0xFF392E2C)
private val inversePrimaryDark = Color(0xFF904B40)
private val surfaceDimDark = Color(0xFF1A1110)
private val surfaceBrightDark = Color(0xFF423735)
private val surfaceContainerLowestDark = Color(0xFF140C0B)
private val surfaceContainerLowDark = Color(0xFF231918)
private val surfaceContainerDark = Color(0xFF271D1C)
private val surfaceContainerHighDark = Color(0xFF322826)
private val surfaceContainerHighestDark = Color(0xFF3D3230)

private val LightColorScheme = lightColorScheme(
   primary = primaryLight,
   onPrimary = onPrimaryLight,
   primaryContainer = primaryContainerLight,
   onPrimaryContainer = onPrimaryContainerLight,
   secondary = secondaryLight,
   onSecondary = onSecondaryLight,
   secondaryContainer = secondaryContainerLight,
   onSecondaryContainer = onSecondaryContainerLight,
   tertiary = tertiaryLight,
   onTertiary = onTertiaryLight,
   tertiaryContainer = tertiaryContainerLight,
   onTertiaryContainer = onTertiaryContainerLight,
   error = errorLight,
   onError = onErrorLight,
   errorContainer = errorContainerLight,
   onErrorContainer = onErrorContainerLight,
   background = backgroundLight,
   onBackground = onBackgroundLight,
   surface = surfaceLight,
   onSurface = onSurfaceLight,
   surfaceVariant = surfaceVariantLight,
   onSurfaceVariant = onSurfaceVariantLight,
   outline = outlineLight,
   outlineVariant = outlineVariantLight,
   scrim = scrimLight,
   inverseSurface = inverseSurfaceLight,
   inverseOnSurface = inverseOnSurfaceLight,
   inversePrimary = inversePrimaryLight,
   surfaceDim = surfaceDimLight,
   surfaceBright = surfaceBrightLight,
   surfaceContainerLowest = surfaceContainerLowestLight,
   surfaceContainerLow = surfaceContainerLowLight,
   surfaceContainer = surfaceContainerLight,
   surfaceContainerHigh = surfaceContainerHighLight,
   surfaceContainerHighest = surfaceContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
   primary = primaryDark,
   onPrimary = onPrimaryDark,
   primaryContainer = primaryContainerDark,
   onPrimaryContainer = onPrimaryContainerDark,
   secondary = secondaryDark,
   onSecondary = onSecondaryDark,
   secondaryContainer = secondaryContainerDark,
   onSecondaryContainer = onSecondaryContainerDark,
   tertiary = tertiaryDark,
   onTertiary = onTertiaryDark,
   tertiaryContainer = tertiaryContainerDark,
   onTertiaryContainer = onTertiaryContainerDark,
   error = errorDark,
   onError = onErrorDark,
   errorContainer = errorContainerDark,
   onErrorContainer = onErrorContainerDark,
   background = backgroundDark,
   onBackground = onBackgroundDark,
   surface = surfaceDark,
   onSurface = onSurfaceDark,
   surfaceVariant = surfaceVariantDark,
   onSurfaceVariant = onSurfaceVariantDark,
   outline = outlineDark,
   outlineVariant = outlineVariantDark,
   scrim = scrimDark,
   inverseSurface = inverseSurfaceDark,
   inverseOnSurface = inverseOnSurfaceDark,
   inversePrimary = inversePrimaryDark,
   surfaceDim = surfaceDimDark,
   surfaceBright = surfaceBrightDark,
   surfaceContainerLowest = surfaceContainerLowestDark,
   surfaceContainerLow = surfaceContainerLowDark,
   surfaceContainer = surfaceContainerDark,
   surfaceContainerHigh = surfaceContainerHighDark,
   surfaceContainerHighest = surfaceContainerHighestDark,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterTheme(
   darkTheme: Boolean = isSystemInDarkTheme(),
   // Dynamic color is available on Android 12+
   dynamicColor: Boolean = true,
   content: @Composable () -> Unit,
) {
   val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
         val context = LocalContext.current
         if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
   }

   MaterialTheme(
      colorScheme = colorScheme,
   ) {
      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
         // Workaround for https://issuetracker.google.com/issues/274471576 - Ripple effects do not work on Android 13
         val defaultRippleConfig = LocalRippleConfiguration.current ?: RippleConfiguration()
         val transparencyFixRippleConfiguration = RippleConfiguration(
            color = defaultRippleConfig.color,
            rippleAlpha = defaultRippleConfig.rippleAlpha?.run {
               RippleAlpha(
                  draggedAlpha,
                  focusedAlpha,
                  hoveredAlpha,
                  pressedAlpha.coerceAtLeast(MIN_RIPPLE_ALPHA_ON_TIRAMISU)
               )
            }

         )
         CompositionLocalProvider(LocalRippleConfiguration provides transparencyFixRippleConfiguration) {
            content()
         }
      } else {
         content()
      }
   }
}

private const val MIN_RIPPLE_ALPHA_ON_TIRAMISU = 0.5f
