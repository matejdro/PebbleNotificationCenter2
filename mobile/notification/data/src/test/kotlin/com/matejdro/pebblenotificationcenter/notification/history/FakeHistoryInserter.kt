package com.matejdro.pebblenotificationcenter.notification.history

import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification

class FakeHistoryInserter : HistoryInserter {
   val insertedEntries = ArrayList<InsertedEntry>()

   override suspend fun insertHistoryEntry(
      notification: ParsedNotification,
      affectedRules: List<String>,
      hideReason: HideReason?,
      muteReason: MuteReason?,
   ) {
      insertedEntries += InsertedEntry(notification, affectedRules, hideReason, muteReason)
   }

   data class InsertedEntry(
      val notification: ParsedNotification,
      val affectedRules: List<String>,
      val hideReason: HideReason?,
      val muteReason: MuteReason?,
   )
}
