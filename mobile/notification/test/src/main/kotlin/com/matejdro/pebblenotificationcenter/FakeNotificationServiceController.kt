package com.matejdro.pebblenotificationcenter

import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import com.matejdro.pebblenotificationcenter.notification.model.LightNotificationChannel

class FakeNotificationServiceController : NotificationServiceController {
   private val providedChannels = HashMap<String, List<LightNotificationChannel>>()

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

   fun putNotificationChannels(pkg: String, channels: List<LightNotificationChannel>) {
      providedChannels[pkg] = channels
   }

   override fun getNotificationChannels(pkg: String): List<LightNotificationChannel> {
      return providedChannels[pkg] ?: error("Channels for the package $pkg not faked")
   }
}
