package com.matejdro.pebblenotificationcenter.tasker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.tasker.ui.R
import com.matejdro.pebblenotificationcenter.navigation.keys.TaskerTaskSetScreenKey
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.navigation.instructions.goBack
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.pebblenotificationcenter.sharedresources.R as sharedR

@InjectNavigationScreen
class TaskerTaskSetScreen(private val navigator: Navigator) : Screen<TaskerTaskSetScreenKey>() {
   @Composable
   override fun Content(key: TaskerTaskSetScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current
      var list by remember { mutableStateOf(key.initialList.toList()) }
      val context = LocalContext.current

      val taskerSelectResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
         val taskName = result.data?.dataString ?: return@rememberLauncherForActivityResult
         list += taskName
      }

      TaskerTaskSetScreenContent(
         key.title,
         list,
         addNew = {
            val taskerSelectIntent = Intent("net.dinglisch.android.tasker.ACTION_TASK_SELECT")
            try {
               taskerSelectResult.launch(taskerSelectIntent)
            } catch (_: ActivityNotFoundException) {
               Toast.makeText(context, R.string.error_no_tasker, Toast.LENGTH_SHORT).show()
            }
         },
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
private fun TaskerTaskSetScreenContent(
   title: String,
   list: List<String>,
   addNew: () -> Unit,
   delete: (Int) -> Unit,
   dismiss: () -> Unit,
   accept: () -> Unit,
) {
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
            item {
               Text(stringResource(R.string.tasker_variables))
            }

            itemsIndexed(list) { index, entry ->
               Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                  Text("•", Modifier.padding(end = 8.dp))

                  Text(
                     entry,
                     modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                  )

                  Button(onClick = { delete(index) }) {
                     Icon(painterResource(sharedR.drawable.ic_delete), contentDescription = stringResource(sharedR.string.delete))
                  }
               }
            }

            item {
               Button(onClick = { addNew() }) {
                  Icon(painterResource(sharedR.drawable.ic_add), contentDescription = stringResource(sharedR.string.add))
               }
            }
         }
      },
   )
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun TaskerTaskListScreenPreview() {
   PreviewTheme {
      Box {
         TaskerTaskSetScreenContent(
            "Options",
            listOf("Option A", "Option B", "Option C"),
            {},
            {},
            {},
            {},
         )
      }
   }
}
