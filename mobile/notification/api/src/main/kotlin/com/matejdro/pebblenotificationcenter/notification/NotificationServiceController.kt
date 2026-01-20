package com.matejdro.pebblenotificationcenter.notification

interface NotificationServiceController {
   fun cancelNotification(key: String): Boolean
}
