package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import com.matejdro.pebblenotificationcenter.notification.FakeNotificationRepository
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import dispatch.core.DefaultCoroutineScope
import io.kotest.matchers.collections.shouldContainExactly
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
               ) +
                  // 83 'a' characters, followed by the ...
                  ByteArray(80) { 'a'.code.toByte() } +
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

   private fun TestScope.setup() {
      backgroundScope.launch {
         packetQueue.runQueue()
      }
   }
}
