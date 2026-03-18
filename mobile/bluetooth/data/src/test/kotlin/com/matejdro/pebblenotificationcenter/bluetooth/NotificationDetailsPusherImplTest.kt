package com.matejdro.pebblenotificationcenter.bluetooth

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebble.bluetooth.common.util.requireBytes
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import com.matejdro.pebblenotificationcenter.bluetooth.images.FakeDrawableExtractor
import com.matejdro.pebblenotificationcenter.notification.FakeActionOrderRepository
import com.matejdro.pebblenotificationcenter.notification.FakeNotificationRepository
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import dispatch.core.DefaultCoroutineScope
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import java.time.Instant

class NotificationDetailsPusherImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val sender = FakePebbleSender(scope.virtualTimeProvider())
   private val packetQueue = PacketQueue(sender, WatchIdentifier("watch"), WATCHAPP_UUID)
   private val notificationRepository = FakeNotificationRepository()

   private val actionOrderRepository = FakeActionOrderRepository()

   private val drawableExtractor = FakeDrawableExtractor()

   private val notificationDetailsPusher = NotificationDetailsPusherImpl(
      packetQueue,
      notificationRepository,
      actionOrderRepository,
      drawableExtractor,
      DefaultCoroutineScope(scope.backgroundScope.coroutineContext),
   )

   @Test
   fun `Send text of the notification`() = scope.runTest {
      setup()

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test

                  0, 0, // No image

                  // Hello in UTf-8
                  72,
                  101,
                  108,
                  108,
                  111
               )
            )
         )
      )
   }

   @Test
   fun `Limit the text of the notification to the max packet size`() = scope.runTest {
      setup()

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "a".repeat(100),
               Instant.MIN,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test
                  0, 0, // No image
               ) +
                  // 77 'a' characters, followed by the ...
                  ByteArray(77) { 'a'.code.toByte() } +
                  byteArrayOf(46, 46, 46)
            )
         )
      )
   }

   @Test
   fun `Cancel previous packets when new request is made`() = scope.runTest {
      setup()

      repeat(3) { inex ->
         notificationRepository.putNotification(
            inex,
            ProcessedNotification(
               ParsedNotification(
                  "",
                  "",
                  "",
                  "",
                  "Hello",
                  Instant.MIN,
               )
            )
         )
      }

      sender.pauseSending = true

      notificationDetailsPusher.pushNotificationDetails(bucketId = 0, maxPacketSize = 100, colorWatch = false)
      runCurrent()

      notificationDetailsPusher.pushNotificationDetails(bucketId = 1, maxPacketSize = 100, colorWatch = false)
      runCurrent()

      notificationDetailsPusher.pushNotificationDetails(bucketId = 2, maxPacketSize = 100, colorWatch = false)
      runCurrent()

      sender.pauseSending = false
      runCurrent()

      // 2u should be skipped, because it never had the chance to be sent
      sender.sentData
         .map { (it.getValue(1u) as PebbleDictionaryItem.Bytes).value[0] }
         .shouldContainExactly(
            0,
            2,
         )
   }

   @Test
   fun `Send a list of actions of the notification`() = scope.runTest {
      setup()

      notificationRepository.putNotification(
         12,
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
               Action.Dismiss("A1"),
               Action.Dismiss("A2"),
               Action.Dismiss("A3"),
            )
         ),
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  3, // 3 actions
                  65, 49, 0, // A1 & null
                  65, 50, 0, // A2 & null
                  65, 51, 0, // A2 & null

                  0, 0, // No image

                  // Hello in UTf-8
                  72,
                  101,
                  108,
                  108,
                  111
               )
            )
         )
      )
   }

   @Test
   fun `Trim action text length`() = scope.runTest {
      setup()

      notificationRepository.putNotification(
         12,
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
               Action.Dismiss("a".repeat(100)),
            )
         ),
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  1, // 1 action
               ) +
                  // 17 'a' characters, followed by the ...
                  ByteArray(17) { 'a'.code.toByte() } +

                  byteArrayOf(
                     // ...
                     46, 46, 46,
                     0, // Null terminator

                     0, 0, // No image

                     // Notification body, Hello in UTf-8
                     72,
                     101,
                     108,
                     108,
                     111
                  )
            )
         )
      )
   }

   @Test
   fun `Allow maximum of 20 notification actions`() = scope.runTest {
      setup()

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = List(30) { Action.Dismiss(it.toString()) }
         ),
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldHaveSize(1).elementAt(0).requireBytes(1u).get(1) shouldBe 20
   }

   @Test
   fun `Send blank packet when the notification does not exist`() = scope.runTest {
      setup()

      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test
                  0, 0, // No image

                  // No text
               )
            )
         )
      )
   }

   @Test
   fun `Send vibration after successful details push`() = scope.runTest {
      setup()

      notificationRepository.nextVibration = intArrayOf(10, 10, 10, 10)

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldHaveSize(2).elementAt(1) shouldBe mapOf(
         0u to PebbleDictionaryItem.UInt8(7),
         1u to PebbleDictionaryItem.Bytes(
            byteArrayOf(
               0, 10,
               0, 10,
               0, 10,
               0, 10,
            )
         )
      )
   }

   @Test
   fun `Send vibration even if details are superseded by another notification`() = scope.runTest {
      setup()

      sender.pauseSending = true

      notificationRepository.nextVibration = intArrayOf(10, 10, 10, 10)
      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)
      runCurrent()

      notificationRepository.nextVibration = null
      notificationRepository.putNotification(
         13,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 13, maxPacketSize = 100, colorWatch = false)
      runCurrent()

      sender.pauseSending = false
      runCurrent()

      sender.sentData.map { it.getValue(0u) }.shouldContain(PebbleDictionaryItem.UInt8(7))
   }

   @Test
   fun `Do not send vibration of the previous notification when superseded by another notification with vibration`() =
      scope.runTest {
         setup()

         sender.pauseSending = true

         notificationRepository.nextVibration = intArrayOf(10, 10, 10, 10)
         notificationRepository.putNotification(
            12,
            ProcessedNotification(
               ParsedNotification(
                  "",
                  "",
                  "",
                  "",
                  "Hello",
                  Instant.MIN,
               )
            )
         )
         notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)
         runCurrent()

         notificationRepository.nextVibration = intArrayOf(20, 20, 20, 20)
         notificationRepository.putNotification(
            13,
            ProcessedNotification(
               ParsedNotification(
                  "",
                  "",
                  "",
                  "",
                  "Hello",
                  Instant.MIN,
               )
            )
         )
         notificationDetailsPusher.pushNotificationDetails(bucketId = 13, maxPacketSize = 100, colorWatch = false)
         runCurrent()

         sender.pauseSending = false
         runCurrent()

         sender.sentData.map { it.getValue(0u) }.shouldContainExactly(
            PebbleDictionaryItem.UInt8(5),
            PebbleDictionaryItem.UInt8(5),
            PebbleDictionaryItem.UInt8(7)
         )

         sender.sentData.last() shouldBe mapOf(
            0u to PebbleDictionaryItem.UInt8(7),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  0, 20,
                  0, 20,
                  0, 20,
                  0, 20,
               )
            )
         )
      }

   @Test
   fun `Mark notification as read on sending`() = scope.runTest {
      setup()

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            )
         )
      )

      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)
      runCurrent()

      notificationRepository.notificationsMarkedAsRead.shouldContainExactly(12)
   }

   @Test
   fun `Fix indentation of the text`() = scope.runTest {
      setup()

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               " c",
               Instant.MIN,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test
                  0, 0, // No image

                  // UTF8 Bytes for the text
                  194.toByte(), // UTF8 marker
                  160.toByte(), // Non-breaking space, not the input regular space
                  99, // c
               )
            )
         )
      )
   }

   @Test
   fun `It should respect action order from the action order repository`() = scope.runTest {
      actionOrderRepository.moveOrder("A1", 2)

      setup()

      notificationRepository.putNotification(
         12,
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
               Action.Dismiss("A1"),
               Action.Dismiss("A2"),
               Action.Dismiss("A3"),
            )
         ),
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  3, // 3 actions
                  65, 50, 0, // A2 & null
                  65, 51, 0, // A3 & null
                  65, 49, 0, // A1 & null

                  0, 0, // No image

                  // Hello in UTf-8
                  72,
                  101,
                  108,
                  108,
                  111
               )
            )
         )
      )
   }

   @Test
   fun `Send a notification icon`() = scope.runTest {
      val fakeDrawable = object : Drawable() {
         override fun draw(canvas: Canvas) {
            throw UnsupportedOperationException()
         }

         @Deprecated("Deprecated in Java")
         override fun getOpacity(): Int {
            throw UnsupportedOperationException()
         }

         override fun setAlpha(alpha: Int) {
            throw UnsupportedOperationException()
         }

         override fun setColorFilter(colorFilter: ColorFilter?) {
            throw UnsupportedOperationException()
         }
      }

      drawableExtractor.registerOutput(
         drawable = fakeDrawable,
         width = 32,
         height = 32,
         colorWatch = false,
         output = byteArrayOf(1, 2, 3)
      )

      setup()

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
               iconDrawable = fakeDrawable,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = false)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test

                  0, 3, // 3 Bytes for the image
                  // Image data
                  1,
                  2,
                  3,

                  // Hello in UTf-8
                  72,
                  101,
                  108,
                  108,
                  111
               )
            )
         )
      )
   }

   @Test
   fun `Send a colorful notification icon`() = scope.runTest {
      val fakeDrawable = object : Drawable() {
         override fun draw(canvas: Canvas) {
            throw UnsupportedOperationException()
         }

         @Deprecated("Deprecated in Java")
         override fun getOpacity(): Int {
            throw UnsupportedOperationException()
         }

         override fun setAlpha(alpha: Int) {
            throw UnsupportedOperationException()
         }

         override fun setColorFilter(colorFilter: ColorFilter?) {
            throw UnsupportedOperationException()
         }
      }

      drawableExtractor.registerOutput(
         drawable = fakeDrawable,
         width = 32,
         height = 32,
         colorWatch = true,
         output = byteArrayOf(1, 2, 3)
      )

      setup()

      notificationRepository.putNotification(
         12,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
               iconDrawable = fakeDrawable,
            )
         )
      )
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100, colorWatch = true)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test

                  0, 3, // 3 Bytes for the image
                  // Image data
                  1,
                  2,
                  3,

                  // Hello in UTf-8
                  72,
                  101,
                  108,
                  108,
                  111
               )
            )
         )
      )
   }

   private fun TestScope.setup() {
      backgroundScope.launch {
         packetQueue.runQueue()
      }
   }
}
