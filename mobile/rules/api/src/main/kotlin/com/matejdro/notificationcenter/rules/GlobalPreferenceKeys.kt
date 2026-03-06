package com.matejdro.notificationcenter.rules

import com.matejdro.notificationcenter.rules.keys.BooleanPreferenceKeyWithDefault

object GlobalPreferenceKeys {
   val muteWatch = BooleanPreferenceKeyWithDefault("mute_watch", false)
   val mutePhone = BooleanPreferenceKeyWithDefault("mute_phone", false)
}
