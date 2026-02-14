package com.matejdro.notificationcenter.rules

import com.matejdro.notificationcenter.rules.keys.EnumPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.NullableStringPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.StringSetPreferenceKeyWithDefault

object RuleOption {
   val conditionAppPackage = NullableStringPreferenceKeyWithDefault("condition_app_package", null)
   val conditionNotificationChannels = StringSetPreferenceKeyWithDefault("condition_notification_channels", emptySet())
   val conditionWhitelistRegexes = StringSetPreferenceKeyWithDefault("condition_whitelist_regexes", emptySet())
   val conditionBlacklistRegexes = StringSetPreferenceKeyWithDefault("condition_blacklist_regexes", emptySet())

   val masterSwitch = EnumPreferenceKeyWithDefault("master_switch", MasterSwitch.SHOW)

   enum class MasterSwitch {
      SHOW,
      MUTE,
      HIDE,
   }
}
