package com.matejdro.pebblenotificationcenter.rules.ui.dialogs

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import kotlinx.serialization.Serializable
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.compose.result.ResultKey
import si.inova.kotlinova.navigation.instructions.goBack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.DialogKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.pebblenotificationcenter.sharedresources.R as sharedR

@InjectNavigationScreen
class NameEntryScreen(private val navigator: Navigator) : Screen<NameEntryScreenKey>() {
   @Composable
   override fun Content(key: NameEntryScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current

      NameEntryScreenContent(
         key,
         dismiss = { navigator.goBack() },
         accept = { name ->
            navigator.goBack()
            resultPassingStore.sendResult(key.result, NameEntryScreenKey.Result.Text(name))
         },
         thirdButtonClick = {
            navigator.goBack()
            resultPassingStore.sendResult(key.result, NameEntryScreenKey.Result.ThirdButtonClicked)
         }
      )
   }
}

@Composable
private fun NameEntryScreenContent(
   key: NameEntryScreenKey,
   dismiss: () -> Unit,
   accept: (String) -> Unit,
   thirdButtonClick: () -> Unit,
) {
   val textFieldState = rememberTextFieldState(key.initialText, initialSelection = TextRange(0, key.initialText.length))

   AlertDialogInnerContent(
      title = {
         Text(text = key.title)
      },
      dismissButton = {
         TextButton(
            onClick = {
               dismiss()
            }
         ) {
            Text(stringResource(sharedR.string.cancel))
         }
      },
      confirmButton = {
         TextButton(
            onClick = {
               accept(textFieldState.text.toString())
            },
            enabled = textFieldState.text.isNotBlank()
         ) {
            Text(stringResource(sharedR.string.ok))
         }
      },
      neutralButton = {
         key.thirdButtonText?.let { thirdButtonText ->
            TextButton(
               onClick = thirdButtonClick,
            ) {
               Text(thirdButtonText)
            }
         }
      },
      content = {
         val focusRequester = remember { FocusRequester() }

         TextField(
            textFieldState,
            Modifier
               .fillMaxWidth()
               .focusRequester(focusRequester),
            onKeyboardAction = { accept(textFieldState.text.toString()) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = key.enableAutocorrect),
            lineLimits = TextFieldLineLimits.SingleLine,
         )

         LaunchedEffect(Unit) {
            focusRequester.requestFocus()
         }
      },
   )
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
@SuppressLint("VisibleForTests") // Previews are sort of tests
internal fun NameEntryScreenPreview() {
   PreviewTheme {
      Box {
         NameEntryScreenContent(
            NameEntryScreenKey("Enter text:", ResultKey(0, 0), "Hello"),
            {},
            {},
            {},
         )
      }
   }
}

@ShowkaseComposable(group = "test")
@Composable
@Preview
internal fun NameEntryScreenWithThirdButtonPreview() {
   PreviewTheme {
      Box {
         NameEntryScreenContent(
            NameEntryScreenKey(
               "Enter text:",
               ResultKey(0, 0),
               "Hello",
               thirdButtonText = "Delete"
            ),
            {},
            {},
            {},
         )
      }
   }
}

@ShowkaseComposable(group = "test")
@Composable
@Preview
internal fun NameEntryScreenBlankPreview() {
   PreviewTheme {
      Box {
         NameEntryScreenContent(
            NameEntryScreenKey(
               "Enter text:",
               ResultKey(0, 0),
               "",
            ),
            {},
            {},
            {},
         )
      }
   }
}

@Serializable
data class NameEntryScreenKey(
   val title: String,
   val result: ResultKey<Result>,
   val initialText: String = "",
   val thirdButtonText: String? = null,
   val enableAutocorrect: Boolean = true,
) : ScreenKey(), DialogKey {

   @Serializable
   sealed class Result {
      @Serializable
      data object ThirdButtonClicked : Result()

      @Serializable
      data class Text(val text: String) : Result()
   }
}
