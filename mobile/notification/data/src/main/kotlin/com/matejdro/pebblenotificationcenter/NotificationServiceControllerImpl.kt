package com.matejdro.pebblenotificationcenter

import com.matejdro.pebblenotificationcenter.notification.NotificationService
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class NotificationServiceControllerImpl : NotificationServiceController {
   override fun cancelNotification(key: String): Boolean {
      val service = NotificationService.instance ?: return false

      service.cancelNotification(key)

      return true
   }
}
