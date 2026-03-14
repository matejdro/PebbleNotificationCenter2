package com.matejdro.notificationcenter.rules

import com.matejdro.notificationcenter.rules.keys.BooleanPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.EnumPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.NullableStringPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.StringListPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.StringPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.StringSetPreferenceKeyWithDefault

object RuleOption {
   val conditionAppPackage = NullableStringPreferenceKeyWithDefault("condition_app_package", null)
   val conditionNotificationChannels = StringSetPreferenceKeyWithDefault("condition_notification_channels", emptySet())
   val conditionWhitelistRegexes = StringSetPreferenceKeyWithDefault("condition_whitelist_regexes", emptySet())
   val conditionBlacklistRegexes = StringSetPreferenceKeyWithDefault("condition_blacklist_regexes", emptySet())

   val masterSwitch = EnumPreferenceKeyWithDefault("master_switch", MasterSwitch.SHOW)
   val vibrationPattern = StringPreferenceKeyWithDefault(
      "vibration_pattern",
      "50, 50, 50, 50, 50, 50, 50, 50, 50, 50"
   )

   val replyCannedTexts = StringListPreferenceKeyWithDefault(
      "reply_canned_texts_list",
      listOf("Yes", "No", "Okay")
   )

   val autoAppPause = BooleanPreferenceKeyWithDefault("auto_app_pause", false)
   val autoConversationPause = BooleanPreferenceKeyWithDefault("auto_conversation_pause", false)

   val muteSilentNotifications = BooleanPreferenceKeyWithDefault("mute_silent_notifications", true)
   val muteDndNotifications = BooleanPreferenceKeyWithDefault("mute_dnd_notifications", true)
   val muteIdenticalNotifications = BooleanPreferenceKeyWithDefault("mute_identical_notifications", true)
   val hideOngoingNotifications = BooleanPreferenceKeyWithDefault("hide_ongoing_notifications", true)
   val hideGroupSummaryNotifications = BooleanPreferenceKeyWithDefault("hide_group_summary_notifications", true)
   val hideLocalOnlyNotifications = BooleanPreferenceKeyWithDefault("hide_local_only_notifications", true)
   val hideMediaNotifications = BooleanPreferenceKeyWithDefault("hide_media_notifications", true)

   enum class MasterSwitch {
      SHOW,
      MUTE,
      HIDE,
   }
}
