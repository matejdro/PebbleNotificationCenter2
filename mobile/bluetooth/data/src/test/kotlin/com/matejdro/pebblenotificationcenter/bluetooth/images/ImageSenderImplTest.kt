package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.drawable.Icon
import com.matejdro.pebble.bluetooth.WatchMetadata
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import io.kotest.matchers.collections.shouldContainExactly
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.time.virtualTimeProvider

class ImageSenderImplTest {
   private val scope = TestScope()

   private val drawableExtractor = FakeDrawableExtractor()
   private val pebbleSender = FakePebbleSender(scope.virtualTimeProvider())
   private val packetQueue = PacketQueue(pebbleSender, WatchIdentifier("watch"), WATCHAPP_UUID)

   private val watchMetadata = WatchMetadata(watchBufferSize = 10000)

   private val imageSender = ImageSenderImpl(drawableExtractor, packetQueue, watchMetadata)

   @Test
   fun `Send bitmap to the watch when triggering show image action`() = scope.runTest {
      initWatchSender()

      val icon = Icon.createWithContentUri("content://image")
      drawableExtractor.registerOutput(icon, byteArrayOf(74))

      imageSender.showImageOnTheWatch(icon)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(byteArrayOf(0, 1, 3, 74))
            ),
         )
      )
   }

   @Test
   fun `Split large images`() = scope.runTest {
      initWatchSender()

      val icon = Icon.createWithContentUri("content://image")
      drawableExtractor.registerOutput(icon, byteArrayOf(74))

      watchMetadata.watchBufferSize = 150
      drawableExtractor.registerOutput(icon, ByteArray(300))

      imageSender.showImageOnTheWatch(icon)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(byteArrayOf(0x01, 0x2c, 1) + ByteArray(134))
            ),
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(byteArrayOf(0x01, 0x2c, 0) + ByteArray(134))
            ),
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(byteArrayOf(0x01, 0x2c, 2) + ByteArray(32))
            )
         ),
      )
   }

   private fun TestScope.initWatchSender() {
      backgroundScope.launch {
         packetQueue.runQueue()
      }
   }
}
