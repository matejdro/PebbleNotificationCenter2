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
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.preferenceTheme
import kotlin.enums.enumEntries

@Composable
internal fun ColumnScope.Settings(
   preferences: Preferences,
   updatePreference: SetPreference,
) {
   Text(
      stringResource(R.string.settings),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.Companion
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
   }
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

@Preview
@Composable
@ShowkaseComposable(group = "test")
internal fun AllPreferencesPreview() {
   PreviewTheme {
      Column {
         Settings(emptyPreferences(), SetPreference { _, _ -> })
      }
   }
}
