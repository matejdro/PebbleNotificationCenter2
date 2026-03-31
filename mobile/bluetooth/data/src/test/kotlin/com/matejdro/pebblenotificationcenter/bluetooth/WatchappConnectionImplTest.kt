package com.matejdro.pebblenotificationcenter.bluetooth

import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.bucketsync.BucketSyncWatchLoopImpl
import com.matejdro.bucketsync.FakeBucketSyncRepository
import com.matejdro.bucketsync.background.FakeBackgroundSyncNotifier
import com.matejdro.pebble.bluetooth.WatchMetadata
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import com.matejdro.pebblenotificationcenter.common.test.InMemoryDataStore
import com.matejdro.pebblenotificationcenter.notification.FakeActionHandler
import com.matejdro.pebblenotificationcenter.notification.FakeNotificationRepository
import com.matejdro.pebblenotificationcenter.notification.FakeSubmenuActionHandler
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.keys.get
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem.UInt32
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
   private val submenuActionHandler = FakeSubmenuActionHandler()

   private val watch = WatchIdentifier("watch")

   private val packetQueue = PacketQueue(sender, watch, WATCHAPP_UUID)

   private val globalPreferences = InMemoryDataStore(emptyPreferences())

   private val watchMetadata = WatchMetadata()

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
      submenuActionHandler,
      notificationsRepository,
      watch,
      globalPreferences,
      watchMetadata,
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
      notificationDetailsPusher.lastColorWatch shouldBe false
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
   fun `Trigger regular action handler on activate action packet without menu id`() = scope.runTest {
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
      submenuActionHandler.handledActions.shouldBeEmpty()
   }

   @Test
   fun `Trigger regular action handler on activate action packet with menu id 0`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(6u),
            1u to PebbleDictionaryItem.UInt32(10u),
            2u to PebbleDictionaryItem.UInt32(5u),
            3u to PebbleDictionaryItem.UInt32(0u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      actionHandler.lastHandledAction shouldBe FakeActionHandler.HandledAction(10, 5)
      submenuActionHandler.handledActions.shouldBeEmpty()
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

   @Test
   fun `Trigger submenu action handler on activate action packet with menu id not 0`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(6u),
            1u to PebbleDictionaryItem.UInt32(10u),
            2u to PebbleDictionaryItem.UInt32(5u),
            3u to PebbleDictionaryItem.UInt32(1u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      submenuActionHandler.handledActions.shouldContainExactly(
         FakeSubmenuActionHandler.HandledAction(10u, SubmenuType.REPLY_ANSWERS, 5),
      )
      actionHandler.lastHandledAction shouldBe null
   }

   @Test
   fun `Return nack on activate action packet when submenu action handling fails`() = scope.runTest {
      submenuActionHandler.returnValue = false
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(6u),
            1u to PebbleDictionaryItem.UInt32(10u),
            2u to PebbleDictionaryItem.UInt32(5u),
            3u to PebbleDictionaryItem.UInt32(1u),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Nack
   }

   @Test
   fun `Trigger submenu action handler on activate action packet with voice data`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(6u),
            1u to PebbleDictionaryItem.UInt32(10u),
            2u to PebbleDictionaryItem.UInt32(5u),
            3u to PebbleDictionaryItem.UInt32(1u),
            4u to PebbleDictionaryItem.Text("Hello from the watch"),
         )
      )
      runCurrent()

      result shouldBe ReceiveResult.Ack

      submenuActionHandler.handledActions.shouldContainExactly(
         FakeSubmenuActionHandler.HandledAction(
            10u,
            SubmenuType.REPLY_ANSWERS,
            5,
            "Hello from the watch"
         ),
      )
      actionHandler.lastHandledAction shouldBe null
   }

   @Test
   fun `Change mute watch setting`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(10u),
            1u to PebbleDictionaryItem.UInt32(0u),
            2u to PebbleDictionaryItem.UInt32(1u),
         )
      )
      runCurrent()

      globalPreferences.data.first()[GlobalPreferenceKeys.muteWatch] shouldBe true
      result shouldBe ReceiveResult.Ack
   }

   @Test
   fun `Change mute phone setting`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u)

      val result = connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(10u),
            1u to PebbleDictionaryItem.UInt32(1u),
            2u to PebbleDictionaryItem.UInt32(1u),
         )
      )
      runCurrent()

      globalPreferences.data.first()[GlobalPreferenceKeys.mutePhone] shouldBe true
      result shouldBe ReceiveResult.Ack
   }

   @Test
   fun `Push notification details with color watch when color flag is set`() = scope.runTest {
      receiveStandardHelloPacket(bufferSize = 123u, flags = 1u)

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
      notificationDetailsPusher.lastColorWatch shouldBe true
   }

   @Test
   fun `Save width and height into watch metadata`() = scope.runTest {
      connection.onPacketReceived(
         mapOf(
            0u to UInt32(0u),
            1u to UInt32(PROTOCOL_VERSION.toUInt()),
            2u to UInt32(0u),
            3u to UInt32(value = 123u),
            4u to UInt32(0u),
            5u to UInt32(300u),
            6u to UInt32(400u),
         )
      )
      runCurrent()

      watchMetadata.screenWidth shouldBe 300
      watchMetadata.screenHeight shouldBe 400
   }

   private suspend fun receiveStandardHelloPacket(version: UInt = 0u, bufferSize: UInt = 1000u, flags: UInt = 0u): ReceiveResult =
      connection.onPacketReceived(
         mapOf(
            0u to PebbleDictionaryItem.UInt32(0u),
            1u to PebbleDictionaryItem.UInt32(PROTOCOL_VERSION.toUInt()),
            2u to PebbleDictionaryItem.UInt32(version),
            3u to PebbleDictionaryItem.UInt32(bufferSize),
            4u to PebbleDictionaryItem.UInt32(flags),
         )
      )
}
