package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.ui.R
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme

@Composable
internal fun ColumnScope.Conditions(
   state: RuleDetailsScreenState,
   changeTargetApp: () -> Unit,
) {
   Text(
      stringResource(R.string.conditions),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.Companion
         .padding(16.dp)
         .semantics { heading() }
   )

   if (state.targetAppName == null) {
      Text(stringResource(R.string.no_conditions), modifier = Modifier.Companion.padding(horizontal = 16.dp))
   } else {
      Text(
         stringResource(R.string.conditions_header),
         modifier = Modifier.Companion.padding(horizontal = 16.dp)
      )
      val appText = buildAnnotatedString {
         append(stringResource(R.string.app_condition_prefix))
         withStyle(SpanStyle(fontWeight = FontWeight.Companion.Bold)) {
            append(state.targetAppName)
         }
      }
      Text(appText, modifier = Modifier.Companion.padding(horizontal = 16.dp))

      val channels = state.targetChannelNames
      if (channels.isNotEmpty()) {
         val channelsText = buildAnnotatedString {
            if (channels.size == 1) {
               append(stringResource(R.string.single_channel_condition_prefix))
            } else {
               append(stringResource(R.string.multi_channel_condition_prefix))
            }

            channels.forEachIndexed { index, channel ->
               withStyle(SpanStyle(fontWeight = FontWeight.Companion.Bold)) {
                  append(channel)
               }

               if (index != channels.lastIndex) {
                  append(", ")
               }
            }
         }
         Text(channelsText, modifier = Modifier.Companion.padding(horizontal = 16.dp))
      }
   }

   if (state.ruleMetadata.id != RULE_ID_DEFAULT_SETTINGS) {
      FlowRow(
         Modifier.Companion.padding(top = 16.dp, start = 16.dp, end = 16.dp),
         horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
         Button(onClick = changeTargetApp) { Text(stringResource(R.string.change_target_app)) }
      }
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun ConditionsWithTargetAppPreview() {
   PreviewTheme {
      Column {
         Conditions(
            state = RuleDetailsScreenState(
               ruleMetadata = RuleMetadata(2, "Test Rule"),
               preferences = emptyPreferences(),
               targetAppName = "My super app",
               targetChannelNames = emptyList()
            ),
            {},
         )
      }
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun ConditionsWithTargetAppAndChannelsPreview() {
   PreviewTheme {
      Column {
         Conditions(
            state = RuleDetailsScreenState(
               ruleMetadata = RuleMetadata(2, "Test Rule"),
               preferences = emptyPreferences(),
               targetAppName = "My super app",
               targetChannelNames = listOf("Channel 1", "Channel 2", "Channel 3")
            ),
            {},
         )
      }
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun ConditionsWithTargetAppAndSingleChannelPreview() {
   PreviewTheme {
      Column {
         Conditions(
            state = RuleDetailsScreenState(
               ruleMetadata = RuleMetadata(2, "Test Rule"),
               preferences = emptyPreferences(),
               targetAppName = "My super app",
               targetChannelNames = listOf("Channel 1")
            ),
            {},
         )
      }
   }
}

@Preview
@ShowkaseComposable(group = "test")
@Composable
internal fun ConditionsWithDefaultSettingsPreview() {
   PreviewTheme {
      Column {
         Conditions(
            state = RuleDetailsScreenState(
               ruleMetadata = RuleMetadata(1, "Default Settings"),
               preferences = emptyPreferences(),
            ),
            {},
         )
      }
   }
}
