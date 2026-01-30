package com.matejdro.pebblenotificationcenter.notification

import android.content.Context
import com.matejdro.pebblenotificationcenter.bluetooth.WatchSyncer
import com.matejdro.pebblenotificationcenter.bluetooth.WatchappOpenController
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import logcat.logcat
import java.util.concurrent.atomic.AtomicReference

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NotificationProcessor(
   private val context: Context,
   private val watchSyncer: WatchSyncer,
   private val openController: WatchappOpenController,
) : NotificationRepository {
   private val notifications = HashMap<Int, ProcessedNotification>()
   private val notificationsByKey = HashMap<String, ProcessedNotification>()

   private var nextVibration: AtomicReference<IntArray?> = AtomicReference(null)

   suspend fun onNotificationPosted(parsedNotification: ParsedNotification, suppressVibration: Boolean = false) {
      val actions = processActions(parsedNotification)

      val initialProcessedNotification = ProcessedNotification(parsedNotification, 0, actions)
      val bucketId = watchSyncer.syncNotification(initialProcessedNotification)

      val processedNotification = initialProcessedNotification.copy(bucketId = bucketId)

      val previousNotification = notificationsByKey[parsedNotification.key]
      notifications[bucketId] = processedNotification
      notificationsByKey[parsedNotification.key] = processedNotification

      logcat {
         "Notification flags: " +
            "suppress=$suppressVibration " +
            "silent=${parsedNotification.isSilent} " +
            "dnd=${parsedNotification.isFilteredByDoNotDisturb}"
      }

      val identicalText = previousNotification != null &&
         previousNotification.systemData.title == parsedNotification.title &&
         previousNotification.systemData.subtitle == parsedNotification.subtitle &&
         previousNotification.systemData.body == parsedNotification.body

      val noisy = !suppressVibration && !parsedNotification.isSilent
      if (noisy && !parsedNotification.isFilteredByDoNotDisturb && !identicalText) {
         nextVibration.set(
            // Until settings are there, just hardcode jackhammer
            @Suppress("MagicNumber")
            intArrayOf(50, 50, 50, 50, 50, 50, 50, 50, 50, 50)
         )
         openController.openWatchapp()
      }
   }

   private fun processActions(parsedNotification: ParsedNotification): List<Action> {
      val defaultActions = listOf<Action>(
         Action.Dismiss(context.getString(R.string.dismiss)),
      )

      val nativeActions = parsedNotification.nativeActions.map { Action.Native(it.text, it.pendingIntent) }

      return defaultActions + nativeActions
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

   override fun pollNextVibration(): IntArray? {
      return nextVibration.getAndSet(null)
   }
}
