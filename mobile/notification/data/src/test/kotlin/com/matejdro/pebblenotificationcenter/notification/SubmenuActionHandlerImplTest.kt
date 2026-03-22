package com.matejdro.pebblenotificationcenter.notification

import android.app.createPendingIntent
import com.matejdro.pebblenotificationcenter.FakeNotificationServiceController
import com.matejdro.pebblenotificationcenter.bluetooth.FakeSubmenuController
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuItem
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuType
import com.matejdro.pebblenotificationcenter.notification.model.NativeAction
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import com.matejdro.pebblenotificationcenter.submenus.ReplySubmenuPayload
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class SubmenuActionHandlerImplTest {
   private val submenuController = FakeSubmenuController()
   private val serviceController = FakeNotificationServiceController()
   private val notifications = FakeNotificationRepository()

   private val actionHandler = SubmenuActionHandlerImpl(submenuController, serviceController, notifications)

   @Test
   fun `Send reply response to the service controller`() = runTest {
      val intent1 = createPendingIntent()
      val intent2 = createPendingIntent()

      submenuController.showSubmenuOnTheWatch(
         2u,
         SubmenuType.REPLY_ANSWERS,
         listOf(
            SubmenuItem(
               "Short text 1",
               ReplySubmenuPayload("Longer text 1", intent1, "inputKey")
            ),
            SubmenuItem(
               "Short text 2",
               ReplySubmenuPayload("Longer text 2", intent2, "inputKey")
            ),
         )
      )

      actionHandler.handleSubmenuAction(2u, SubmenuType.REPLY_ANSWERS, 1)

      serviceController.lastTriggeredReplyAction shouldBe
         NativeAction("Longer text 2", intent2, "inputKey")
   }

   @Test
   fun `Send voice reply response to the service controller`() = runTest {
      val intent1 = createPendingIntent()
      val intent2 = createPendingIntent()

      submenuController.showSubmenuOnTheWatch(
         2u,
         SubmenuType.REPLY_ANSWERS,
         listOf(
            SubmenuItem(
               "Short text 1",
               ReplySubmenuPayload("Longer text 1", intent1, "inputKey")
            ),
            SubmenuItem(
               "Short text 2",
               ReplySubmenuPayload("Longer text 2", intent2, "inputKey")
            ),
         )
      )

      actionHandler.handleSubmenuAction(
         2u,
         SubmenuType.REPLY_ANSWERS,
         0,
         "Hello from the watch"
      )

      serviceController.lastTriggeredReplyAction shouldBe
         NativeAction("Hello from the watch", intent1, "inputKey")
   }

   @Test
   fun `Send snooze response to the service controller`() = runTest {
      notifications.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "keyNotification",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            bucketId = 2
         ),
      )

      submenuController.showSubmenuOnTheWatch(
         2u,
         SubmenuType.SNOOZE,
         listOf(
            SubmenuItem(
               "10 min",
               10.minutes
            ),
            SubmenuItem(
               "10 min",
               20.minutes
            ),
         )
      )

      actionHandler.handleSubmenuAction(2u, SubmenuType.SNOOZE, 0)

      serviceController.lastSnooze shouldBe
         ("keyNotification" to 10.minutes)
   }
}
