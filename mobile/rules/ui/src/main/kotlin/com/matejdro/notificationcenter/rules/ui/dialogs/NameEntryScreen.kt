package com.matejdro.notificationcenter.rules.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogWithContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.compose.result.ResultKey
import si.inova.kotlinova.navigation.instructions.goBack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.DialogKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class NameEntryScreen(private val navigator: Navigator) : Screen<NameEntryScreenKey>() {
   @Composable
   override fun Content(key: NameEntryScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current

      NameEntryScreenContent(
         key,
         dismiss = { navigator.goBack() },
         accept = {
            navigator.goBack()
            resultPassingStore.sendResult(key.result, it)
         }
      )
   }
}

@Composable
private fun NameEntryScreenContent(
   key: NameEntryScreenKey,
   dismiss: () -> Unit,
   accept: (String) -> Unit,
) {
   val textFieldState = rememberTextFieldState(key.initialText)

   AlertDialogWithContent(
      title = {
         Text(text = key.title)
      },
      onDismissRequest = {
         dismiss()
      },
      confirmButton = {
         TextButton(
            onClick = {
               accept(textFieldState.text.toString())
            }
         ) {
            Text(stringResource(R.string.ok))
         }
      },
      dismissButton = {
         TextButton(
            onClick = {
               dismiss()
            }
         ) {
            Text(stringResource(R.string.cancel))
         }
      },
   ) {
      val focusRequester = remember { FocusRequester() }

      TextField(
         textFieldState,
         Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
         onKeyboardAction = { accept(textFieldState.text.toString()) },
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
         lineLimits = TextFieldLineLimits.SingleLine,
      )

      LaunchedEffect(Unit) {
         focusRequester.requestFocus()
      }
   }
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun NameEntryScreenPreview() {
   Dialog({}) {
      PreviewTheme {
         NameEntryScreenContent(
            NameEntryScreenKey("Enter text:", ResultKey(0), "Hello"),
            {},
            {}
         )
      }
   }
}

@Parcelize
data class NameEntryScreenKey(
   val title: String,
   val result: ResultKey<String>,
   val initialText: String = "",
) : ScreenKey(), DialogKey {
   @IgnoredOnParcel
   override val dialogProperties: DialogProperties = DialogProperties()
}
