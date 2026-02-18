package com.matejdro.pebblenotificationcenter.notification

import android.content.Context
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.get
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
   private val ruleResolver: RuleResolver,
) : NotificationRepository {
   private val notifications = HashMap<Int, ProcessedNotification>()
   private val notificationsByKey = HashMap<String, ProcessedNotification>()

   private var nextVibration: AtomicReference<IntArray?> = AtomicReference(null)

   suspend fun onNotificationPosted(parsedNotification: ParsedNotification, suppressVibration: Boolean = false) {
      val (affectedRules, settings) = ruleResolver.resolveRules(parsedNotification)
      logcat { "Notification ${parsedNotification.key} rules: $affectedRules" }
      for (setting in settings.asMap()) {
         logcat { "   ${setting.key} = ${setting.value}" }
      }

      val masterSwitch = settings[RuleOption.masterSwitch]
      if (masterSwitch == RuleOption.MasterSwitch.HIDE) {
         onNotificationDismissed(parsedNotification.key)
         return
      }

      val actions = processActions(parsedNotification)

      val initialProcessedNotification = ProcessedNotification(parsedNotification, 0, actions, unread = !suppressVibration)
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
      @Suppress("ComplexCondition") // Lots of unrelated checks
      if (
         masterSwitch != RuleOption.MasterSwitch.MUTE &&
         noisy &&
         !parsedNotification.isFilteredByDoNotDisturb &&
         !identicalText
      ) {
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

   fun getNotificationByKey(key: String): ProcessedNotification? {
      return notificationsByKey[key]
   }

   override fun pollNextVibration(): IntArray? {
      return nextVibration.getAndSet(null)
   }

   override suspend fun markAsRead(bucketId: Int) {
      logcat { "Marking $bucketId as read" }
      val notification = notifications.computeIfPresent(bucketId) { _, value ->
         value.copy(unread = false)
      } ?: return

      notificationsByKey[notification.systemData.key] = notification
      watchSyncer.prepareNotificationReadStatus(notification)
   }
}
