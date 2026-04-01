package com.matejdro.pebblenotificationcenter.history.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.matejdro.pebblenotificationcenter.history.HistoryEntry
import com.matejdro.pebblenotificationcenter.navigation.keys.HistoryScreenKey
import com.matejdro.pebblenotificationcenter.ui.components.ProgressErrorSuccessScaffold
import com.matejdro.pebblenotificationcenter.ui.debugging.PreviewTheme
import si.inova.kotlinova.compose.components.itemsWithDivider
import si.inova.kotlinova.compose.flow.collectAsStateWithLifecycleAndBlinkingPrevention
import si.inova.kotlinova.compose.time.ComposeAndroidDateTimeFormatter
import si.inova.kotlinova.compose.time.LocalDateFormatter
import si.inova.kotlinova.core.time.FakeAndroidDateTimeFormatter
import si.inova.kotlinova.core.time.FakeAndroidTimeProvider
import si.inova.kotlinova.core.time.TimeProvider
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.FormatStyle

@InjectNavigationScreen
class HistoryScreen(
   private val viewModel: HistoryScreenViewModel,
   private val timeProvider: TimeProvider,
) : Screen<HistoryScreenKey>() {
   @Composable
   override fun Content(key: HistoryScreenKey) {
      val state = viewModel.uiState.collectAsStateWithLifecycleAndBlinkingPrevention()

      ProgressErrorSuccessScaffold(
         state::value,
         errorProgressModifier = Modifier.safeDrawingPadding(),
      ) {
         HistoryScreenContent(it, timeProvider)
      }
   }
}

@Composable
private fun HistoryScreenContent(history: List<HistoryEntry>, timeProvider: TimeProvider) {
   LazyColumn(
      contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
   ) {
      itemsWithDivider(history) { historyEntry ->
         HistoryItem(historyEntry, timeProvider)
      }
   }
}

@Composable
private fun HistoryItem(
   historyEntry: HistoryEntry,
   timeProvider: TimeProvider,
) {
   Column(Modifier.padding(16.dp)) {
      Text(historyEntry.notificationTitle, fontWeight = FontWeight.Bold)
      Text(historyEntry.notificationSubtitle)

      val localDateTime = historyEntry.time.atZone(timeProvider.systemDefaultZoneId()).toLocalDateTime()
      Text(LocalDateFormatter.current.ofLocalizedDateTime(FormatStyle.SHORT).format(localDateTime))

      Text(stringResource(R.string.active_rules), Modifier.padding(top = 8.dp))
      for (rule in listOf(stringResource(R.string.default_rule)) + historyEntry.affectedRules) {
         Text("• $rule", Modifier.padding(start = 4.dp))
      }

      val muteReason = historyEntry.muteReason
      val hideReason = historyEntry.hideReason
      if (muteReason != null) {
         Text(stringResource(R.string.history_notification_muted, muteReason), Modifier.padding(top = 8.dp))
      } else if (hideReason != null) {
         Text(stringResource(R.string.history_notification_hidden, hideReason), Modifier.padding(top = 8.dp))
      } else {
         Text(stringResource(R.string.history_notification_shown), Modifier.padding(top = 8.dp))
      }
   }
}

@Preview
@Composable
internal fun HistoryScreenPreview() {
   PreviewTheme {
      CompositionLocalProvider(LocalDateFormatter provides ComposeAndroidDateTimeFormatter(FakeAndroidDateTimeFormatter())) {
         HistoryScreenContent(
            listOf(
               HistoryEntry(
                  "Notification A",
                  "Subtitle",
                  ZonedDateTime.of(2026, 1, 10, 9, 30, 0, 0, ZoneId.of("Europe/Berlin")).toInstant(),
                  listOf("Rule A", "Rule B"),
                  "Annoying person"
               ),

               HistoryEntry(
                  "Notification B",
                  "",
                  ZonedDateTime.of(2026, 1, 17, 18, 27, 0, 0, ZoneId.of("Europe/Berlin")).toInstant(),
                  emptyList(),
               ),

               HistoryEntry(
                  "Notification A",
                  "Subtitle D",
                  ZonedDateTime.of(2026, 2, 5, 0, 30, 0, 0, ZoneId.of("Europe/Berlin")).toInstant(),
                  listOf("Rule C"),
                  null,
                  "Ongoing notification"
               ),
            ),
            FakeAndroidTimeProvider(
               currentTimezone = { ZoneId.of("Europe/Berlin") }
            )
         )
      }
   }
}
