package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class ActionHandlerImpl(
   private val notificationRepository: NotificationRepository,
   private val serviceController: NotificationServiceController,
) : ActionHandler {
   override suspend fun handleAction(notificationId: Int, actionIndex: Int): Boolean {
      val notification = notificationRepository.getNotification(notificationId) ?: return false
      val action = notification.actions.elementAtOrNull(actionIndex) ?: return false

      return handleAction(notification, action)
   }

   private fun handleAction(notification: ProcessedNotification, action: Action): Boolean {
      return when (action) {
         is Action.Dismiss -> {
            serviceController.cancelNotification(notification.systemData.key)
         }
      }
   }
}
