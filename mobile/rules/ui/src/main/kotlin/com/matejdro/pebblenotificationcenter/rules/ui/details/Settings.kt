package com.matejdro.pebblenotificationcenter.rules.ui.details

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.pebblenotificationcenter.navigation.keys.TaskerTaskSetScreenKey
import com.matejdro.pebblenotificationcenter.navigation.util.rememberNavigationPopup
import com.matejdro.pebblenotificationcenter.rules.PebbleFont
import com.matejdro.pebblenotificationcenter.rules.RuleOption
import com.matejdro.pebblenotificationcenter.rules.keys.IntListPreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.SetPreference
import com.matejdro.pebblenotificationcenter.rules.keys.StringListPreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.get
import com.matejdro.pebblenotificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.rules.ui.dialogs.IntListScreenKey
import com.matejdro.pebblenotificationcenter.rules.ui.dialogs.RegexReplacementSetScreenKey
import com.matejdro.pebblenotificationcenter.rules.ui.dialogs.StringListScreenKey
import com.matejdro.pebblenotificationcenter.rules.ui.dialogs.VibrationPatternScreenKey
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.preferenceTheme
import si.inova.kotlinova.compose.result.ResultKey
import si.inova.kotlinova.navigation.navigator.Navigator
import kotlin.enums.enumEntries

