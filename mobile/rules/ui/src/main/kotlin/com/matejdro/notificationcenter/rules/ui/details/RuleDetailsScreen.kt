package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.notificationcenter.rules.ui.errors.ruleUserFriendlyMessage
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.ui.animations.LocalSharedTransitionScope
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.instructions.goBack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@InjectNavigationScreen
class RuleDetailsScreen(
   private val viewModel: RuleDetailsViewModel,
   private val navigator: Navigator,
) : Screen<RuleDetailsScreenKey>() {
   @Composable
   override fun Content(key: RuleDetailsScreenKey) {
      val windowSizeClass = calculateWindowSizeClass(LocalContext.current.requireActivity())

      val stateOutcome by viewModel.uiState.collectAsState()

      var showDeleteConfirmation by remember { mutableStateOf(false) }
      if (showDeleteConfirmation) {
         AlertDialog(
            title = { Text(stringResource(R.string.delete_rule)) },
            text = {
               Text(
                  stringResource(
                     R.string.delete_confirmation_text,
                     stateOutcome.data?.ruleMetadata?.name.orEmpty()
                  )
               )
            },

            onDismissRequest = { showDeleteConfirmation = false },
            confirmButton = {
               TextButton(onClick = {
                  showDeleteConfirmation = false
                  viewModel.deleteRule()
                  navigator.goBack()
               }) {
                  Text(stringResource(R.string.delete_rule))
               }
            },
            dismissButton = {
               TextButton(onClick = { showDeleteConfirmation = false }) {
                  Text(stringResource(R.string.cancel))
               }
            }
         )
      }

      ProgressErrorSuccessScaffold(
         stateOutcome,
         Modifier.safeDrawingPadding(),
         errorText = { it.ruleUserFriendlyMessage() }
      ) {
         RuleDetailsScreenContent(
            it,
            windowSizeClass.widthSizeClass,
            delete = {
               showDeleteConfirmation = true
            }
         )
      }
   }
}

@OptIn(
   ExperimentalMaterial3Api::class,
   ExperimentalSharedTransitionApi::class,
)
@Composable
private fun RuleDetailsScreenContent(
   state: RuleDetailsScreenState,
   windowSizeClass: WindowWidthSizeClass,
   delete: () -> Unit,
) = with(LocalSharedTransitionScope.current) {
   val largeDevice: Boolean = windowSizeClass != WindowWidthSizeClass.Compact

   Column(
      Modifier
         .fillMaxWidth()
         .verticalScroll(state = rememberScrollState())
         .safeDrawingPadding()
   ) {
      TopAppBar(
         title = {
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
               },
            )
         },
         actions = {
            IconButton(onClick = delete) {
               Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete_rule))
            }
         }
      )
   }
}

@FullScreenPreviews
@ShowkaseComposable(group = "test")
@Composable
internal fun RuleDetailsScreenContentPreview() {
   PreviewTheme {
      RuleDetailsScreenContent(
         state = RuleDetailsScreenState(RuleMetadata(2, "Test Rule")),
         windowSizeClass = WindowWidthSizeClass.Compact,
         {},
      )
   }
}
