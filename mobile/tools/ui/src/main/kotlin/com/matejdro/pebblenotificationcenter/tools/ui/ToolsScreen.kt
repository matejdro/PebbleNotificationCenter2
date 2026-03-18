package com.matejdro.pebblenotificationcenter.tools.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.navigation.keys.OnboardingKey
import com.matejdro.pebblenotificationcenter.navigation.keys.ToolsScreenKey
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.get
import com.matejdro.pebblenotificationcenter.tools.ui.dialogs.ActionOrderListScreenKey
import com.matejdro.pebblenotificationcenter.ui.components.ErrorAlertDialog
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.preferenceTheme
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.navigation.di.ContributesScreenBinding
import si.inova.kotlinova.navigation.instructions.navigateTo
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
@ContributesScreenBinding
class ToolsScreen(
   private val navigator: Navigator,
   private val viewModel: ToolsViewModel,
) : Screen<ToolsScreenKey>() {
   @Composable
   override fun Content(key: ToolsScreenKey) {
      val stateOutcome = viewModel.appVersion.collectAsStateWithLifecycle()
      val logSaveStatus = viewModel.logSave.collectAsStateWithLifecycleAndBlinkingPrevention().value

      ProgressErrorSuccessScaffold(
         stateOutcome::value,
         Modifier
            .fillMaxSize()
            .safeDrawingPadding()
      ) { state ->
         ToolsScreenContent(
            state = state,
            loggingTransmissionState = logSaveStatus,
            openPermissions = { navigator.navigateTo(OnboardingKey) },
            startLogSaving = viewModel::getLogs,
            notifyLogIntentSent = viewModel::resetLog,
            openActionOrderDialog = {
               navigator.navigateTo(ActionOrderListScreenKey)
            },
            updatePreference = { prefKey, value ->
               @Suppress("UNCHECKED_CAST")
               viewModel.updatePreference(prefKey as PreferenceKeyWithDefault<Any?>, value)
            }
         )
      }
   }
}

@Composable
private fun ToolsScreenContent(
   state: ToolsState,
   loggingTransmissionState: Outcome<Uri?>?,
   openPermissions: () -> Unit,
   startLogSaving: () -> Unit,
   notifyLogIntentSent: () -> Unit,
   openActionOrderDialog: () -> Unit,
   updatePreference: (PreferenceKeyWithDefault<*>, Any?) -> Unit,
) {
   CompositionLocalProvider(
      LocalPreferenceTheme provides preferenceTheme(
         verticalSpacing = 0.dp,
         padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      )
   ) {
      LazyVerticalGrid(
         GridCells.Adaptive(160.dp),
         horizontalArrangement = Arrangement.spacedBy(16.dp),
         verticalArrangement = Arrangement.spacedBy(16.dp),
         contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
         modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
      ) {
         item {
            ToolButton(onClick = openPermissions, icon = R.drawable.permissions, text = R.string.permissions)
         }

         item {
            ErrorAlertDialog(loggingTransmissionState)

            if (loggingTransmissionState is Outcome.Progress<*>) {
               CircularProgressIndicator(
                  Modifier
                     .size(60.dp)
                     .wrapContentWidth()
               )
            } else {
               ToolButton(onClick = startLogSaving, icon = R.drawable.logs, text = R.string.save_logs)

               val context = LocalContext.current
               SideEffect {
                  loggingTransmissionState?.data?.let { targetUri ->
                     val activityIntent = Intent(Intent.ACTION_SEND)
                     activityIntent.putExtra(Intent.EXTRA_STREAM, targetUri)
                     activityIntent.setType("application/zip")

                     activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                     activityIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                     context.startActivity(Intent.createChooser(activityIntent, null))
                     notifyLogIntentSent()
                  }
               }
            }
         }

         item(span = { GridItemSpan(maxLineSpan) }) {
            SwitchPreference(
               state.preferences[GlobalPreferenceKeys.muteWatch],
               onValueChange = {
                  updatePreference(GlobalPreferenceKeys.muteWatch, it)
               },
               title = { Text(stringResource(R.string.setting_mute_watch)) },
               summary = { Text(stringResource(R.string.setting_mute_watch_description)) }
            )
         }

         item(span = { GridItemSpan(maxLineSpan) }) {
            SwitchPreference(
               state.preferences[GlobalPreferenceKeys.mutePhone],
               onValueChange = {
                  updatePreference(GlobalPreferenceKeys.mutePhone, it)
               },
               title = { Text(stringResource(R.string.setting_mute_phone)) },
               summary = { Text(stringResource(R.string.setting_mute_phone_description)) }
            )
         }

         item(span = { GridItemSpan(maxLineSpan) }) {
            Preference(
               title = { Text(stringResource(R.string.setting_action_order)) },
               summary = { Text(stringResource(R.string.setting_action_order_description)) },
               onClick = openActionOrderDialog
            )
         }

         item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
               stringResource(R.string.version, state.versionName),
               Modifier
                  .fillMaxWidth()
                  .wrapContentWidth(Alignment.CenterHorizontally)
            )
         }
      }
   }
}

@Composable
private fun ToolButton(onClick: () -> Unit, icon: Int, text: Int) {
   Button(onClick = onClick, Modifier.sizeIn(minHeight = 60.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
         Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
         )

         Text(stringResource(text))
      }
   }
}

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun ToolsScreenPreview() {
   PreviewTheme {
      ToolsScreenContent(
         state = ToolsState("1.0.0-alpha07", emptyPreferences()),
         loggingTransmissionState = Outcome.Success(null),
         openPermissions = {},
         startLogSaving = {},
         notifyLogIntentSent = {},
         openActionOrderDialog = {},
         updatePreference = { _, _ -> },
      )
   }
}
