package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.SetPreference
import com.matejdro.notificationcenter.rules.keys.StringSetPreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.notificationcenter.rules.ui.dialogs.StringListScreenKey
import com.matejdro.notificationcenter.rules.ui.dialogs.VibrationPatternScreenKey
import com.matejdro.pebblenotificationcenter.navigation.util.rememberNavigationPopup
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

      StringSetPreference(
         navigator,
         stringResource(R.string.setting_mute_silent),
         updatePreference,
         RuleOption.replyCannedTexts,
         stringResource(R.string.setting_mute_silent_description),
         preferences
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
private fun StringSetPreference(
   navigator: Navigator,
   title: String,
   updatePreference: SetPreference,
   preference: StringSetPreferenceKeyWithDefault,
   description: String,
   preferences: Preferences,
) {
   val dialog = navigator.rememberNavigationPopup(
      navigationKey = { initialList: List<String>, resultKey: ResultKey<List<String>> ->
         StringListScreenKey(title, initialList, resultKey)
      },
      onResult = {
         updatePreference(preference, it.toSet())
      }
   )

   Preference(
      title = { Text(title) },
      summary = {
         Text(description)
      },
      onClick = {
         dialog.trigger(preferences[preference].toList())
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
   val enumEntries = enumEntries<RuleOption.MasterSwitch>()
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
               enumValueOf<RuleOption.MasterSwitch>(enumName).ordinal
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
