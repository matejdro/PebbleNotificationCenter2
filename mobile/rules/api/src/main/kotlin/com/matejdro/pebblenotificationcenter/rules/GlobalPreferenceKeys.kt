package com.matejdro.pebblenotificationcenter.rules

import com.matejdro.pebblenotificationcenter.rules.keys.BooleanPreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.IntPreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.StringListPreferenceKeyWithDefault

object GlobalPreferenceKeys {
   val muteWatch = BooleanPreferenceKeyWithDefault("mute_watch", false)
   val mutePhone = BooleanPreferenceKeyWithDefault("mute_phone", false)
   val actionOrder = StringListPreferenceKeyWithDefault("action_order", emptyList())

   val autoCloseSeconds = IntPreferenceKeyWithDefault("auto_close", 0)

   val notifyOnReconnect = BooleanPreferenceKeyWithDefault("notify_on_reconnect", true)
}
