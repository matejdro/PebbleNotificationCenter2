package com.matejdro.notificationcenter.rules

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.matejdro.notificationcenter.rules.keys.EnumPreferenceKeyWithDefault

object RuleOption {
   val conditionAppPackage = stringPreferencesKey("condition_app_package")
   val conditionNotificationChannels = stringSetPreferencesKey("condition_notification_channels")
   val conditionWhitelistRegexes = stringSetPreferencesKey("condition_whitelist_regexes")
   val conditionBlacklistRegexes = stringSetPreferencesKey("condition_blacklist_regexes")

   val masterSwitch = EnumPreferenceKeyWithDefault("master_switch", MasterSwitch.SHOW)

   enum class MasterSwitch {
      SHOW,
      MUTE,
      HIDE,
   }
}
