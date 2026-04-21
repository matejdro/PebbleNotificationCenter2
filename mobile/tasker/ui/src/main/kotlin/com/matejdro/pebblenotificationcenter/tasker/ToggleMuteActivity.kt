package com.matejdro.pebblenotificationcenter.tasker

import com.matejdro.pebblenotificationcenter.tasker.screens.togglemute.ToggleMuteScreenKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

class ToggleMuteActivity : TaskerConfigurationActivity() {
   override fun getInitialHistory(): List<ScreenKey> {
      return listOf(ToggleMuteScreenKey)
   }
}
