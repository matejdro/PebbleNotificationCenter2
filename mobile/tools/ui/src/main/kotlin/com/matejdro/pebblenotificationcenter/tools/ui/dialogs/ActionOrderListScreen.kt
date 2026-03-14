package com.matejdro.pebblenotificationcenter.tools.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.notification.ActionOrderRepository
import com.matejdro.pebblenotificationcenter.tools.ui.R
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import com.matejdro.pebblenotificationcenter.ui.lists.ReorderableListContainer
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import si.inova.kotlinova.navigation.instructions.goBack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.DialogKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.pebblenotificationcenter.sharedresources.R as sharedR

@InjectNavigationScreen
class ActionOrderListScreen(
   private val navigator: Navigator,
   private val actionOrderRepository: ActionOrderRepository,
) : Screen<ActionOrderListScreenKey>() {
   @Composable
   override fun Content(key: ActionOrderListScreenKey) {
      val list = actionOrderRepository.getList().collectAsState(initial = emptyList()).value

      val scope = rememberCoroutineScope()
      ActionOrderListScreenContent(
         list,
         dismiss = { navigator.goBack() },
         move = { value, to ->
            scope.launch {
               actionOrderRepository.moveOrder(value, to)
            }
         },
      )
   }
}

@Composable
private fun ActionOrderListScreenContent(
   list: List<String>,
   dismiss: () -> Unit,
   move: (value: String, to: Int) -> Unit,
) {
   AlertDialogInnerContent(
      title = {
         Text(text = stringResource(R.string.setting_action_order))
      },
      confirmButton = {
         TextButton(
            onClick = {
               dismiss()
            },
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
               items(shownList, key = { it }) { entry ->
                  ReorderableListItem(entry, entry, move) { modifier, _ ->
                     Text(
                        entry,
                        modifier = modifier
                           .fillMaxWidth()
                           .padding(16.dp)
                     )
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
internal fun ActionOrderListScreenPreview() {
   PreviewTheme {
      Box {
         ActionOrderListScreenContent(
            listOf("Option A", "Option B", "Option C"),
            { },
            { _, _ -> },
         )
      }
   }
}

@Serializable
data object ActionOrderListScreenKey : ScreenKey(), DialogKey
