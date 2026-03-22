package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuController
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuType
import com.matejdro.pebblenotificationcenter.submenus.ReplySubmenuPayload
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration

@Inject
@ContributesBinding(WatchappConnectionScope::class)
class SubmenuActionHandlerImpl(
   private val submenuController: SubmenuController,
   private val serviceController: NotificationServiceController,
   private val notificationRepository: NotificationRepository,
) : SubmenuActionHandler {
   override fun handleSubmenuAction(notificationId: UByte, menu: SubmenuType, index: Int, voiceInputText: String?): Boolean {
      val payload = submenuController.getPayloadForMenuItem<Any>(notificationId, menu, index) ?: return false

      return when (menu) {
         SubmenuType.REPLY_ANSWERS -> handleReplyMenuAction(payload, voiceInputText)
         SubmenuType.SNOOZE -> handleSnoozeMenuAction(payload, notificationId)
      }
   }

   private fun handleReplyMenuAction(payload: Any, voiceInputText: String?): Boolean {
      payload as ReplySubmenuPayload

      return serviceController.triggerReplyAction(
         pendingIntent = payload.intent,
         remoteInputKey = payload.remoteInputResultKey,
         text = voiceInputText ?: payload.text
      )
   }

   private fun handleSnoozeMenuAction(payload: Any, notificationId: UByte): Boolean {
      payload as Duration

      val notification = notificationRepository.getNotification(notificationId.toInt()) ?: return false

      return serviceController.snoozeNotificationNotification(
         notification.systemData.key,
         payload
      )
   }
}
