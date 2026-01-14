package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification

class FakeWatchSyncer : WatchSyncer {
   val syncedNotifications = mutableListOf<ParsedNotification>()
   val clearedNotifications = mutableListOf<String>()
   var clearAllCalled = false

   var nextBucketId = 1

   override suspend fun init() {
   }

   override suspend fun clearAllNotifications() {
      clearAllCalled = true
   }

   override suspend fun clearNotification(key: String) {
      clearedNotifications.add(key)
   }

   override suspend fun syncNotification(notification: ParsedNotification): Int {
      syncedNotifications.add(notification)
      return nextBucketId++
   }
}
