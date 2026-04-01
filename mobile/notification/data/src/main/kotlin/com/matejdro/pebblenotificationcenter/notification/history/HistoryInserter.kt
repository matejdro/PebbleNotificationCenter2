package com.matejdro.pebblenotificationcenter.notification.history

import android.content.Context
import com.matejdro.pebblenotificationcenter.history.HistoryEntry
import com.matejdro.pebblenotificationcenter.history.HistoryRepository
import com.matejdro.pebblenotificationcenter.notification.R
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import si.inova.kotlinova.core.time.TimeProvider

interface HistoryInserter {
   suspend fun insertHistoryEntry(
      notification: ParsedNotification,
      affectedRules: List<String>,
      hideReason: HideReason?,
      muteReason: MuteReason?,
   )
}

@Inject
@ContributesBinding(AppScope::class)
class HistoryInserterImpl(
   private val historyRepository: HistoryRepository,
   private val context: Context,
   private val timeProvider: TimeProvider,
) : HistoryInserter {
   override suspend fun insertHistoryEntry(
      notification: ParsedNotification,
      affectedRules: List<String>,
      hideReason: HideReason?,
      muteReason: MuteReason?,
   ) {
      historyRepository.addHistoryEntry(
         HistoryEntry(
            notificationTitle = notification.title,
            notificationSubtitle = notification.subtitle,
            time = timeProvider.currentInstant(),
            affectedRules = affectedRules,
            muteReason = muteReason?.getDescription(),
            hideReason = hideReason?.getDescription(),
         )
      )
   }

   private fun MuteReason.getDescription(): String {
      return when (this) {
         MuteReason.APP_STARTUP -> context.getString(R.string.reason_app_startup)
         MuteReason.WATCH_MUTE -> context.getString(R.string.reason_watch_mute)
         MuteReason.PAUSE -> context.getString(R.string.reason_paused)
         MuteReason.MASTER_SWITCH -> context.getString(R.string.reason_master_switch)
         MuteReason.SILENT_NOTIFICATION -> context.getString(R.string.reason_silent_notification)
         MuteReason.DO_NOT_DISTURB -> context.getString(R.string.reason_dnd)
         MuteReason.IDENTICAL_TEXT -> context.getString(R.string.reason_identical)
      }
   }

   private fun HideReason.getDescription(): String {
      return when (this) {
         HideReason.MASTER_SWITCH -> context.getString(R.string.reason_master_switch)
         HideReason.ONGOING_NOTIFICATION -> context.getString(R.string.reason_ongoing)
         HideReason.GROUP_SUMMARY_NOTIFICATION -> context.getString(R.string.reason_group_summary)
         HideReason.LOCAL_ONLY_NOTIFICATION -> context.getString(R.string.reason_local_only)
         HideReason.MEDIA_NOTIFICATION -> context.getString(R.string.reason_media)
      }
   }
}
