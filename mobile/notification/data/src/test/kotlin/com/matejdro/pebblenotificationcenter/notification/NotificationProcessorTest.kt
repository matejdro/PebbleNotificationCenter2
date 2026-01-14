package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.bluetooth.FakeWatchSyncer
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

class NotificationProcessorTest {
   private val watchSyncer = FakeWatchSyncer()

   private val processor = NotificationProcessor(watchSyncer)

   @Test
   fun `It should forward received notifications to the watch syncer`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldContainExactly(notification)
   }

   @Test
   fun `It should forward notification deletions to the watch syncer`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationDismissed("key")

      watchSyncer.clearedNotifications.shouldContainExactly("key")
   }

   @Test
   fun `It should forward notification clears to the watch syncer`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationsCleared()

      watchSyncer.clearAllCalled shouldBe true
   }

   @Test
   fun `It should allow getting received notifications`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      processor.getNotification(1) shouldBe ProcessedNotification(
         ParsedNotification(
            "key",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         ),
         bucketId = 1
      )
   }

   @Test
   fun `Dismissing notification should delete it from getNotificatoin query`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationDismissed(notification.key)

      processor.getNotification(1).shouldBeNull()
   }

   @Test
   fun `Clearing should clear internal notifications`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationsCleared()

      processor.getNotification(1).shouldBeNull()
   }
}
