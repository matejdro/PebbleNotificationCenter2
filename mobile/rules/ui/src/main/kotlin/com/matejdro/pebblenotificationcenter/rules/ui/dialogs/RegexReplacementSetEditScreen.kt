package com.matejdro.pebblenotificationcenter.rules.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.rules.ui.R
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
class RegexReplacementSetScreen(private val navigator: Navigator) : Screen<RegexReplacementSetScreenKey>() {
   @Composable
   override fun Content(key: RegexReplacementSetScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current
      var list by remember { mutableStateOf(key.initialList.toList()) }

      RegexReplacementSetScreenContent(
         list,
         addNew = { list += it },
         delete = { removeIndex ->
            list = list.filterIndexed { index, _ -> index != removeIndex }
         },
         dismiss = { navigator.goBack() },
         accept = {
            navigator.goBack()
            resultPassingStore.sendResult(key.result, list.toSet())
         },
      )
   }
}

@Composable
private fun RegexReplacementSetScreenContent(
   list: List<Pair<String, String>>,
   addNew: (Pair<String, String>) -> Unit,
   delete: (Int) -> Unit,
   dismiss: () -> Unit,
   accept: () -> Unit,
) {
   var addDialogActive by remember { mutableStateOf(false) }

   if (addDialogActive) {
      AddNewDialog(
         cancel = { addDialogActive = false },
         confirm = { regexPair ->
            addDialogActive = false
            addNew(regexPair)
         }
      )
   }

   AlertDialogInnerContent(
      title = {
         Text(text = stringResource(R.string.preference_regex_replacement))
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
               accept()
            },
         ) {
            Text(stringResource(sharedR.string.ok))
         }
      },
      content = {
         val listState = rememberLazyListState()
         LazyColumn(
            Modifier
               .fillMaxWidth(),
            state = listState,
         ) {
            itemsIndexed(list) { index, entry ->
               Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                  Text("•", Modifier.padding(end = 8.dp))

                  SelectionContainer {
                     Column(
                        Modifier
                           .weight(1f)
                           .padding(end = 8.dp)
                     ) {
                        Text(entry.first)
                        Text(entry.second)
                     }
                  }

                  Button(onClick = { delete(index) }) {
                     Icon(
                        painterResource(sharedR.drawable.ic_delete),
                        contentDescription = stringResource(com.matejdro.pebblenotificationcenter.sharedresources.R.string.delete)
                     )
                  }
               }
            }

            item {
               Button(onClick = { addDialogActive = true }) {
                  Icon(painterResource(sharedR.drawable.ic_add), contentDescription = stringResource(sharedR.string.add))
               }
            }
         }
      },
   )
}

@Composable
private fun AddNewDialog(
   cancel: () -> Unit,
   confirm: (Pair<String, String>) -> Unit,
) {
   val from = rememberTextFieldState()
   val to = rememberTextFieldState()

   AlertDialog(
      cancel,
      dismissButton = {
         TextButton(
            onClick = {
               cancel()
            }
         ) {
            Text(stringResource(sharedR.string.cancel))
         }
      },
      confirmButton = {
         TextButton(
            onClick = {
               confirm(from.text.toString() to to.text.toString())
            },
         ) {
            Text(stringResource(sharedR.string.add))
         }
      },
      title = { Text(stringResource(sharedR.string.add)) },
      text = {
         Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
         ) {
            Text(stringResource(R.string.preference_regex_replacement_add_from))
            TextField(from, Modifier.fillMaxWidth())
            Text(stringResource(R.string.preference_regex_replacement_add_to))
            TextField(to, Modifier.fillMaxWidth())
            Text(stringResource(R.string.preference_regex_replacement_add_groups))
         }
      }
   )
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun RegexReplacementSetScreenPreview() {
   PreviewTheme {
      Box {
         RegexReplacementSetScreenContent(
            listOf(
               "Replace A" to "With A",
               "Long long replace B" to "Long long with B"
            ),
            {},
            {},
            { },
            {},
         )
      }
   }
}

@ShowkaseComposable(group = "test")
@Composable
@Preview
internal fun RegexReplacementSetScreenAddPreview() {
   PreviewTheme {
      AddNewDialog({}, {})
   }
}

@Serializable
data class RegexReplacementSetScreenKey(
   val initialList: Set<Pair<String, String>>,
   val result: ResultKey<Set<Pair<String, String>>>,
) : ScreenKey(), DialogKey
