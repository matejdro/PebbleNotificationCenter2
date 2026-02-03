package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncWatchLoopImpl
import com.matejdro.bucketsync.FakeBucketSyncRepository
import com.matejdro.bucketsync.background.FakeBackgroundSyncNotifier
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import com.matejdro.pebblenotificationcenter.notification.FakeActionHandler
import com.matejdro.pebblenotificationcenter.notification.FakeNotificationRepository
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import kotlin.time.Duration.Companion.seconds

class WatchappConnectionImplTest {
   private val scope = TestScopeWithDispatcherProvider()

   private val sender = FakePebbleSender(scope.virtualTimeProvider())
   private val bucketSyncRepository = FakeBucketSyncRepository()

   private val watchappOpenController = FakeWatchappOpenController()

   private val notificationDetailsPusher = FakeNotificationDetailsPusher()

   private val notificationsRepository = FakeNotificationRepository()

   private val actionHandler = FakeActionHandler()

   private val watch = WatchIdentifier("watch")

   private val packetQueue = PacketQueue(sender, watch, WATCHAPP_UUID)

   private val connection = WatchappConnectionImpl(
      scope.backgroundScope,
      watchappOpenController,
      packetQueue,
      BucketSyncWatchLoopImpl(
         scope.backgroundScope,
         packetQueue,
         bucketSyncRepository,
         watchappOpenController,
         FakeBackgroundSyncNotifier(),
         watch,
      ),
      notificationDetailsPusher,
      actionHandler,
      notificationsRepository,
      watch
   )

   @Test
   fun `Nack unknown packets`() = scope.runTest {
      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(255u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Nack
   }

   @Test
   fun `Send only version back when watch packets do not match`() = scope.runTest {
      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(0u),
            1u to PebbleDictionaryItem.UInt32(PROTOCOL_VERSION + 1u),
            2u to PebbleDictionaryItem.UInt32(1u),
            3u to PebbleDictionaryItem.UInt32(1000u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
         )
      )
   }

   @Test
   fun `Send a list of updated buckets`() = scope.runTest {
      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))

      val result = receiveStandardHelloPacket(bufferSize = 38u)
      runCurrent()

      result shouldBe ReceiveResult.Ack

      sender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(1u),
            1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION),
            2u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  0, // Status
                  0, 2, // Latest version
                  2, // Num of active buckets
                  1, 0, // Metadata for bucket 1
                  2, 0, // Metadata for bucket 2
                  1, 1, 1, // Sync data for bucket 1
               )
            ),
         ),
         mapOf(
            0u to PebbleDictionaryItem.UInt8(3u),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  1, // Status
                  2, 1, 2, // Sync data for bucket 2
               )
            ),
         )
      )
   }

   @Test
   fun `Send bucketsync data after Acking first packet`() = scope.runTest {
      sender.pauseSending = true

      val result = async { receiveStandardHelloPacket() }
      runCurrent()

      result.getCompleted() shouldBe ReceiveResult.Ack
   }

   @Test
   fun `Send auto-close flag when watchapp was started by auto sync`() = scope.runTest {
      watchappOpenController.setNextWatchappOpenForAutoSync()

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))

      receiveStandardHelloPacket(bufferSize = 61u)
      runCurrent()

      sender.sentData.first().shouldContainKey(3u)
   }

   @Test
   fun `Send auto-close flag when watchapp was started by auto sync and there are no updates`() = scope.runTest {
      watchappOpenController.setNextWatchappOpenForAutoSync()

      receiveStandardHelloPacket(bufferSize = 61u)
      runCurrent()

      sender.sentData.first().shouldContainKey(3u)
   }

   @Test
   fun `Push notification details on receive of the notification opened packet`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(4u),
            1u to PebbleDictionaryItem.UInt32(12u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      notificationDetailsPusher.lastPushRequestId shouldBe 12
      notificationDetailsPusher.lastMaxPacketSize shouldBe 123
   }

   @Test
   fun `Ignore notification details packets before valid hello packet`() = scope.runTest {
      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(4u),
            1u to PebbleDictionaryItem.UInt32(12u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      notificationDetailsPusher.lastPushRequestId.shouldBeNull()
   }

   @Test
   fun `Trigger action handler on activate action packet`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(6u),
            1u to PebbleDictionaryItem.UInt32(10u),
            2u to PebbleDictionaryItem.UInt32(5u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      actionHandler.lastHandledAction shouldBe FakeActionHandler.HandledAction(10, 5)
   }

   @Test
   fun `Return nack on activate action packet when action handling fails`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      actionHandler.returnValue = false

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(6u),
            1u to PebbleDictionaryItem.UInt32(10u),
            2u to PebbleDictionaryItem.UInt32(5u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Nack
   }

   @Test
   fun `Send vibrations after buckets update post initial sync packet `() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)
      runCurrent()

      sender.sentPackets.clear()

      notificationsRepository.nextVibration = intArrayOf(20, 20, 20, 20)

      bucketSyncRepository.updateBucket(1u, byteArrayOf(1))
      bucketSyncRepository.updateBucket(2u, byteArrayOf(2))
      delay(1.seconds)

      sender.sentData.shouldNotBeEmpty().last() shouldBe mapOf(
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
   fun `Close app upon receiving close me packet`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)
      runCurrent()

      sender.sentPackets.clear()

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(8u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack
      watchappOpenController.watchappClosedToTheLastApp.shouldBe(WatchIdentifier("watch"))
   }

   private suspend fun receiveStandardHelloPacket(version: UInt = 0u, bufferSize: UInt = 1000u): ReceiveResult =
      connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(0u),
            1u to PebbleDictionaryItem.UInt32(PROTOCOL_VERSION.toUInt()),
            2u to PebbleDictionaryItem.UInt32(version),
            3u to PebbleDictionaryItem.UInt32(bufferSize),
         )
      )
}
