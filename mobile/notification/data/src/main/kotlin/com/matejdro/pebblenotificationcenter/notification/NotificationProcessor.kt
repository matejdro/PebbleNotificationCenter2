package com.matejdro.pebblenotificationcenter.notification

import android.content.Context
import com.matejdro.pebblenotificationcenter.bluetooth.WatchSyncer
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NotificationProcessor(
   private val context: Context,
   private val watchSyncer: WatchSyncer,
) : NotificationRepository {
   private val notifications = HashMap<Int, ProcessedNotification>()
   private val notificationsByKey = HashMap<String, ProcessedNotification>()

   suspend fun onNotificationPosted(parsedNotification: ParsedNotification) {
      val actions = listOf<Action>(
         Action.Dismiss(context.getString(R.string.dismiss)),
      )

      val bucketId = watchSyncer.syncNotification(parsedNotification)
      val processedNotification = ProcessedNotification(parsedNotification, bucketId, actions)

      notifications[bucketId] = processedNotification
      notificationsByKey[parsedNotification.key] = processedNotification
   }

   suspend fun onNotificationDismissed(key: String) {
      val processedNotification = notificationsByKey.remove(key)
      if (processedNotification != null) {
         notifications.remove(processedNotification.bucketId)
      }

      watchSyncer.clearNotification(key)
   }

   suspend fun onNotificationsCleared() {
      notifications.clear()
      notificationsByKey.clear()

      watchSyncer.clearAllNotifications()
   }

   override fun getNotification(bucketId: Int): ProcessedNotification? {
      return notifications[bucketId]
   }
}
