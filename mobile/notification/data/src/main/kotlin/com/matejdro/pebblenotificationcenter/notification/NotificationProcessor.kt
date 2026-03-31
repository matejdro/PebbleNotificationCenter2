package com.matejdro.pebblenotificationcenter.notification

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.matejdro.pebblenotificationcenter.bluetooth.WatchSyncer
import com.matejdro.pebblenotificationcenter.bluetooth.WatchappOpenController
import com.matejdro.pebblenotificationcenter.common.di.AndroidVersion
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.PauseStatus
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import com.matejdro.pebblenotificationcenter.notification.model.any
import com.matejdro.pebblenotificationcenter.notification.utils.parseVibrationPattern
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.MasterSwitch
import com.matejdro.pebblenotificationcenter.rules.RuleOption
import com.matejdro.pebblenotificationcenter.rules.keys.get
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
   @AndroidVersion
   private val androidVersion: Int,
) : NotificationRepository {
   private val notifications = ConcurrentHashMap<Int, ProcessedNotification>()
   private val notificationIdsByKeys = HashMap<String, Int>()

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

      val isUpdate = notificationIdsByKeys.containsKey(parsedNotification.key)
      val pauseStatusBeforeInsert = pauseController.computePauseStatus(parsedNotification)
      if (!isUpdate) {
         pauseController.onNewNotification(parsedNotification, settings)
      }
      val pauseStatus = pauseController.computePauseStatus(parsedNotification)

      val actions = processActions(parsedNotification, pauseStatus)

      val previousNotification = notificationIdsByKeys[parsedNotification.key]?.let { notifications[it] }
      val vibrationPattern = getVibrationPattern(
         previousNotification,
         parsedNotification,
         suppressVibration,
         settings,
         pauseStatusBeforeInsert
      )

      val initialProcessedNotification = ProcessedNotification(
         parsedNotification,
         0,
         actions,
         unread = !suppressVibration,
         paused = pauseStatus,
         vibrated = vibrationPattern != null
      )
      val bucketId = watchSyncer.syncNotification(initialProcessedNotification, settings)

      val processedNotification = initialProcessedNotification.copy(bucketId = bucketId)

      logcat {
         "Notification flags: " +
            "suppress=$suppressVibration " +
            "silent=${parsedNotification.isSilent} " +
            "dnd=${parsedNotification.isFilteredByDoNotDisturb}"
      }
      if (vibrationPattern != null) {
         logcat { "Vibrating with ${vibrationPattern.contentToString()}" }
         nextVibration.set(vibrationPattern)
         openController.openWatchapp()
      }

      notifications[bucketId] = processedNotification
      notificationIdsByKeys[parsedNotification.key] = bucketId
   }

   private fun shouldHide(
      notification: ParsedNotification,
      preferences: Preferences,
   ): Boolean {
      if (notification.forceVibrate) {
         logcat { "Force notification: always show" }
         return false
      }

      if (preferences[RuleOption.masterSwitch] == MasterSwitch.HIDE) {
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

      if (preferences[RuleOption.masterSwitch] == MasterSwitch.MUTE) {
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

   @Suppress("CognitiveComplexMethod") // A bunch of ifs for separate actions. Clearer when left together.
   private fun processActions(parsedNotification: ParsedNotification, pauseStatus: PauseStatus): List<Action> {
      val ncActions = buildList {
         add(Action.Dismiss(title = context.getString(R.string.dismiss), id = size.toUByte()))

         if (androidVersion >= Build.VERSION_CODES.O) {
            add(Action.Snooze(title = context.getString(R.string.snooze), id = size.toUByte()))
         }

         if (parsedNotification.largeImage != null) {
            add(Action.ShowImage(title = context.getString(R.string.show_image), id = size.toUByte()))
         }

         add(
            Action.PauseApp(
               title = if (pauseStatus.app) {
                  context.getString(R.string.unpause_app)
               } else {
                  context.getString(R.string.pause_app)
               },
               id = size.toUByte()
            )
         )
         add(
            Action.PauseConversation(
               title = if (pauseStatus.conversation) {
                  context.getString(R.string.unpause_conversation)
               } else {
                  context.getString(R.string.pause_conversation)
               },
               id = size.toUByte()
            )
         )
      }

      val appActions = parsedNotification.nativeActions.mapIndexed { index, action ->
         val text = if (ncActions.any { it.title == action.text }) {
            context.getString(R.string.app_suffix, action.text)
         } else {
            action.text
         }

         val id = (ncActions.size + index).toUByte()

         val remoteInputResultKey = action.remoteInputResultKey
         if (remoteInputResultKey == null) {
            Action.Native(title = text, intent = action.pendingIntent, id)
         } else {
            Action.Reply(
               title = text,
               intent = action.pendingIntent,
               remoteInputResultKey = remoteInputResultKey,
               cannedTexts = action.cannedTexts,
               allowFreeFormInput = action.allowFreeFormInput,
               id = id
            )
         }
      }

      return ncActions + appActions
   }

   override suspend fun notifyPackagePauseStatusChanged(pkg: String) {
      val activeMatchingNotifications = getAllActiveNotifications().filter { it.systemData.pkg == pkg }
      for (notificationIterator in activeMatchingNotifications) {
         val newNotification = notifications.compute(notificationIterator.bucketId) { _, oldNotification ->
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
               val preferences = ruleResolver.resolveRules(newNotification.systemData).preferences
               watchSyncer.syncNotification(newNotification, preferences)
            }
         }
      }
   }

   suspend fun onNotificationDismissed(key: String) {
      val notificationId = notificationIdsByKeys.remove(key)
      if (notificationId != null) {
         val processedNotification = notifications.remove(notificationId)
         if (processedNotification != null) {
            pauseController.onNotificationDismissed(processedNotification.systemData)
         }
      }

      watchSyncer.clearNotification(key)
   }

   suspend fun onNotificationsCleared() {
      notifications.clear()
      notificationIdsByKeys.clear()

      watchSyncer.clearAllNotifications()
   }

   override fun getNotification(bucketId: Int): ProcessedNotification? {
      return notifications[bucketId]
   }

   override fun getAllActiveNotifications(): Collection<ProcessedNotification> {
      return notifications.values.toList()
   }

   fun getNotificationByKey(key: String): ProcessedNotification? {
      return notificationIdsByKeys[key]?.let { notifications[it] }
   }

   override fun pollNextVibration(): IntArray? {
      return nextVibration.getAndSet(null)
   }

   override suspend fun markAsRead(bucketId: Int) {
      logcat { "Marking $bucketId as read" }
      val notification = notifications.computeIfPresent(bucketId) { _, value ->
         value.copy(unread = false)
      } ?: return

      val preferences = ruleResolver.resolveRules(notification.systemData).preferences

      watchSyncer.prepareNotificationReadStatus(notification, preferences)
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
