package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.bluetooth.FakeWatchSyncer
import com.matejdro.pebblenotificationcenter.bluetooth.FakeWatchappOpenController
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.fakes.FakeActivity
import java.time.Instant

class NotificationProcessorTest {
   private val watchSyncer = FakeWatchSyncer()

   private val context = FakeActivity()

   private val openController = FakeWatchappOpenController()

   private val processor = NotificationProcessor(context, watchSyncer, openController)

   @BeforeEach
   fun setUp() {
      context.resources.putString(R.string.dismiss, "Dismiss")
   }

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

      assertSoftly(processor.getNotification(1).shouldNotBeNull()) {
         systemData shouldBe ParsedNotification(
            "key",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )

         bucketId shouldBe 1
      }
   }

   @Test
   fun `Dismissing notification should delete it from getNotification query`() = runTest {
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

   @Test
   fun `It should return dismiss action on every notification`() = runTest {
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

      processor.getNotification(1)?.actions shouldBe listOf(
         Action.Dismiss("Dismiss")
      )
   }

   @Test
   fun `It should not vibrate for the silent notifications by default`() = runTest {
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

      openController.watchappOpened shouldBe false
      processor.pollNextVibration() shouldBe null
   }

   @Test
   fun `It should not vibrate for the loud but filtered by dnd notifications by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false,
         isFilteredByDoNotDisturb = true
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe false
      processor.pollNextVibration() shouldBe null
   }

   @Test
   fun `It should vibrate for the non-silent notifications by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }
}
