package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuController
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuType
import com.matejdro.pebblenotificationcenter.submenus.ReplySubmenuPayload
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(WatchappConnectionScope::class)
class SubmenuActionHandlerImpl(
   private val submenuController: SubmenuController,
   private val serviceController: NotificationServiceController,
) : SubmenuActionHandler {
   override fun handleSubmenuAction(notificationId: UByte, menu: SubmenuType, index: Int): Boolean {
      val payload = submenuController.getPayloadForMenuItem<Any>(notificationId, menu, index) ?: return false

      return when (menu) {
         SubmenuType.REPLY_ANSWERS -> handleReplyMenuAction(payload)
         SubmenuType.OTHER -> throw UnsupportedOperationException("Other is only meant for tests")
      }
   }

   private fun handleReplyMenuAction(payload: Any): Boolean {
      payload as ReplySubmenuPayload

      return serviceController.triggerReplyAction(
         payload.intent,
         payload.remoteInputResultKey,
         payload.text
      )
   }
}