@Composable
internal fun ColumnScope.Settings(
   navigator: Navigator,
   preferences: Preferences,
   updatePreference: SetPreference,
) {
   Text(
      stringResource(R.string.settings),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier
         .padding(horizontal = 16.dp)
         .semantics { heading() }
   )

   CompositionLocalProvider(LocalPreferenceTheme provides preferenceTheme()) {
      EnumListPreference(
         preferences[RuleOption.masterSwitch],
         { updatePreference(RuleOption.masterSwitch, it) },
         stringResource(R.string.setting_master_switch_title),
         listOf(
            stringResource(R.string.setting_master_switch_show),
            stringResource(R.string.setting_master_switch_mute),
            stringResource(R.string.setting_master_switch_hide),
         ),
         stringResource(R.string.setting_master_switch_description_suffix)
      )

      VibrationPatternPreference(navigator, updatePreference, preferences)

      SwitchPreference(
         value = preferences[RuleOption.periodicVibration],
         onValueChange = { updatePreference(RuleOption.periodicVibration, it) },
         title = { Text(stringResource(R.string.setting_periodic_vibration)) },
         summary = { Text(stringResource(R.string.setting_periodic_vibration_description)) }
      )

      val fontNames = remember {
         PebbleFont.entries.map { font ->
            font.name.split("_").joinToString(" ") { name -> name.lowercase().replaceFirstChar { it.uppercase() } }
         }
      }
      EnumListPreference(
         preferences[RuleOption.titleFont],
         { updatePreference(RuleOption.titleFont, it) },
         stringResource(R.string.preference_font_title),
         fontNames,
      )
      EnumListPreference(
         preferences[RuleOption.subtitleFont],
         { updatePreference(RuleOption.subtitleFont, it) },
         stringResource(R.string.preference_font_subtitle),
         fontNames,
      )
      EnumListPreference(
         preferences[RuleOption.bodyFont],
         { updatePreference(RuleOption.bodyFont, it) },
         stringResource(R.string.preference_font_body),
         fontNames,
      )

      RegexReplacementSetPreference(navigator, updatePreference, preferences)

      PreferenceCategory({ Text(stringResource(R.string.actions)) })

      StringListPreference(
         navigator,
         stringResource(R.string.setting_mute_silent),
         updatePreference,
         RuleOption.replyCannedTexts,
         stringResource(R.string.setting_mute_silent_description),
         preferences
      )

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         IntListPreference(
            navigator,
            "Snooze intervals",
            updatePreference,
            RuleOption.snoozeIntervals,
            null,
            preferences
         )
      }

      TaskerTaskListPreference(navigator, updatePreference, preferences)

      PreferenceCategory({ Text(stringResource(R.string.pausing)) })

      SwitchPreference(
         value = preferences[RuleOption.autoAppPause],
         onValueChange = { updatePreference(RuleOption.autoAppPause, it) },
         title = { Text(stringResource(R.string.setting_auto_app_pause)) },
         summary = { Text(stringResource(R.string.setting_auto_app_pause_description)) }
      )
      SwitchPreference(
         value = preferences[RuleOption.autoConversationPause],
         onValueChange = { updatePreference(RuleOption.autoConversationPause, it) },
         title = { Text(stringResource(R.string.setting_auto_conversation_pause)) },
         summary = { Text(stringResource(R.string.setting_auto_conversation_pause_description)) }
      )

      PreferenceCategory({ Text(stringResource(R.string.default_filter_overrides)) })

      SwitchPreference(
         value = preferences[RuleOption.muteSilentNotifications],
         onValueChange = { updatePreference(RuleOption.muteSilentNotifications, it) },
         title = { Text(stringResource(R.string.setting_mute_silent_notifications)) },
         summary = { Text(stringResource(R.string.setting_mute_silent_notifications_description)) }
      )
      SwitchPreference(
         value = preferences[RuleOption.muteDndNotifications],
         onValueChange = { updatePreference(RuleOption.muteDndNotifications, it) },
         title = { Text(stringResource(R.string.setting_mute_dnd_notifications)) },
         summary = { Text(stringResource(R.string.setting_mute_dnd_notifications_description)) }
      )
      SwitchPreference(
         value = preferences[RuleOption.muteIdenticalNotifications],
         onValueChange = { updatePreference(RuleOption.muteIdenticalNotifications, it) },
         title = { Text(stringResource(R.string.setting_mute_identical_notifications)) },
         summary = { Text(stringResource(R.string.setting_mute_identical_notifications_description)) }
      )
      SwitchPreference(
         value = preferences[RuleOption.hideOngoingNotifications],
         onValueChange = { updatePreference(RuleOption.hideOngoingNotifications, it) },
         title = { Text(stringResource(R.string.setting_hide_ongoing_notifications)) },
         summary = { Text(stringResource(R.string.setting_hide_ongoing_notifications_description)) }
      )
      SwitchPreference(
         value = preferences[RuleOption.hideGroupSummaryNotifications],
         onValueChange = { updatePreference(RuleOption.hideGroupSummaryNotifications, it) },
         title = { Text(stringResource(R.string.setting_hide_group_summary_notifications)) },
         summary = { Text(stringResource(R.string.setting_hide_group_summary_notifications_description)) }
      )
      SwitchPreference(
         value = preferences[RuleOption.hideLocalOnlyNotifications],
         onValueChange = { updatePreference(RuleOption.hideLocalOnlyNotifications, it) },
         title = { Text(stringResource(R.string.setting_hide_local_only_notifications)) },
         summary = { Text(stringResource(R.string.setting_hide_local_only_notifications_description)) }
      )
      SwitchPreference(
         value = preferences[RuleOption.hideMediaNotifications],
         onValueChange = { updatePreference(RuleOption.hideMediaNotifications, it) },
         title = { Text(stringResource(R.string.setting_hide_media_notifications)) },
         summary = { Text(stringResource(R.string.setting_hide_media_notifications_description)) }
      )
   }
}

@Composable
private fun StringListPreference(
   navigator: Navigator,
   title: String,
   updatePreference: SetPreference,
   preference: StringListPreferenceKeyWithDefault,
   description: String,
   preferences: Preferences,
) {
   val updatedPreferences by rememberUpdatedState(preferences)

   val dialog = navigator.rememberNavigationPopup(
      navigationKey = { initialList: List<String>, resultKey: ResultKey<List<String>> ->
         StringListScreenKey(title, initialList, resultKey)
      },
      onResult = {
         updatePreference(preference, it)
      }
   )

   Preference(
      title = { Text(title) },
      summary = {
         Text(description)
      },
      onClick = {
         dialog.trigger(updatedPreferences[preference].toList())
      }
   )
}

