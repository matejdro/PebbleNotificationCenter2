package com.matejdro.notificationcenter.rules.ui.details

import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.notificationcenter.rules.ui.dialogs.AppSelectionScreenKey
import com.matejdro.notificationcenter.rules.ui.dialogs.ChannelSelectionScreenKey
import com.matejdro.notificationcenter.rules.ui.dialogs.NameEntryScreenKey
import com.matejdro.pebblenotificationcenter.navigation.util.PopupTrigger
import com.matejdro.pebblenotificationcenter.navigation.util.rememberNavigationPopup
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.navigation.navigator.Navigator

@Composable
internal fun DeleteDialog(
   stateOutcome: Outcome<RuleDetailsScreenState>,
   dismiss: () -> Unit,
   delete: () -> Unit,
) {
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

      onDismissRequest = { dismiss() },
      confirmButton = {
         TextButton(onClick = {
            dismiss()
            delete()
         }) {
            Text(stringResource(R.string.delete_rule))
         }
      },
      dismissButton = {
         TextButton(onClick = { dismiss() }) {
            Text(stringResource(R.string.cancel))
         }
      }
   )
}

@Composable
internal fun appPickingDialog(
   navigator: Navigator,
   changeTargetApp: (String, List<String>) -> Unit,
): PopupTrigger<Unit> {
   var lastSelectedPkg by remember { mutableStateOf<String?>(null) }

   val channelPickerDialog = key("channels") {
      navigator.rememberNavigationPopup(
         navigationKey = { pkg: String, resultKey ->
            ChannelSelectionScreenKey(pkg, resultKey)
         },
         onResult = { channels ->
            lastSelectedPkg?.let { pkg -> changeTargetApp(pkg, channels) }
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
               changeTargetApp(it, emptyList())
            }
         }
      )
   }

   return appPickerDialog
}

@Composable
internal fun renameDialog(navigator: Navigator, acceptName: (String) -> Unit): PopupTrigger<String> = key("rename") {
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
            acceptName(it)
         }
      }
   )
}
