package com.matejdro.pebblenotificationcenter

import android.app.PendingIntent
import android.os.Build
import android.os.Process
import com.matejdro.pebblenotificationcenter.notification.NotificationService
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import com.matejdro.pebblenotificationcenter.notification.model.LightNotificationChannel
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

   override fun getNotificationChannels(pkg: String): List<LightNotificationChannel> {
      val service = NotificationService.instance ?: return emptyList()

      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         service.getNotificationChannels(pkg, Process.myUserHandle()).orEmpty().map {
            LightNotificationChannel(it.id, it.name.toString())
         }
      } else {
         emptyList()
      }
   }
}
