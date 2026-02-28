package com.matejdro.notificationcenter.rules.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
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
class StringListScreen(private val navigator: Navigator) : Screen<StringListScreenKey>() {
   @Composable
   override fun Content(key: StringListScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current
      var list by remember { mutableStateOf(key.initialList) }

      StringListScreenContent(
         key.title,
         list,
         addNew = { list += it },
         delete = { removeIndex -> list = list.filterIndexed { index, _ -> index != removeIndex } },
         dismiss = { navigator.goBack() },
         accept = {
            navigator.goBack()
            resultPassingStore.sendResult(key.result, list)
         },
      )
   }
}

@Composable
private fun StringListScreenContent(
   title: String,
   list: List<String>,
   addNew: (String) -> Unit,
   delete: (Int) -> Unit,
   dismiss: () -> Unit,
   accept: () -> Unit,
   showTextField: Boolean = false,
) {
   val textFieldState = rememberTextFieldState("")
   var addTextFieldShown by remember { mutableStateOf(showTextField) }

   AlertDialogInnerContent(
      title = {
         Text(text = title)
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
      confirmButton = {
         TextButton(
            onClick = {
               accept()
            },
            enabled = textFieldState.text.isNotBlank()
         ) {
            Text(stringResource(R.string.ok))
         }
      },
      content = {
         Column(
            Modifier
               .fillMaxWidth()
               .verticalScroll(rememberScrollState())
         ) {
            list.forEachIndexed { index, entry ->
               Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                  Text("•", Modifier.padding(end = 8.dp))

                  Text(entry, modifier = Modifier.padding(end = 8.dp))
                  Spacer(Modifier.weight(1f))

                  Button(onClick = { delete(index) }) {
                     Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete_rule))
                  }
               }
            }

            if (addTextFieldShown) {
               val focusRequester = remember { FocusRequester() }

               TextField(
                  textFieldState,
                  Modifier
                     .fillMaxWidth()
                     .focusRequester(focusRequester),
                  onKeyboardAction = {
                     addNew(textFieldState.text.toString())
                     addTextFieldShown = false
                  },
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                  lineLimits = TextFieldLineLimits.SingleLine,
               )

               LaunchedEffect(Unit) {
                  focusRequester.requestFocus()
               }
            } else {
               Button(onClick = { addTextFieldShown = true }) {
                  Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add))
               }
            }
         }
      },
   )
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun StringListScreenPreview() {
   PreviewTheme {
      Box {
         StringListScreenContent(
            "Options",
            listOf("Option A", "Option B", "Option C"),
            { },
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
internal fun StringListAddFieldPreview() {
   PreviewTheme {
      Box {
         StringListScreenContent(
            "Options",
            listOf("Option A", "Option B", "Option C"),
            { },
            {},
            {},
            {},
            showTextField = true
         )
      }
   }
}

@Parcelize
data class StringListScreenKey(
   val title: String,
   val initialList: List<String>,
   val result: ResultKey<List<String>>,
) : ScreenKey(), DialogKey {
   @IgnoredOnParcel
   override val dialogProperties: DialogProperties = DialogProperties()
}
