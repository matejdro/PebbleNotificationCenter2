package com.matejdro.pebblenotificationcenter.bluetooth

import androidx.datastore.preferences.core.Preferences
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification

class FakeWatchSyncer : WatchSyncer {
   val syncedNotifications = mutableListOf<ProcessedNotification>()
   val syncedNotificationReadStatuses = mutableListOf<ProcessedNotification>()
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

   override suspend fun syncNotification(
      notification: ProcessedNotification,
      preferences: Preferences,
   ): Int {
      syncedNotifications.add(notification)
      return nextBucketId++
   }

   override suspend fun prepareNotificationReadStatus(notification: ProcessedNotification) {
      syncedNotificationReadStatuses.add(notification)
   }
}
