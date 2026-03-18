package com.matejdro.pebblenotificationcenter.rules.ui.details

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.pebblenotificationcenter.rules.RuleMetadata
import com.matejdro.pebblenotificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.SetPreference
import com.matejdro.pebblenotificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.rules.ui.errors.ruleUserFriendlyMessage
import com.matejdro.pebblenotificationcenter.ui.animations.LocalSharedTransitionScope
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.core.activity.requireActivity
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

      val stateOutcome = viewModel.uiState.collectAsState()

      val renameDialog = renameDialog(navigator, { viewModel.renameRule(it) })
      val copyDialog = copyDialog(navigator, { viewModel.copyRule(it) })
      val addRegexDialog = addRegexDialog(navigator, viewModel::addRegex)
      val editRegexDialog = editRegexDialog(navigator, viewModel::editRegex)

      val appPickerDialog = appPickingDialog(
         navigator,
         { pkg, channels -> viewModel.changeTargetApp(pkg, channels) },
      )

      var showDeleteConfirmation by remember { mutableStateOf(false) }
      if (showDeleteConfirmation) {
         DeleteDialog(
            stateOutcome = stateOutcome.value,
            dismiss = { showDeleteConfirmation = false },
            delete = {
               viewModel.deleteRule(
                  largeScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
               )
            }
         )
      }

      ProgressErrorSuccessScaffold(
         stateOutcome::value,
         Modifier.safeDrawingPadding(),
         errorText = { it.ruleUserFriendlyMessage() }
      ) { state ->
         RuleDetailsScreenContent(
            state,
            windowSizeClass.widthSizeClass,
            rename = {
               renameDialog.trigger(state.ruleMetadata.name)
            },
            copy = {
               copyDialog.trigger(state.ruleMetadata.name)
            },
            delete = {
               showDeleteConfirmation = true
            },
            changeTargetApp = {
               appPickerDialog.trigger(true)
            },
            setPreference = SetPreference { prefKey, value ->
               @Suppress("UNCHECKED_CAST")
               viewModel.updatePreference(prefKey as PreferenceKeyWithDefault<Any?>, value)
            },
            changeRegex = { index, whitelist ->
               val regexText = if (whitelist) {
                  state.whitelistRegexes.elementAtOrNull(index)
               } else {
                  state.blacklistRegexes.elementAtOrNull(index)
               } ?: return@RuleDetailsScreenContent

               editRegexDialog.trigger(EditRegexData(whitelist, regexText, index))
            },
            addRegex = addRegexDialog::trigger,
            navigator = navigator,
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
   navigator: Navigator,
   delete: () -> Unit,
   rename: () -> Unit,
   copy: () -> Unit,
   changeTargetApp: () -> Unit,
   setPreference: SetPreference,
   changeRegex: (index: Int, whitelist: Boolean) -> Unit,
   addRegex: (whitelist: Boolean) -> Unit,
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
            if (state.ruleMetadata.id != RULE_ID_DEFAULT_SETTINGS) {
               IconButton(onClick = rename) {
                  Icon(painterResource(R.drawable.ic_rename), contentDescription = stringResource(R.string.rename_rule))
               }

               IconButton(onClick = copy) {
                  Icon(painterResource(R.drawable.ic_copy), contentDescription = stringResource(R.string.copy_rule))
               }

               IconButton(onClick = delete) {
                  Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete_rule))
               }
            }
         }
      )

      Conditions(state, changeTargetApp, changeRegex, addRegex)

      HorizontalDivider(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 16.dp))

      Settings(navigator, state.preferences, setPreference)
   }
}

@FullScreenPreviews
@ShowkaseComposable(group = "test")
@Composable
internal fun RuleDetailsScreenContentPreview() {
   PreviewTheme {
      RuleDetailsScreenContent(
         state = RuleDetailsScreenState(RuleMetadata(2, "Test Rule"), emptyPreferences()),
         windowSizeClass = WindowWidthSizeClass.Compact,
         {},
         {},
         {},
         {},
         {},
         SetPreference { _, _ -> },
         { _, _ -> },
         {},
      )
   }
}
