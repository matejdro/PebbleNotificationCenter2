package com.matejdro.pebblenotificationcenter.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.matejdro.notificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.pebblenotificationcenter.bluetooth.WatchSyncer
import com.matejdro.pebblenotificationcenter.bluetooth.WatchappOpenController
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.PauseStatus
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import com.matejdro.pebblenotificationcenter.notification.model.any
import com.matejdro.pebblenotificationcenter.notification.utils.parseVibrationPattern
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import logcat.logcat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NotificationProcessor(
   private val context: Context,
   private val watchSyncer: WatchSyncer,
   private val openController: WatchappOpenController,
   private val ruleResolver: RuleResolver,
   private val globalPreferenceStore: DataStore<Preferences>,
   private val pauseController: PauseController,
) : NotificationRepository {
   private val notifications = ConcurrentHashMap<Int, ProcessedNotification>()
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

      val isUpdate = notificationsByKey.containsKey(parsedNotification.key)
      val pauseStatusBeforeInsert = pauseController.computePauseStatus(parsedNotification)
      if (!isUpdate) {
         pauseController.onNewNotification(parsedNotification, settings)
      }
      val pauseStatus = pauseController.computePauseStatus(parsedNotification)

      val actions = processActions(parsedNotification, pauseStatus)

      val initialProcessedNotification = ProcessedNotification(
         parsedNotification,
         0,
         actions,
         unread = !suppressVibration,
         paused = pauseStatus
      )
      val bucketId = watchSyncer.syncNotification(initialProcessedNotification)

      val processedNotification = initialProcessedNotification.copy(bucketId = bucketId)

      val previousNotification = notificationsByKey[parsedNotification.key]

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
         settings,
         pauseStatusBeforeInsert
      )
      if (vibrationPattern != null) {
         logcat { "Vibrating with ${vibrationPattern.contentToString()}" }
         nextVibration.set(vibrationPattern)
         openController.openWatchapp()
      }

      notifications[bucketId] = processedNotification
      notificationsByKey[parsedNotification.key] = processedNotification
   }

   private fun shouldHide(
      notification: ParsedNotification,
      preferences: Preferences,
   ): Boolean {
      if (notification.forceVibrate) {
         logcat { "Force notification: always show" }
         return false
      }

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

      if (notification.media && preferences[RuleOption.hideMediaNotifications]) {
         logcat { "Hiding: media" }
         return true
      }

      return false
   }

   @Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod") // Lots of successive checks
   private suspend fun getVibrationPattern(
      previousNotification: ProcessedNotification?,
      notification: ParsedNotification,
      suppressVibration: Boolean,
      preferences: Preferences,
      pausedBeforeInsert: PauseStatus,
   ): IntArray? {
      val pattern = (
         notification.overrideVibrationPattern
            ?: parseVibrationPattern(preferences[RuleOption.vibrationPattern])
            ?: error("Invalid vibration pattern '${preferences[RuleOption.vibrationPattern]}'")
         )
         .map { it.toInt() }
         .toIntArray()

      if (notification.forceVibrate) {
         logcat { "Force notification: always vibrate" }
         return pattern
      }

      if (suppressVibration) {
         logcat { "Not vibrating: suppressVibration flag" }
         return null
      }

      if (globalPreferenceStore.data.first()[GlobalPreferenceKeys.muteWatch]) {
         logcat { "Not vibrating: watch muted" }
         return null
      }

      if (pausedBeforeInsert.any) {
         logcat { "Not vibrating: paused" }
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

      return pattern
   }

   private fun processActions(parsedNotification: ParsedNotification, pauseStatus: PauseStatus): List<Action> {
      val ncActions = listOf<Action>(
         Action.Dismiss(context.getString(R.string.dismiss)),
         Action.PauseApp(
            if (pauseStatus.app) {
               context.getString(R.string.unpause_app)
            } else {
               context.getString(R.string.pause_app)
            }
         ),
         Action.PauseConversation(
            if (pauseStatus.conversation) {
               context.getString(R.string.unpause_conversation)
            } else {
               context.getString(R.string.pause_conversation)
            }
         ),
      )

      val appActions = parsedNotification.nativeActions.map { action ->
         val text = if (ncActions.any { it.title == action.text }) {
            context.getString(R.string.app_suffix, action.text)
         } else {
            action.text
         }

         val remoteInputResultKey = action.remoteInputResultKey
         if (remoteInputResultKey == null) {
            Action.Native(title = text, intent = action.pendingIntent)
         } else {
            Action.Reply(
               title = text,
               intent = action.pendingIntent,
               remoteInputResultKey = remoteInputResultKey,
               cannedTexts = action.cannedTexts,
               allowFreeFormInput = action.allowFreeFormInput
            )
         }
      }

      return ncActions + appActions
   }

   override suspend fun notifyPackagePauseStatusChanged(pkg: String) {
      val activeMatchingNotifications = getAllActiveNotifications().filter { it.systemData.pkg == pkg }
      for (notificationIterator in activeMatchingNotifications) {
         val newNotification = notificationsByKey.compute(notificationIterator.systemData.key) { _, oldNotification ->
            if (oldNotification == null) {
               return@compute null
            }
            val newPaused = pauseController.computePauseStatus(oldNotification.systemData)

            if (newPaused != oldNotification.paused) {
               oldNotification.copy(
                  paused = newPaused,
                  actions = oldNotification.actions.renamePauseActions(newPaused)
               )
            } else {
               oldNotification
            }
         }
         if (newNotification != null) {
            notifications[newNotification.bucketId] = newNotification

            if (newNotification != notificationIterator) {
               watchSyncer.syncNotification(newNotification)
            }
         }
      }
   }

   suspend fun onNotificationDismissed(key: String) {
      val processedNotification = notificationsByKey.remove(key)
      if (processedNotification != null) {
         notifications.remove(processedNotification.bucketId)
         pauseController.onNotificationDismissed(processedNotification.systemData)
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

   override fun getAllActiveNotifications(): Collection<ProcessedNotification> {
      return notifications.values.toList()
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

   private fun List<Action>.renamePauseActions(newPausedStatus: PauseStatus): List<Action> = map { action ->
      when (action) {
         is Action.PauseApp -> {
            action.copy(
               title = if (newPausedStatus.app) {
                  context.getString(R.string.unpause_app)
               } else {
                  context.getString(R.string.pause_app)
               },
            )
         }

         is Action.PauseConversation -> {
            action.copy(
               title = if (newPausedStatus.conversation) {
                  context.getString(R.string.unpause_conversation)
               } else {
                  context.getString(R.string.pause_conversation)
               },
            )
         }

         else -> {
            action
         }
      }
   }
}
