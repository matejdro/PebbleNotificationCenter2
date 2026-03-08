package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification

interface NotificationRepository {
   fun getAllActiveNotifications(): Collection<ProcessedNotification>
   fun getNotification(bucketId: Int): ProcessedNotification?
   fun pollNextVibration(): IntArray?

   suspend fun markAsRead(bucketId: Int)

   fun notifyPackagePauseStatusChanged(pkg: String)
}
