package com.matejdro.pebblenotificationcenter.notification

import android.app.PendingIntent
import android.content.res.Resources
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuController
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuItem
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuType
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import com.matejdro.pebblenotificationcenter.rules.RuleOption
import com.matejdro.pebblenotificationcenter.rules.keys.get
import com.matejdro.pebblenotificationcenter.submenus.ReplySubmenuPayload
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat
import kotlin.time.Duration.Companion.minutes

@ContributesBinding(WatchappConnectionScope::class)
@Inject
class ActionHandlerImpl(
   private val notificationRepository: NotificationRepository,
   private val serviceController: NotificationServiceController,
   private val submenuController: SubmenuController,
   private val ruleResolver: RuleResolver,
   private val resources: Resources,
   private val pauseController: PauseController,
) : ActionHandler {
   override suspend fun handleAction(notificationId: Int, actionId: Int): Boolean {
      val notification = notificationRepository.getNotification(notificationId)
      if (notification == null) {
         logcat { "Got action for unknown notificationId: $notificationId, skipping..." }
         return false
      }

      val action = notification.actions.firstOrNull { it.id == actionId.toUByte() }
      if (action == null) {
         logcat { "Got action for unknown actionIndex: $actionId, notification has ${notification.actions.size} actions..." }
         return false
      }

      return handleAction(notification, action)
   }

   private suspend fun handleAction(notification: ProcessedNotification, action: Action): Boolean {
      logcat { "Handle action: $action" }
      return when (action) {
         is Action.Dismiss -> {
            serviceController.cancelNotification(notification.systemData.key)
         }

         is Action.Native -> {
            serviceController.triggerAction(action.intent)
         }

         is Action.Reply -> {
            handleReplyAction(action, notification)
         }

         is Action.PauseApp -> {
            pauseController.toggleAppPause(notification.systemData)
            true
         }

         is Action.PauseConversation -> {
            pauseController.toggleConversationPause(notification.systemData)
            true
         }

         is Action.Snooze -> {
            handleSnoozeAction(notification)
         }
      }
   }

   private suspend fun handleReplyAction(
      action: Action.Reply,
      notification: ProcessedNotification,
   ): Boolean {
      val voiceItem = if (action.allowFreeFormInput) {
         listOf(
            SubmenuItem(
               resources.getString(R.string.voice),
               ReplySubmenuPayload("", action.intent as PendingIntent, action.remoteInputResultKey),
               voiceInput = true
            )
         )
      } else {
         emptyList()
      }

      val userTexts = if (action.allowFreeFormInput) {
         val preferences = ruleResolver.resolveRules(notification.systemData).preferences
         preferences[RuleOption.replyCannedTexts]
      } else {
         emptyList()
      }

      val cannedTexts = userTexts + action.cannedTexts
      val listItems = voiceItem + cannedTexts.map { cannedText ->
         SubmenuItem(
            cannedText,
            ReplySubmenuPayload(cannedText, action.intent as PendingIntent, action.remoteInputResultKey)
         )
      }

      submenuController.showSubmenuOnTheWatch(
         notification.bucketId.toUByte(),
         SubmenuType.REPLY_ANSWERS,
         listItems
      )

      return true
   }

   private suspend fun handleSnoozeAction(notification: ProcessedNotification): Boolean {
      val preferences = ruleResolver.resolveRules(notification.systemData).preferences

      val listItems = preferences[RuleOption.snoozeIntervals].map { snoozeMinutes ->
         SubmenuItem(
            resources.getString(R.string.minutes_suffix_short, snoozeMinutes),
            snoozeMinutes.minutes
         )
      }

      submenuController.showSubmenuOnTheWatch(
         notification.bucketId.toUByte(),
         SubmenuType.SNOOZE,
         listItems
      )

      return true
   }
}
