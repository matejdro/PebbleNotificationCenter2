package com.matejdro.pebblenotificationcenter.tasker.screens.togglemute

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf

import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.tasker.ui.R
import com.matejdro.pebblenotificationcenter.tasker.BundleKeys
import com.matejdro.pebblenotificationcenter.tasker.TaskerAction
import com.matejdro.pebblenotificationcenter.tasker.TaskerConfigurationActivity
import com.matejdro.pebblenotificationcenter.ui.debugging.FullScreenPreviews
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.preferenceTheme
import si.inova.kotlinova.core.activity.requireActivity
import si.inova.kotlinova.navigation.screenkeys.ScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import com.matejdro.pebblenotificationcenter.sharedresources.R as sharedR

@InjectNavigationScreen
class ToggleMuteScreen : Screen<ToggleMuteScreenKey>() {
   @Composable
   override fun Content(key: ToggleMuteScreenKey) {
      var muteWatch by remember { mutableStateOf(false) }
      var mutePhone by remember { mutableStateOf(false) }

      val activity = LocalContext.current.requireActivity().let { it as TaskerConfigurationActivity }
      DisposableEffect(Unit) {
         val existingData = activity.existingData
         muteWatch = existingData.getBoolean(BundleKeys.MUTE_WATCH)
         mutePhone = existingData.getBoolean(BundleKeys.MUTE_PHONE)

         onDispose { }
      }

      BackHandler {
         val bundle = bundleOf(
            BundleKeys.ACTION to TaskerAction.TOGGLE_MUTE.name,
            BundleKeys.MUTE_WATCH to muteWatch,
            BundleKeys.MUTE_PHONE to mutePhone,
         )

         val muteWatchTitle = activity.getString(sharedR.string.setting_mute_watch)
         val watchMessage = if (muteWatch) {
            activity.getString(R.string.enable, muteWatchTitle)
         } else {
            activity.getString(R.string.disable, muteWatchTitle)
         }

         val mutePhoneTitle = activity.getString(sharedR.string.setting_mute_phone)
         val phoneMessage = if (mutePhone) {
            activity.getString(R.string.enable, mutePhoneTitle)
         } else {
            activity.getString(R.string.disable, mutePhoneTitle)
         }

         activity.saveConfiguration(bundle, "$watchMessage, $phoneMessage")
         activity.finish()
      }

      ToggleMuteScreenContent(
         muteWatch = muteWatch,
         mutePhone = mutePhone,
         setMuteWatch = { muteWatch = it },
         setMutePhone = { mutePhone = it },
      )
   }
}

@Composable
private fun ToggleMuteScreenContent(
   muteWatch: Boolean,
   mutePhone: Boolean,
   setMuteWatch: (Boolean) -> Unit,
   setMutePhone: (Boolean) -> Unit,
) {
   Column(
      Modifier
         .fillMaxWidth()
         .safeDrawingPadding()
   ) {
      CompositionLocalProvider(
         LocalPreferenceTheme provides preferenceTheme()
      ) {
         SwitchPreference(
            muteWatch,
            onValueChange = {
               setMuteWatch(it)
            },
            title = { Text(stringResource(sharedR.string.setting_mute_watch)) },
            summary = { Text(stringResource(sharedR.string.setting_mute_watch_description)) }
         )

         SwitchPreference(
            mutePhone,
            onValueChange = {
               setMutePhone(it)
            },
            title = { Text(stringResource(sharedR.string.setting_mute_phone)) },
            summary = { Text(stringResource(sharedR.string.setting_mute_phone_description)) }
         )
      }
   }
}

@Serializable
data object ToggleMuteScreenKey : ScreenKey()

@FullScreenPreviews
@Composable
@ShowkaseComposable(group = "test")
internal fun ToggleMuteScreenContentPreview() {
   PreviewTheme {
      ToggleMuteScreenContent(true, false, {}, {})
   }
}
