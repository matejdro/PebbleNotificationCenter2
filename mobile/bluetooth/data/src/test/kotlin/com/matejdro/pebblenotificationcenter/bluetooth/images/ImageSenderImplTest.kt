package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.drawable.Icon
import com.matejdro.pebble.bluetooth.WatchMetadata
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebble.bluetooth.common.test.sentData
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
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
      val png = fakePng(width = 144, height = 144)
      drawableExtractor.registerOutput(icon, png)

      imageSender.showImageOnTheWatch(2u, icon, 0u, 1u, 0u, false)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(expectedHeader(png.size, 3, 144, 144, 0) + png),
            ),
         )
      )
      drawableExtractor.wasZoomLevel shouldBe 0
   }

   @Test
   fun `Split large images`() = scope.runTest {
      initWatchSender()

      val icon = Icon.createWithContentUri("content://image")
      val png = fakePng(width = 144, height = 144, payloadSize = 267)
      drawableExtractor.registerOutput(icon, png)

      watchMetadata.watchBufferSize = 150

      imageSender.showImageOnTheWatch(2u, icon, 0u, 1u, 0u, false)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(expectedHeader(png.size, 1, 144, 144, 0) + png.sliceArray(0 until 134)),
            ),
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(expectedHeader(png.size, 0, 144, 144, 0) + png.sliceArray(134 until 268)),
            ),
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(expectedHeader(png.size, 2, 144, 144, 0) + png.sliceArray(268 until 300)),
            ),
         ),
      )
   }

   @Test
   fun `Send zoomed image dimensions in the header for zoom level 4`() = scope.runTest {
      initWatchSender()

      val icon = Icon.createWithContentUri("content://image")
      val png = fakePng(width = 288, height = 288)
      drawableExtractor.registerOutput(icon, png)

      imageSender.showImageOnTheWatch(2u, icon, 0u, 1u, 4u, false)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(expectedHeader(png.size, 3, 288, 288, 4) + png),
            ),
         )
      )
      drawableExtractor.wasZoomLevel shouldBe 4
   }

   @Test
   fun `Send initial push flag when requested`() = scope.runTest {
      initWatchSender()

      val icon = Icon.createWithContentUri("content://image")
      val png = fakePng(width = 144, height = 144)
      drawableExtractor.registerOutput(icon, png)

      imageSender.showImageOnTheWatch(2u, icon, 0u, 1u, 0u, true)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(expectedHeader(png.size, 0x01 or 0x02 or 0x10, 144, 144, 0) + png),
            ),
         )
      )
   }

   @Test
   fun `Send unavailable signal when the source cannot be loaded`() = scope.runTest {
      initWatchSender()

      val icon = Icon.createWithContentUri("content://missing")

      imageSender.showImageOnTheWatch(2u, icon, 1u, 2u, 2u, false)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(byteArrayOf(2, 0, 0, 0x07, 1, 2, 0, 0, 0, 0, 2)),
            ),
         )
      )
   }

   @Test
   fun `Send too-large signal when the png exceeds the max size`() = scope.runTest {
      initWatchSender()

      val icon = Icon.createWithContentUri("content://image")
      drawableExtractor.registerOutput(icon, ByteArray(256_001))

      imageSender.showImageOnTheWatch(2u, icon, 0u, 1u, 4u, false)

      pebbleSender.sentData.shouldContainExactly(
         listOf(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11u),
               1u to PebbleDictionaryItem.Bytes(byteArrayOf(2, 0, 0, 0x0B, 0, 1, 0, 0, 0, 0, 4)),
            ),
         )
      )
   }

   private fun TestScope.initWatchSender() {
      backgroundScope.launch {
         packetQueue.runQueue()
      }
   }

   private fun expectedHeader(
      totalSize: Int,
      flags: Int,
      width: Int,
      height: Int,
      zoomLevel: Int,
   ): ByteArray = byteArrayOf(
      2,
      (totalSize ushr 8).toByte(),
      totalSize.toByte(),
      flags.toByte(),
      0,
      1,
      (width ushr 8).toByte(),
      width.toByte(),
      (height ushr 8).toByte(),
      height.toByte(),
      zoomLevel.toByte(),
   )

   private fun fakePng(width: Int, height: Int, payloadSize: Int = 1): ByteArray = byteArrayOf(
      0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
      0, 0, 0, 13,
      0x49, 0x48, 0x44, 0x52,
      (width ushr 24).toByte(), (width ushr 16).toByte(), (width ushr 8).toByte(), width.toByte(),
      (height ushr 24).toByte(), (height ushr 16).toByte(), (height ushr 8).toByte(), height.toByte(),
      8, 3, 0, 0, 0,
      0, 0, 0, 0,
   ) + ByteArray(payloadSize) { 74 }
}
