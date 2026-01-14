package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification

class FakeNotificationRepository : NotificationRepository {
   private val notifications = HashMap<Int, ProcessedNotification?>()

   override fun getNotification(bucketId: Int): ProcessedNotification? {
      return notifications[bucketId]
   }

   fun putNotification(bucketId: Int, processedNotification: ProcessedNotification) {
      notifications[bucketId] = processedNotification
   }
}
