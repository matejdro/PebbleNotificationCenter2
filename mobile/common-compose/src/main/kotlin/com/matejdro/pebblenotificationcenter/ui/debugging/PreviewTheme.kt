package com.matejdro.pebblenotificationcenter.ui.debugging

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.matejdro.pebblenotificationcenter.ui.animations.LocalSharedTransitionScope
import com.matejdro.pebblenotificationcenter.ui.theme.NotificationCenterTheme
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.compose.result.ResultPassingStore
import si.inova.kotlinova.compose.time.ComposeAndroidDateTimeFormatter
import si.inova.kotlinova.compose.time.LocalDateFormatter
import si.inova.kotlinova.core.time.AndroidDateTimeFormatter
import si.inova.kotlinova.core.time.FakeAndroidDateTimeFormatter

@Composable
@Suppress("ModifierMissing") // This is intentional
// AnimatedContent is only used to provide its scope
@SuppressLint("UnusedContentLambdaTargetStateParameter", "UnusedSharedTransitionModifierParameter")
fun PreviewTheme(
   formatter: AndroidDateTimeFormatter = FakeAndroidDateTimeFormatter(),
   fill: Boolean = true,
   content: @Composable () -> Unit,
) {
   AnimatedContent(Unit) {
      SharedTransitionScope {
         CompositionLocalProvider(
            LocalDateFormatter provides ComposeAndroidDateTimeFormatter(formatter),
            LocalSharedTransitionScope provides this,
            LocalNavAnimatedContentScope provides this@AnimatedContent,
            LocalResultPassingStore provides ResultPassingStore(),
         ) {
            // Disable Material You on previews (and screenshot tests) to improve reproducibility
            NotificationCenterTheme(dynamicColor = false) {
               Surface(modifier = if (fill) Modifier.fillMaxSize() else Modifier, content = content)
            }
         }
      }
   }
}
