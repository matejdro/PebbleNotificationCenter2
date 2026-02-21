package com.matejdro.pebblenotificationcenter.notification

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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

      if (shouldHide(parsedNotification, settings)) {
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

      val vibrationPattern = getVibrationPattern(
         previousNotification,
         parsedNotification,
         suppressVibration,
         settings
      )
      if (vibrationPattern != null) {
         nextVibration.set(vibrationPattern)
         openController.openWatchapp()
      }
   }

   private fun shouldHide(
      notification: ParsedNotification,
      preferences: Preferences,
   ): Boolean {
      if (preferences[RuleOption.masterSwitch] == RuleOption.MasterSwitch.HIDE) {
         logcat { "Hiding: master switch is hidden" }
         return true
      }

      if (notification.isOngoing && preferences[RuleOption.hideOngoingNotifications]) {
         logcat { "Hiding: ongoing" }
         return true
      }

      if (notification.groupSummary && preferences[RuleOption.hideGroupSummaryNotifications]) {
         logcat { "Hiding: group summary" }
         return true
      }

      if (notification.localOnly && preferences[RuleOption.hideLocalOnlyNotifications]) {
         logcat { "Hiding: local only" }
         return true
      }

      if (notification.media) {
         logcat { "Hiding: media" }
         return true
      }

      return false
   }

   private fun getVibrationPattern(
      previousNotification: ProcessedNotification?,
      notification: ParsedNotification,
      suppressVibration: Boolean,
      preferences: Preferences,
   ): IntArray? {
      if (suppressVibration) {
         logcat { "Not vibrating: suppressVibration flag" }
         return null
      }

      if (preferences[RuleOption.masterSwitch] == RuleOption.MasterSwitch.MUTE) {
         logcat { "Not vibrating: master switch" }
         return null
      }

      if (notification.isSilent && preferences[RuleOption.muteSilentNotifications]) {
         logcat { "Not vibrating: silent notification" }
         return null
      }

      if (notification.isFilteredByDoNotDisturb && preferences[RuleOption.muteDndNotifications]) {
         logcat { "Not vibrating: DND filter" }
         return null
      }

      val identicalText = previousNotification != null &&
         previousNotification.systemData.title == notification.title &&
         previousNotification.systemData.subtitle == notification.subtitle &&
         previousNotification.systemData.body == notification.body

      if (identicalText && preferences[RuleOption.muteIdenticalNotifications]) {
         logcat { "Not vibrating: identical text notification" }
         return null
      }

      // Until settings are there, just hardcode jackhammer
      @Suppress("MagicNumber")
      return intArrayOf(50, 50, 50, 50, 50, 50, 50, 50, 50, 50)
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
