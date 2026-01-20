package com.matejdro.pebblenotificationcenter

import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController

class FakeNotificationServiceController : NotificationServiceController {
   var returnValue: Boolean = true
   var lastCancelledNotification: String? = null

   override fun cancelNotification(key: String): Boolean {
      lastCancelledNotification = key
      return returnValue
   }
}