@Composable
private fun TaskerTaskListPreference(
   navigator: Navigator,
   updatePreference: SetPreference,
   preferences: Preferences,
) {
   val updatedPreferences by rememberUpdatedState(preferences)

   val title by rememberUpdatedState(stringResource(R.string.setting_tasker_actions))

   val dialog = navigator.rememberNavigationPopup(
      navigationKey = { initialList: Set<String>, resultKey: ResultKey<Set<String>> ->
         TaskerTaskSetScreenKey(title, initialList, resultKey)
      },
      onResult = {
         updatePreference(RuleOption.taskerTaskActions, it)
      }
   )

   Preference(
      title = { Text(title) },
      summary = {
         Text(stringResource(R.string.setting_tasker_actions_description))
      },
      onClick = {
         dialog.trigger(updatedPreferences[RuleOption.taskerTaskActions])
      }
   )
}

@Composable
private fun IntListPreference(
   navigator: Navigator,
   title: String,
   updatePreference: SetPreference,
   preference: IntListPreferenceKeyWithDefault,
   description: String?,
   preferences: Preferences,
) {
   val updatedPreferences by rememberUpdatedState(preferences)

   val dialog = navigator.rememberNavigationPopup(
      navigationKey = { initialList: List<Int>, resultKey: ResultKey<List<Int>> ->
         IntListScreenKey(title, initialList, resultKey)
      },
      onResult = {
         updatePreference(preference, it)
      }
   )

   Preference(
      title = { Text(title) },
      summary = {
         description?.let { Text(it) }
      },
      onClick = {
         dialog.trigger(updatedPreferences[preference].toList())
      }
   )
}

@Composable
private fun RegexReplacementSetPreference(
   navigator: Navigator,
   updatePreference: SetPreference,
   preferences: Preferences,
) {
   val updatedPreferences by rememberUpdatedState(preferences)
   val preference = RuleOption.regexReplacements

   val dialog = navigator.rememberNavigationPopup(
      navigationKey = { initialList: Set<Pair<String, String>>, resultKey: ResultKey<Set<Pair<String, String>>> ->
         RegexReplacementSetScreenKey(initialList, resultKey)
      },
      onResult = {
         updatePreference(preference, it)
      }
   )

   Preference(
      title = { Text(stringResource(R.string.preference_regex_replacement)) },
      summary = {
         Text(stringResource(R.string.preference_regex_replacement_description))
      },
      onClick = {
         dialog.trigger(updatedPreferences[preference])
      }
   )
}

@Composable
private inline fun <reified T : Enum<T>> EnumListPreference(
   value: T,
   crossinline setValue: (T) -> Unit,
   title: String,
   valueTexts: List<String>,
   description: String = "",
) {
   val enumEntries = enumEntries<T>()
   require(valueTexts.size == valueTexts.size) { "valueTexts does not contain values for every enum" }

   ListPreference(
      value.name,
      {
         setValue(enumValueOf<T>(it))
      },
      enumEntries.map { it.name },
      { Text(title) },
      summary = { Text(valueTexts.elementAt(value.ordinal) + description) },
      valueToText = { enumName ->
         AnnotatedString(
            valueTexts.elementAt(
               enumValueOf<T>(enumName).ordinal
            )
         )
      },
      type = ListPreferenceType.ALERT_DIALOG
   )
}

@Composable
private fun VibrationPatternPreference(
   navigator: Navigator,
   updatePreference: SetPreference,
   preferences: Preferences,
) {
   val dialog = navigator.rememberNavigationPopup(
      navigationKey = { pattern: String, resultKey: ResultKey<String> ->
         VibrationPatternScreenKey(pattern, resultKey)
      },
      onResult = {
         updatePreference(RuleOption.vibrationPattern, it)
      }
   )

   Preference(
      title = { Text(stringResource(R.string.setting_vibration_pattern)) },
      summary = {
         Text(
            stringResource(
               R.string.setting_vibration_pattern_description,
               preferences[RuleOption.vibrationPattern]
            )
         )
      },
      onClick = {
         dialog.trigger(preferences[RuleOption.vibrationPattern])
      }
   )
}

@Preview(heightDp = 2000, showBackground = true)
@Composable
@ShowkaseComposable(group = "test")
internal fun AllPreferencesPreview() {
   PreviewTheme {
      Column {
         Settings({}, emptyPreferences(), SetPreference { _, _ -> })
      }
   }
}
