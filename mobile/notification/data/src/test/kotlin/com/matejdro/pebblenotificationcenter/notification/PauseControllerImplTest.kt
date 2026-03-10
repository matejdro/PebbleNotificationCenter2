package com.matejdro.pebblenotificationcenter.notification

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

class PauseControllerImplTest {
   private val fakeRepo = FakeNotificationRepository()
   private val preferences = mutablePreferencesOf()

   private val pauseController = PauseControllerImpl({ fakeRepo })

   @Test
   fun `A notification should not be muted by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      fakeRepo.putNotification(1, ProcessedNotification(notification, 1))
      pauseController.onNewNotification(notification, preferences)

      pauseController.isNotificationPaused(notification) shouldBe false
   }

   @Test
   fun `A notification should be muted after toggling mute`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      fakeRepo.putNotification(1, ProcessedNotification(notification, 1))
      pauseController.onNewNotification(notification, preferences)
      fakeRepo.notifiedPackageStatusesChanged.shouldBeEmpty()

      pauseController.toggleAppPause(notification)

      pauseController.isNotificationPaused(notification) shouldBe true
      fakeRepo.notifiedPackageStatusesChanged.shouldContainExactly("com.app")
   }

   @Test
   fun `A notification should be unmuted after toggling mute twice`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      fakeRepo.putNotification(1, ProcessedNotification(notification, 1))
      pauseController.onNewNotification(notification, preferences)
      fakeRepo.notifiedPackageStatusesChanged.shouldBeEmpty()

      pauseController.toggleAppPause(notification)
      pauseController.toggleAppPause(notification)

      pauseController.isNotificationPaused(notification) shouldBe false
   }

   @Test
   fun `Notifications from a specific app should remain paused until all notifications from that app are gone`() = runTest {
      val notificationA = ParsedNotification(
         "keyA",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationB = ParsedNotification(
         "keyB",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationC = ParsedNotification(
         "keyC",
         "another.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      fakeRepo.putNotification(1, ProcessedNotification(notificationA, 1))
      pauseController.onNewNotification(notificationA, preferences)
      fakeRepo.putNotification(2, ProcessedNotification(notificationB, 2))
      pauseController.onNewNotification(notificationB, preferences)
      fakeRepo.putNotification(3, ProcessedNotification(notificationC, 3))
      pauseController.onNewNotification(notificationC, preferences)
      pauseController.toggleAppPause(notificationA)
      fakeRepo.notifiedPackageStatusesChanged.clear()

      pauseController.isNotificationPaused(notificationA) shouldBe true

      fakeRepo.removeNotification(1)
      pauseController.onNotificationDismissed(notificationA)
      pauseController.isNotificationPaused(notificationA) shouldBe true
      fakeRepo.notifiedPackageStatusesChanged.shouldBeEmpty()

      fakeRepo.removeNotification(2)
      pauseController.onNotificationDismissed(notificationB)
      pauseController.isNotificationPaused(notificationA) shouldBe false
      fakeRepo.notifiedPackageStatusesChanged.shouldContainExactly("com.app")
   }
}
