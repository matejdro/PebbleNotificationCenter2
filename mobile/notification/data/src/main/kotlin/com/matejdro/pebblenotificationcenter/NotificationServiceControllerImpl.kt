package com.matejdro.pebblenotificationcenter

import android.app.PendingIntent
import com.matejdro.pebblenotificationcenter.notification.NotificationService
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat

@Inject
@ContributesBinding(AppScope::class)
class NotificationServiceControllerImpl : NotificationServiceController {
   override fun cancelNotification(key: String): Boolean {
      val service = NotificationService.instance ?: return false

      logcat { "Canceling notification for $key" }

      service.cancelNotification(key)

      return true
   }

   override fun triggerAction(pendingIntent: Any): Boolean {
      if (NotificationService.instance == null) return false

      pendingIntent as PendingIntent

      pendingIntent.send()

      return true
   }
}
