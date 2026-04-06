package com.matejdro.pebblenotificationcenter.rules.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import com.matejdro.pebblenotificationcenter.ui.lists.ReorderableListContainer
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
class IntListScreen(private val navigator: Navigator) : Screen<IntListScreenKey>() {
   @Composable
   override fun Content(key: IntListScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current
      var list by remember { mutableStateOf(key.initialList.withIndex().toList()) }

      NumberListScreenContent(
         key.title,
         list,
         addNew = { list += IndexedValue(list.size, it) },
         delete = { removeIndex ->
            list = list.filterIndexed { index, _ -> index != removeIndex }.mapIndexed { index, (_, value) ->
               IndexedValue(index, value)
            }
         },
         dismiss = { navigator.goBack() },
         move = { from, to ->
            val existing = list.first { it.index == from }
            list = list.toMutableList().apply {
               remove(existing)
               add(to, existing)
            }.mapIndexed { index, value ->
               IndexedValue(index, value.value)
            }
         },
         accept = {
            navigator.goBack()
            resultPassingStore.sendResult(key.result, list.map { it.value })
         },
         allowEmptyList = key.allowEmptyList,
      )
   }
}

@Composable
private fun NumberListScreenContent(
   title: String,
   list: List<IndexedValue<Int>>,
   addNew: (Int) -> Unit,
   delete: (Int) -> Unit,
   dismiss: () -> Unit,
   accept: () -> Unit,
   move: (from: Int, to: Int) -> Unit,
   allowEmptyList: Boolean,
   showTextField: Boolean = false,
) {
   val textFieldState = rememberTextFieldState("")
   val addTextFieldShown = remember { mutableStateOf(showTextField) }

   AlertDialogInnerContent(
      title = {
         Text(text = title)
      },
      dismissButton = {
         TextButton(
            onClick = {
               if (addTextFieldShown.value) {
                  textFieldState.clearText()
                  addTextFieldShown.value = false
               } else {
                  dismiss()
               }
            }
         ) {
            Text(stringResource(sharedR.string.cancel))
         }
      },
      confirmButton = {
         TextButton(
            onClick = {
               if (addTextFieldShown.value) {
                  addNew(textFieldState.text.toString().toInt())
                  textFieldState.clearText()
                  addTextFieldShown.value = false
               } else {
                  accept()
               }
            },
            enabled = if (addTextFieldShown.value) {
               textFieldState.text.isNotBlank()
            } else {
               allowEmptyList || list.isNotEmpty()
            }
         ) {
            Text(stringResource(sharedR.string.ok))
         }
      },
      content = {
         val listState = rememberLazyListState()
         ReorderableListContainer(list, listState) { shownList ->

            LazyColumn(
               Modifier
                  .fillMaxWidth(),
               state = listState,
            ) {
               item(key = "dummy") {
                  // If user attempts to drag the first item, Lazy List will attempt to scroll down to keep
                  // the list scroll position the same (first item at the same position on the screen)
                  // To counteract that, we add a dummy first item to the start of the list, so the list
                  // hooks on that item instead of on the ordered item

                  Spacer(
                     Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                  )
               }

               items(shownList, key = { it.index }) { entry ->
                  ReorderableListItem(
                     entry.index,
                     entry,
                     move,
                     popupBackground = MaterialTheme.colorScheme.surfaceContainerHigh
                  ) { modifier, _ ->
                     Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
                        Text("•", Modifier.padding(end = 8.dp))

                        Text(
                           entry.value.toString(),
                           modifier = Modifier
                              .weight(1f)
                              .padding(end = 8.dp),
                        )

                        Button(onClick = { delete(entry.index) }) {
                           Icon(
                              painterResource(sharedR.drawable.ic_delete),
                              contentDescription = stringResource(R.string.delete_rule)
                           )
                        }
                     }
                  }
               }

               if (addTextFieldShown.value) {
                  item {
                     val focusRequester = remember { FocusRequester() }

                     TextField(
                        textFieldState,
                        Modifier
                           .fillMaxWidth()
                           .focusRequester(focusRequester),
                        onKeyboardAction = {
                           addNew(textFieldState.text.toString().toInt())
                           textFieldState.clearText()
                           addTextFieldShown.value = false
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        inputTransformation = {
                           if (!asCharSequence().isDigitsOnly()) {
                              revertAllChanges()
                           }
                        },
                     )

                     LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                     }
                  }
               } else {
                  item {
                     Button(onClick = { addTextFieldShown.value = true }) {
                        Icon(painterResource(sharedR.drawable.ic_add), contentDescription = stringResource(sharedR.string.add))
                     }
                  }
               }
            }
         }
      },
   )
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun NumberListScreenPreview() {
   PreviewTheme {
      Box {
         NumberListScreenContent(
            "Options",
            listOf(10, 20, 30).mapIndexed { index, value ->
               IndexedValue(index, value)
            },
            { },
            {},
            {},
            {},
            { _, _ -> },
            false,
         )
      }
   }
}

@ShowkaseComposable(group = "test")
@Composable
@Preview
internal fun NumberListAddFieldPreview() {
   PreviewTheme {
      Box {
         NumberListScreenContent(
            "Options",
            listOf(10, 20, 30).mapIndexed { index, value ->
               IndexedValue(index, value)
            },
            { },
            {},
            {},
            {},
            { _, _ -> },
            false,
            showTextField = true
         )
      }
   }
}

@Serializable
data class IntListScreenKey(
   val title: String,
   val initialList: List<Int>,
   val result: ResultKey<List<Int>>,
   val allowEmptyList: Boolean = false,
) : ScreenKey(), DialogKey
