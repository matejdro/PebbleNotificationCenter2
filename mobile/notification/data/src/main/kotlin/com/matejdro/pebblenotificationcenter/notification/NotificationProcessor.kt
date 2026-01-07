package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.bluetooth.WatchSyncer
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class NotificationProcessor(
   private val watchSyncer: WatchSyncer,
) {
   suspend fun onNotificationPosted(parsedNotification: ParsedNotification) {
      watchSyncer.syncNotification(parsedNotification)
   }

   suspend fun onNotificationDismissed(key: String) {
      watchSyncer.clearNotification(key)
   }

   suspend fun onNotificationsCleared() {
      watchSyncer.clearAllNotifications()
   }
}
