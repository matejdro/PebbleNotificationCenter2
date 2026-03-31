package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification

interface NotificationRepository {
   fun getAllActiveNotifications(): Collection<ProcessedNotification>
   fun getNotification(bucketId: Int): ProcessedNotification?
   fun pollNextVibration(): IntArray?

   /**
    * If the next vibration is null, re-set it to the set value
    */
   fun resetNextVibration(value: IntArray)

   suspend fun markAsRead(bucketId: Int)

   suspend fun notifyPackagePauseStatusChanged(pkg: String)
}
