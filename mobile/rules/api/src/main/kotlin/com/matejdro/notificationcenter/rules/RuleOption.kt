package com.matejdro.notificationcenter.rules

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object RuleOption {
   val filterAppPackage = stringPreferencesKey("filter_app_package")
   val filterNotificationChannels = stringSetPreferencesKey("filter_app_package")
   val filterWhitelistRegexes = stringSetPreferencesKey("filter_app_package")
   val filterBlacklistRegexes = stringSetPreferencesKey("filter_app_package")
}
