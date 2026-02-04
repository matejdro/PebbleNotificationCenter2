package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebble.bluetooth.common.util.requireBytes
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import com.matejdro.pebblenotificationcenter.notification.FakeNotificationRepository
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import dispatch.core.DefaultCoroutineScope
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

   private val notificationDetailsPusher = NotificationDetailsPusherImpl(
      packetQueue,
      notificationRepository,
      DefaultCoroutineScope(scope.backgroundScope.coroutineContext)
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
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test

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
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test
               ) +
                  // 79 'a' characters, followed by the ...
                  ByteArray(79) { 'a'.code.toByte() } +
                  byteArrayOf(46, 46, 46)
            )
         )
      )
   }

   @Test
   fun `Cancel previous packets when new request is made`() = scope.runTest {
      setup()

      repeat(3) {
         notificationRepository.putNotification(
            it,
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

      notificationDetailsPusher.pushNotificationDetails(bucketId = 0, maxPacketSize = 100)
      runCurrent()

      notificationDetailsPusher.pushNotificationDetails(bucketId = 1, maxPacketSize = 100)
      runCurrent()

      notificationDetailsPusher.pushNotificationDetails(bucketId = 2, maxPacketSize = 100)
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
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

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
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

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
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

      runCurrent()

      sender.sentData.shouldHaveSize(1).elementAt(0).requireBytes(1u).get(1) shouldBe 20
   }

   @Test
   fun `Send blank packet when the notification does not exist`() = scope.runTest {
      setup()

      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test

                  // No text
               )
            )
         )
      )
   }

   @Test
   fun `Send send vibration after successful details push`() = scope.runTest {
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
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

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

      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)
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
      notificationDetailsPusher.pushNotificationDetails(bucketId = 12, maxPacketSize = 100)

      runCurrent()

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(5),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  12, // Notification id

                  0, // No actions in this test

                  // UTF8 Bytes for the text
                  194.toByte(), // UTF8 marker
                  160.toByte(), // Non-breaking space, not the input regular space
                  99, // c
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
