package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification

interface NotificationRepository {
   fun getNotification(bucketId: Int): ProcessedNotification?
   fun pollNextVibration(): IntArray?

   suspend fun markAsRead(bucketId: Int)
}
