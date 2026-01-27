package com.matejdro.pebblenotificationcenter

import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController

class FakeNotificationServiceController : NotificationServiceController {
   var returnValue: Boolean = true
   var lastCancelledNotification: String? = null
   var lastTriggeredIntent: Any? = null

   override fun cancelNotification(key: String): Boolean {
      lastCancelledNotification = key
      return returnValue
   }

   override fun triggerAction(pendingIntent: Any): Boolean {
      lastTriggeredIntent = pendingIntent
      return returnValue
   }
}
