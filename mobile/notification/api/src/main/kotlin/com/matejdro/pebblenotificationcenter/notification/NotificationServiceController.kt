package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.LightNotificationChannel

interface NotificationServiceController {
   fun cancelNotification(key: String): Boolean
   fun triggerAction(pendingIntent: Any): Boolean
   fun triggerReplyAction(pendingIntent: Any, remoteInputKey: String, text: String): Boolean
   fun getNotificationChannels(pkg: String): List<LightNotificationChannel>
}
