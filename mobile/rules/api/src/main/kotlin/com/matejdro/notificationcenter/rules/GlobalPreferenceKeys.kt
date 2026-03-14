package com.matejdro.notificationcenter.rules

import com.matejdro.notificationcenter.rules.keys.BooleanPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.StringListPreferenceKeyWithDefault

object GlobalPreferenceKeys {
   val muteWatch = BooleanPreferenceKeyWithDefault("mute_watch", false)
   val mutePhone = BooleanPreferenceKeyWithDefault("mute_phone", false)
   val actionOrder = StringListPreferenceKeyWithDefault("action_order", emptyList())
}
