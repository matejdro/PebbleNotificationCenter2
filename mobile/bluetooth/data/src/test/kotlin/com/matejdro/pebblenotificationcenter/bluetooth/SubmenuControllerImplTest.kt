package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebble.bluetooth.common.util.requireBytes
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.time.virtualTimeProvider

class SubmenuControllerImplTest {
   private val scope = TestScope()
   private val pebbleSender = FakePebbleSender(scope.virtualTimeProvider())
   private val packetQueue: PacketQueue = PacketQueue(pebbleSender, WatchIdentifier("Watch"), WATCHAPP_UUID)

   private val submenuController = SubmenuControllerImpl(packetQueue)

   @BeforeEach
   fun setUp() {
      scope.backgroundScope.launch {
         packetQueue.runQueue()
      }
   }

   @Test
   fun `Send menu packet to the watch`() = scope.runTest {
      submenuController.showSubmenuOnTheWatch(
         10u,
         SubmenuType.REPLY_ANSWERS,
         listOf(
            SubmenuItem("A1", 1),
            SubmenuItem("A2", 2),
         )
      )
      runCurrent()

      pebbleSender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(9),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  10, // Notification id
                  1, // Menu ID

                  2, // 2 actions
                  65, 49, 0, // A1 & null
                  65, 50, 0, // A2 & null

               )
            )
         )
      )
   }

   @Test
   fun `Limit action text length`() = scope.runTest {
      submenuController.showSubmenuOnTheWatch(
         9u,
         SubmenuType.OTHER,
         listOf(
            SubmenuItem("a".repeat(100), 1),
            SubmenuItem("A2", 2),
         )
      )
      runCurrent()

      pebbleSender.sentData.shouldContainExactly(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(9),
            1u to PebbleDictionaryItem.Bytes(
               byteArrayOf(
                  9, // Notification id
                  2, // Menu ID

                  2, // 2 actions
               ) + // 17 'a' characters, followed by the ...
                  ByteArray(17) { 'a'.code.toByte() } +

                  byteArrayOf(
                     // ...
                     46, 46, 46,
                     0, // Null terminator

                     65, 50, 0, // A2 & null
                  )
            )
         )
      )
   }

   @Test
   fun `Send max 20 actions`() = scope.runTest {
      submenuController.showSubmenuOnTheWatch(
         10u,
         SubmenuType.REPLY_ANSWERS,
         List(30) {
            SubmenuItem(it.toString(), it)
         }
      )
      runCurrent()

      pebbleSender.sentData.first().requireBytes(1u).elementAt(2) shouldBe 20
   }

   @Test
   fun `Get payloads of sent items`() = scope.runTest {
      submenuController.showSubmenuOnTheWatch(
         10u,
         SubmenuType.REPLY_ANSWERS,
         listOf(
            SubmenuItem("A1", 1234),
            SubmenuItem("A2", 4567),
         )
      )
      submenuController.showSubmenuOnTheWatch(
         11u,
         SubmenuType.OTHER,
         listOf(
            SubmenuItem("B1", 5678),
            SubmenuItem("B2", 9876),
         )
      )
      runCurrent()

      submenuController.getPayloadForMenuItem<Int>(10u, SubmenuType.REPLY_ANSWERS, 1) shouldBe 4567
      submenuController.getPayloadForMenuItem<Int>(11u, SubmenuType.OTHER, 0) shouldBe 5678
      submenuController.getPayloadForMenuItem<Int>(11u, SubmenuType.REPLY_ANSWERS, 4) shouldBe null
   }

   @Test
   fun `Clear the payload for a specific menu when accessed once`() = scope.runTest {
      submenuController.showSubmenuOnTheWatch(
         10u,
         SubmenuType.REPLY_ANSWERS,
         listOf(
            SubmenuItem("A1", 1234),
            SubmenuItem("A2", 4567),
         )
      )
      submenuController.showSubmenuOnTheWatch(
         11u,
         SubmenuType.OTHER,
         listOf(
            SubmenuItem("B1", 5678),
            SubmenuItem("B2", 9876),
         )
      )
      runCurrent()

      submenuController.getPayloadForMenuItem<Int>(10u, SubmenuType.REPLY_ANSWERS, 1) shouldBe 4567
      submenuController.getPayloadForMenuItem<Int>(10u, SubmenuType.REPLY_ANSWERS, 1) shouldBe null
   }
}
