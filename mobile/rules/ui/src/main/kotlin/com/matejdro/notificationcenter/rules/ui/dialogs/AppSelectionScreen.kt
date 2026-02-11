package com.matejdro.notificationcenter.rules.ui.dialogs

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
class AppSelectionScreen(private val navigator: Navigator) : Screen<AppSelectionScreenKey>() {
   @Composable
   override fun Content(key: AppSelectionScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current

      AppSelectionScreenContent(
         dismiss = { navigator.goBack() },
         accept = {
            navigator.goBack()
            resultPassingStore.sendResult(key.result, it)
         }
      )
   }
}

@Composable
private fun AppSelectionScreenContent(
   dismiss: () -> Unit,
   accept: (String) -> Unit,
) {
   var appList by remember { mutableStateOf<List<App>>(emptyList()) }

   val context = LocalContext.current
   LaunchedEffect(context) {
      withContext(Dispatchers.Default) {
         val packageManager = context.packageManager
         val anyAppEntry = App("", context.getString(R.string.any_app))

         appList = listOf(anyAppEntry) + packageManager.getInstalledPackages(0)
            .mapNotNull {
               val info = it.applicationInfo ?: return@mapNotNull null
               App(it.packageName, packageManager.getApplicationLabel(info).toString())
            }
            .sortedBy { it.name }
      }
   }

   AppSelectionScreenContent(appList, dismiss, accept)
}

@Composable
private fun AppSelectionScreenContent(
   appList: List<App>,
   dismiss: () -> Unit,
   accept: (String) -> Unit,
) {
   val filteredAppList = remember { mutableStateOf<List<App>>(appList) }
   val filterText = remember { mutableStateOf<String>("") }

   LaunchedEffect(appList) {
      snapshotFlow { filterText.value }
         .map { filter ->
            appList.filter { app ->
               filter.isEmpty() || app.name.contains(filter, ignoreCase = true)
            }
         }
         .flowOn(Dispatchers.Default)
         .collect {
            filteredAppList.value = it
         }
   }

   AlertDialogInnerContent(
      title = {
         Text(stringResource(R.string.select_the_app))
      },
      confirmButton = {
         TextButton(
            onClick = {
               dismiss()
            }
         ) {
            Text(stringResource(R.string.cancel))
         }
      },
      content = {
         Column(Modifier.fillMaxSize()) {
            TextField(
               value = filterText.value,
               onValueChange = { filterText.value = it },
               Modifier
                  .fillMaxWidth(),
               singleLine = true,
               label = { Text(stringResource(R.string.filter)) }
            )

            AppItems(filteredAppList::value, accept, Modifier.weight(1f))
         }
      },
   )
}

@Composable
private fun AppItems(
   appList: () -> List<App>,
   accept: (String) -> Unit,
   modifier: Modifier = Modifier,
) {
   LazyColumn(modifier = modifier) {
      items(appList()) { app ->
         AppItem(app, accept = { accept(app.pkg) })
      }
   }
}

@Composable
private fun AppItem(
   app: App,
   accept: () -> Unit,
) {
   Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
         .fillMaxWidth()
         .clickable(onClick = accept)
         .padding(16.dp)
   ) {
      AppIcon(app.pkg, Modifier.padding(end = 8.dp))

      Text(app.name)
   }
}

@Composable
private fun AppIcon(pkg: String, modifier: Modifier = Modifier) {
   if (LocalInspectionMode.current) {
      Box(
         modifier
            .size(24.dp)
            .background(Color.Red)
      )
   } else if (pkg.isEmpty()) {
      Spacer(modifier.size(24.dp))
   } else {
      var icon by remember { mutableStateOf<Drawable?>(null) }
      val context = LocalContext.current
      LaunchedEffect(context) {
         icon = withContext(Dispatchers.Default) {
            val packageManager = context.packageManager
            packageManager.getApplicationIcon(pkg)
         }
      }

      if (icon == null) {
         Spacer(modifier.size(24.dp))
      } else {
         Image(
            rememberDrawablePainter(icon),
            contentDescription = null,
            modifier = modifier.size(24.dp)
         )
      }
   }
}

@Immutable
private data class App(val pkg: String, val name: String)

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun AppSelectionScreenPreview() {
   PreviewTheme {
      Box() {
         AppSelectionScreenContent(
            List(20) {
               App(it.toString(), "App $it")
            },
            {},
            {},
         )
      }
   }
}

@Parcelize
data class AppSelectionScreenKey(
   val result: ResultKey<String>,
) : ScreenKey(), DialogKey {
   @IgnoredOnParcel
   override val dialogProperties: DialogProperties = DialogProperties()
}
