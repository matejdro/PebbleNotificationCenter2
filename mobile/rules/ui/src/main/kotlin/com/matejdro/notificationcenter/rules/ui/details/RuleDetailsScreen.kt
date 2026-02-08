package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.ui.errors.ruleUserFriendlyMessage
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.ui.animations.LocalSharedTransitionScope
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class RuleDetailsScreen(
   private val viewModel: RuleDetailsViewModel,
) : Screen<RuleDetailsScreenKey>() {
   @Composable
   override fun Content(key: RuleDetailsScreenKey) {
      val stateOutcome by viewModel.uiState.collectAsState()
      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier.safeDrawingPadding(),
         errorText = { it.ruleUserFriendlyMessage() }
      ) {
         RuleDetailsScreenContent(it)
      }
   }
}

@OptIn(
   ExperimentalMaterial3Api::class,
   ExperimentalSharedTransitionApi::class,
   ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
private fun RuleDetailsScreenContent(
   state: RuleDetailsScreenState,
) = with(LocalSharedTransitionScope.current) {
   val windowSizeClass = calculateWindowSizeClass(LocalContext.current.requireActivity())
   val largeDevice: Boolean = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

   Column(
      Modifier
         .fillMaxWidth()
         .verticalScroll(state = rememberScrollState())
         .safeDrawingPadding()
   ) {
      TopAppBar(title = {
         Text(
            state.ruleMetadata.name,
            Modifier.run {
               if (!largeDevice) {
                  sharedElement(
                     rememberSharedContentState("ruleName-${state.ruleMetadata.id}"),
                     LocalNavAnimatedContentScope.current,
                  )
               } else {
                  this
               }
            }
         )
      })
   }
}

@FullScreenPreviews
@Composable
private fun RuleDetailsScreenContentPreview() {
   PreviewTheme {
      RuleDetailsScreenContent(
         state = RuleDetailsScreenState(RuleMetadata(2, "Test Rule"))
      )
   }
}
