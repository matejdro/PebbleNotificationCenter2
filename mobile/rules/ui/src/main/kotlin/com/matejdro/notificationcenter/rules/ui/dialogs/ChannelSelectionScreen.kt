package com.matejdro.notificationcenter.rules.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import com.matejdro.pebblenotificationcenter.notification.model.LightNotificationChannel
import com.matejdro.pebblenotificationcenter.ui.components.AlertDialogInnerContent
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import kotlinx.coroutines.Dispatchers
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
class ChannelSelectionScreen(
   private val navigator: Navigator,
   private val notificationServiceController: NotificationServiceController,
) : Screen<ChannelSelectionScreenKey>() {
   @Composable
   override fun Content(key: ChannelSelectionScreenKey) {
      val resultPassingStore = LocalResultPassingStore.current

      var channelList by remember<MutableState<List<LightNotificationChannel>>> { mutableStateOf(emptyList()) }
      var selectedChannels by remember<MutableState<List<String>>> { mutableStateOf(listOf("")) }

      val context = LocalContext.current
      LaunchedEffect(context, notificationServiceController) {
         withContext(Dispatchers.Default) {
            val anyAppEntry = LightNotificationChannel("", context.getString(R.string.select_all))

            channelList = listOf(anyAppEntry) +
               notificationServiceController.getNotificationChannels(key.pkg)
                  .sortedBy { it.title }
         }
      }
      ChannelSelectionScreenContent(
         channelList,
         selectedChannels,
         dismiss = { navigator.goBack() },
         accept = {
            navigator.goBack()
            val finalSelectedChannels = selectedChannels
            val result = if (finalSelectedChannels.contains("")) {
               emptyList()
            } else {
               finalSelectedChannels
            }

            resultPassingStore.sendResult(key.result, result)
         },
         select = { channel, selected ->
            if (selected) {
               selectedChannels = selectedChannels + channel
            } else {
               selectedChannels = selectedChannels - channel
            }
         }
      )
   }
}

@Composable
private fun ChannelSelectionScreenContent(
   channels: List<LightNotificationChannel>,
   selectedChannels: List<String>,
   dismiss: () -> Unit,
   accept: () -> Unit,
   select: (String, Boolean) -> Unit,
) {
   val allSelected = selectedChannels.contains("")

   AlertDialogInnerContent(
      title = {
         Text(stringResource(R.string.affected_channels))
      },
      confirmButton = {
         TextButton(
            onClick = {
               accept()
            },
            enabled = selectedChannels.isNotEmpty()
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
      content = {
         LazyColumn {
            items(channels) { channel ->
               ChannelItem(
                  channel,
                  allSelected || selectedChannels.contains(channel.id),
                  channel.id.isEmpty() || !allSelected,
                  toggle = {
                     select(channel.id, it)
                  }
               )
            }
         }
      },
   )
}

@Composable
private fun ChannelItem(
   channel: LightNotificationChannel,
   selected: Boolean,
   enabled: Boolean,
   toggle: (Boolean) -> Unit,
) {
   Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
         .fillMaxWidth()
         .clickable(onClick = { toggle(!selected) }, enabled = enabled)
         .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
   ) {
      Switch(checked = selected, onCheckedChange = toggle, enabled = enabled)

      Text(channel.title, modifier = Modifier.alpha(if (enabled) 1f else DisabledAlpha))
   }
}

/**
 * A low level of alpha used to represent disabled components, such as text in a disabled Button.
 */
private const val DisabledAlpha = 0.38f

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun ChannelSelectionScreenPreview() {
   PreviewTheme {
      Box() {
         ChannelSelectionScreenContent(
            List(20) {
               LightNotificationChannel(it.toString(), "Channel $it")
            },
            listOf("0", "1", "3"),
            {},
            {},
            { _, _ -> }
         )
      }
   }
}

@ShowkaseComposable(group = "test")
@Composable
@FullScreenPreviews
internal fun ChannelSelectionSelectAllChosenPreview() {
   PreviewTheme {
      Box() {
         ChannelSelectionScreenContent(
            listOf(LightNotificationChannel("", "Select All")) + List(20) {
               LightNotificationChannel(it.toString(), "Channel $it")
            },
            listOf(""),
            {},
            {},
            { _, _ -> }
         )
      }
   }
}

@Parcelize
data class ChannelSelectionScreenKey(
   val pkg: String,
   val result: ResultKey<List<String>>,
) : ScreenKey(), DialogKey {
   @IgnoredOnParcel
   override val dialogProperties: DialogProperties = DialogProperties()
}
