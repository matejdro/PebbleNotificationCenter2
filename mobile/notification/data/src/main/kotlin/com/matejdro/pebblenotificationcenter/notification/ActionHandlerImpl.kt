package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat

@ContributesBinding(AppScope::class)
@Inject
class ActionHandlerImpl(
   private val notificationRepository: NotificationRepository,
   private val serviceController: NotificationServiceController,
) : ActionHandler {
   override suspend fun handleAction(notificationId: Int, actionIndex: Int): Boolean {
      val notification = notificationRepository.getNotification(notificationId)
      if (notification == null) {
         logcat { "Got action for unknown notificationId: $notificationId, skipping..." }
         return false
      }

      val action = notification.actions.elementAtOrNull(actionIndex)
      if (action == null) {
         logcat { "Got action for unknown actionIndex: $actionIndex, notification has ${notification.actions.size} actions..." }
         return false
      }

      return handleAction(notification, action)
   }

   private fun handleAction(notification: ProcessedNotification, action: Action): Boolean {
      logcat { "Handle action: $action" }
      return when (action) {
         is Action.Dismiss -> {
            serviceController.cancelNotification(notification.systemData.key)
         }

         is Action.Native -> {
            serviceController.triggerAction(action.intent)
         }
      }
   }
}
