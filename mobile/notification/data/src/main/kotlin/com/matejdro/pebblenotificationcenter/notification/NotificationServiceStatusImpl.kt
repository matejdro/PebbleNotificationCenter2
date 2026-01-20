package com.matejdro.pebblenotificationcenter.notification

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class NotificationServiceStatusImpl : NotificationServiceStatus {
   override fun isEnabled(): Boolean {
      return NotificationService.instance != null
   }
}
