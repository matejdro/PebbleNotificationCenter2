package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.LightNotificationChannel

interface NotificationServiceController {
   fun cancelNotification(key: String): Boolean
   fun triggerAction(pendingIntent: Any): Boolean
   fun getNotificationChannels(pkg: String): List<LightNotificationChannel>
}
