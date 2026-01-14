package com.matejdro.pebblenotificationcenter.notification.model

data class ProcessedNotification(
   val systemData: ParsedNotification,
   val bucketId: Int = 0,
)
