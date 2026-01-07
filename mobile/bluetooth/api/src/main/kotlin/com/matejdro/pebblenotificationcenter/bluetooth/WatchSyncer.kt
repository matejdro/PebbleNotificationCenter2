package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification

interface WatchSyncer {
   suspend fun init()

   suspend fun clearAllNotifications()
   suspend fun clearNotification(key: String)
   suspend fun syncNotification(notification: ParsedNotification)
}
