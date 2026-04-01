package com.matejdro.pebblenotificationcenter.history

import com.matejdro.pebblenotificationcenter.history.sqldelight.generated.DbHistory
import java.time.Instant

internal fun HistoryEntry.toDb() = DbHistory(
   id = 0L,
   title = notificationTitle,
   subtitle = notificationSubtitle,
   time = time.toEpochMilli(),
   affectedRules = affectedRules.joinToString("\n"),
   muteReason = muteReason,
   hideReason = hideReason,
)

internal fun DbHistory.toHistoryEntry() = HistoryEntry(
   notificationTitle = title,
   notificationSubtitle = subtitle,
   time = Instant.ofEpochMilli(time),
   affectedRules = affectedRules?.split("\n")?.filter { it.isNotBlank() }.orEmpty(),
   muteReason = muteReason,
   hideReason = hideReason,
)
