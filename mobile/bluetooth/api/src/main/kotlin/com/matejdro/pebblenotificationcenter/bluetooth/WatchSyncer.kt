package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification

interface WatchSyncer {
   suspend fun init()

   suspend fun clearAllNotifications()
   suspend fun clearNotification(key: String)

   /**
    * @return bucket id of the notification
    */
   suspend fun syncNotification(notification: ParsedNotification): Int
}
