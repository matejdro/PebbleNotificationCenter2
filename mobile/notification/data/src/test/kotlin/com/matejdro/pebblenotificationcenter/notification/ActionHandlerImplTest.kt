package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.FakeNotificationServiceController
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

class ActionHandlerImplTest {
   private val repo = FakeNotificationRepository()
   private val controller = FakeNotificationServiceController()

   private val handler = ActionHandlerImpl(repo, controller)

   @Test
   fun `Return false when notification does not exist in the repo`() = runTest {
      handler.handleAction(2, 2) shouldBe false
   }

   @Test
   fun `Return false when notification exists but the action does not exist`() = runTest {
      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = emptyList()
         ),
      )

      handler.handleAction(2, 2) shouldBe false
   }

   @Test
   fun `Return false when notification and actions exist but the controller fails with dismissal`() = runTest {
      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = listOf(
               Action.Dismiss("Dismiss")
            )
         ),
      )

      controller.returnValue = false

      handler.handleAction(2, 0) shouldBe false
   }

   @Test
   fun `Forward dismiss action to controller`() = runTest {
      repo.putNotification(
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
            actions = listOf(
               Action.Dismiss("Dismiss")
            )
         ),
      )

      controller.returnValue = true

      handler.handleAction(2, 0) shouldBe true

      controller.lastCancelledNotification shouldBe "keyNotification"
   }
}
