package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification

class FakeNotificationRepository : NotificationRepository {
   private val notifications = HashMap<Int, ProcessedNotification?>()
   val notificationsMarkedAsRead = ArrayList<Int>()
   var nextVibration: IntArray? = null

   override fun getNotification(bucketId: Int): ProcessedNotification? {
      return notifications[bucketId]
   }

   override fun pollNextVibration(): IntArray? {
      val nextVibrationLocal = nextVibration
      nextVibration = null
      return nextVibrationLocal
   }

   fun putNotification(bucketId: Int, processedNotification: ProcessedNotification) {
      notifications[bucketId] = processedNotification
   }

   override suspend fun markAsRead(bucketId: Int) {
      notificationsMarkedAsRead.add(bucketId)
   }
}
