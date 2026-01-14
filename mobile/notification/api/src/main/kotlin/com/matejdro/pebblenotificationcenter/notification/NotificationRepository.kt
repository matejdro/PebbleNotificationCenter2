package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification

interface NotificationRepository {
   fun getNotification(bucketId: Int): ProcessedNotification?
}
