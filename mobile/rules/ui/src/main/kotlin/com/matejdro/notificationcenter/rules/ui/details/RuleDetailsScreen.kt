package com.matejdro.notificationcenter.rules.ui.details

import android.os.Build
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.notificationcenter.rules.ui.dialogs.AppSelectionScreenKey
import com.matejdro.notificationcenter.rules.ui.dialogs.ChannelSelectionScreenKey
import com.matejdro.notificationcenter.rules.ui.dialogs.NameEntryScreenKey
import com.matejdro.notificationcenter.rules.ui.errors.ruleUserFriendlyMessage
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.navigation.util.rememberNavigationPopup
import com.matejdro.pebblenotificationcenter.navigation.util.trigger
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

      val renameDialog = key("rename") {
         navigator.rememberNavigationPopup(
            navigationKey = { oldName: String, resultKey ->
               NameEntryScreenKey(
                  getString(R.string.rename_rule),
                  resultKey,
                  initialText = oldName
               )
            },
            onResult = {
               if (!it.isBlank()) {
                  viewModel.renameRule(it)
               }
            }
         )
      }
      var lastSelectedPkg by remember { mutableStateOf<String?>(null) }

      val channelPickerDialog = key("channels") {
         navigator.rememberNavigationPopup(
            navigationKey = { pkg: String, resultKey ->
               ChannelSelectionScreenKey(pkg, resultKey)
            },
            onResult = { channels ->
               lastSelectedPkg?.let { pkg -> viewModel.changeTargetApp(pkg, channels) }
            }
         )
      }

      val appPickerDialog = key("apps") {
         navigator.rememberNavigationPopup(
            navigationKey = { _: Unit, resultKey ->
               AppSelectionScreenKey(resultKey)
            },
            onResult = {
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  lastSelectedPkg = it
                  channelPickerDialog.trigger(it)
               } else {
                  viewModel.changeTargetApp(it, emptyList())
               }
            }
         )
      }

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
            rename = {
               renameDialog.trigger(stateOutcome.data?.ruleMetadata?.name.orEmpty())
            },
            delete = {
               showDeleteConfirmation = true
            },
            changeTargetApp = {
               appPickerDialog.trigger()
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
   rename: () -> Unit,
   changeTargetApp: () -> Unit,
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
            IconButton(onClick = rename) {
               Icon(painterResource(R.drawable.ic_rename), contentDescription = stringResource(R.string.rename_rule))
            }

            IconButton(onClick = delete) {
               Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete_rule))
            }
         }
      )

      Text(
         stringResource(R.string.conditions),
         style = MaterialTheme.typography.headlineMedium,
         modifier = Modifier.padding(16.dp)
      )

      if (state.targetAppName == null) {
         Text(stringResource(R.string.no_conditions), modifier = Modifier.padding(horizontal = 16.dp))
      } else {
         Text(
            stringResource(R.string.conditions_header),
            modifier = Modifier.padding(horizontal = 16.dp)
         )
         val appText = buildAnnotatedString {
            append(stringResource(R.string.app_condition_prefix))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
               append(state.targetAppName)
            }
         }
         Text(appText, modifier = Modifier.padding(horizontal = 16.dp))

         val channels = state.targetChannelNames
         if (channels.isNotEmpty()) {
            val channelsText = buildAnnotatedString {
               if (channels.size == 1) {
                  append(stringResource(R.string.single_channel_condition_prefix))
               } else {
                  append(stringResource(R.string.multi_channel_condition_prefix))
               }

               channels.forEachIndexed { index, channel ->
                  withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                     append(channel)
                  }

                  if (index != channels.lastIndex) {
                     append(", ")
                  }
               }
            }
            Text(channelsText, modifier = Modifier.padding(horizontal = 16.dp))
         }
      }

      if (state.ruleMetadata.id != RULE_ID_DEFAULT_SETTINGS) {
         FlowRow(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = changeTargetApp) { Text(stringResource(R.string.change_target_app)) }
         }
      }
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
      )
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun RuleDetailsScreenWithTargetAppPreview() {
   PreviewTheme {
      RuleDetailsScreenContent(
         state = RuleDetailsScreenState(
            ruleMetadata = RuleMetadata(2, "Test Rule"),
            preferences = emptyPreferences(),
            targetAppName = "My super app",
            targetChannelNames = emptyList()
         ),
         windowSizeClass = WindowWidthSizeClass.Compact,
         {},
         {},
         {},
      )
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun RuleDetailsScreenWithTargetAppAndChannelsPreview() {
   PreviewTheme {
      RuleDetailsScreenContent(
         state = RuleDetailsScreenState(
            ruleMetadata = RuleMetadata(2, "Test Rule"),
            preferences = emptyPreferences(),
            targetAppName = "My super app",
            targetChannelNames = listOf("Channel 1", "Channel 2", "Channel 3")
         ),
         windowSizeClass = WindowWidthSizeClass.Compact,
         {},
         {},
         {},
      )
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun RuleDetailsScreenWithTargetAppAndSingleChannelPreview() {
   PreviewTheme {
      RuleDetailsScreenContent(
         state = RuleDetailsScreenState(
            ruleMetadata = RuleMetadata(2, "Test Rule"),
            preferences = emptyPreferences(),
            targetAppName = "My super app",
            targetChannelNames = listOf("Channel 1")
         ),
         windowSizeClass = WindowWidthSizeClass.Compact,
         {},
         {},
         {},
      )
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun RuleDetailsScreenWithDefaultSettingsPreview() {
   PreviewTheme {
      RuleDetailsScreenContent(
         state = RuleDetailsScreenState(
            ruleMetadata = RuleMetadata(1, "Default Settings"),
            preferences = emptyPreferences(),
         ),
         windowSizeClass = WindowWidthSizeClass.Compact,
         {},
         {},
         {},
      )
   }
}
