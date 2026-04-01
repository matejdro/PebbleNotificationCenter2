package com.matejdro.pebblenotificationcenter.history

import java.time.Instant

data class HistoryEntry(
   val notificationTitle: String,
   val notificationSubtitle: String,
   val time: Instant,
   val affectedRules: List<String> = emptyList(),
   val muteReason: String? = null,
   val hideReason: String? = null,
)
