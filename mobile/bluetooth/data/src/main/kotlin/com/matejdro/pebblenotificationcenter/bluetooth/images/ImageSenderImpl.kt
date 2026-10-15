package com.matejdro.pebblenotificationcenter.bluetooth.images

import com.matejdro.pebble.bluetooth.WatchMetadata
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebblenotificationcenter.bluetooth.PRIORITY_USER_INTERACTION
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.util.sizeInBytes

@Inject
@ContributesBinding(AppScope::class)
class ImageSenderImpl(
   private val drawableExtractor: DrawableExtractor,
   private val packetQueue: PacketQueue,
   private val watchMetadata: WatchMetadata,
) : ImageSender {
   @Suppress("MagicNumber") // Protocol constants
   override suspend fun showImageOnTheWatch(
      notificationId: UByte,
      icon: Any?,
      imageIndex: UByte,
      imageCount: UByte,
      zoomLevel: UByte,
      initialPush: Boolean,
   ) {
      val pebbleBitmapData = try {
         drawableExtractor.convertIconToBitmapBytes(bitmap = icon, zoomLevel = zoomLevel.toInt())
      } catch (ignored: Exception) {
         null
      }

      if (pebbleBitmapData == null) {
         val flags = 0x01 or 0x02 or 0x04
         sendImageSignal(
            notificationId = notificationId,
            imageIndex = imageIndex,
            imageCount = imageCount,
            zoomLevel = zoomLevel,
            flags = if (initialPush) flags or 0x10 else flags,
         )
         return
      }

      if (pebbleBitmapData.size > MAX_IMAGE_BYTES) {
         val flags = 0x01 or 0x02 or 0x08
         sendImageSignal(
            notificationId = notificationId,
            imageIndex = imageIndex,
            imageCount = imageCount,
            zoomLevel = zoomLevel,
            flags = if (initialPush) flags or 0x10 else flags,
         )
         return
      }

      sendImageChunks(
         notificationId = notificationId,
         imageIndex = imageIndex,
         imageCount = imageCount,
         zoomLevel = zoomLevel,
         initialPush = initialPush,
         pebbleBitmapData = pebbleBitmapData,
      )
   }

   @Suppress("MagicNumber") // Protocol constants
   private suspend fun sendImageSignal(
      notificationId: UByte,
      imageIndex: UByte,
      imageCount: UByte,
      zoomLevel: UByte,
      flags: Int,
   ) {
      val header = byteArrayOf(
         notificationId.toByte(),
         0, 0,
         flags.toByte(),
         imageIndex.toByte(),
         imageCount.toByte(),
         0, 0,
         0, 0,
         zoomLevel.toByte(),
      )
      packetQueue.sendPacket(
         mapOf(
            0u to PebbleDictionaryItem.UInt8(11),
            1u to PebbleDictionaryItem.Bytes(header),
         ),
         priority = PRIORITY_USER_INTERACTION,
      )
   }

   @Suppress("MagicNumber") // Protocol constants
   private suspend fun sendImageChunks(
      notificationId: UByte,
      imageIndex: UByte,
      imageCount: UByte,
      zoomLevel: UByte,
      initialPush: Boolean,
      pebbleBitmapData: ByteArray,
   ) {
      val packetOverhead = mapOf(
         0u to PebbleDictionaryItem.UInt8(11),
         1u to PebbleDictionaryItem.Bytes(byteArrayOf())
      ).sizeInBytes()

      if (watchMetadata.watchBufferSize == 0) {
         return
      }

      val maxPacketSize = watchMetadata.watchBufferSize - packetOverhead
      val width = ((pebbleBitmapData[16].toInt() and 0xFF) shl 24) or ((pebbleBitmapData[17].toInt() and 0xFF) shl 16) or
         ((pebbleBitmapData[18].toInt() and 0xFF) shl 8) or (pebbleBitmapData[19].toInt() and 0xFF)
      val height = ((pebbleBitmapData[20].toInt() and 0xFF) shl 24) or ((pebbleBitmapData[21].toInt() and 0xFF) shl 16) or
         ((pebbleBitmapData[22].toInt() and 0xFF) shl 8) or (pebbleBitmapData[23].toInt() and 0xFF)

      val splits = pebbleBitmapData.toList().chunked(maxPacketSize)

      splits.forEachIndexed { index, split ->
         var flags = 0
         if (index == 0) {
            flags = flags or 0x01
         }
         if (index == splits.lastIndex) {
            flags = flags or 0x02
         }
         if (initialPush) {
            flags = flags or 0x10
         }

         val header = byteArrayOf(
            notificationId.toByte(),
            (pebbleBitmapData.size ushr 8).toByte(),
            pebbleBitmapData.size.toByte(),
            flags.toByte(),
            imageIndex.toByte(),
            imageCount.toByte(),
            (width ushr 8).toByte(),
            width.toByte(),
            (height ushr 8).toByte(),
            height.toByte(),
            zoomLevel.toByte(),
         )
         packetQueue.sendPacket(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(11),
               1u to PebbleDictionaryItem.Bytes(header + split.toByteArray()),
            ),
            priority = PRIORITY_USER_INTERACTION,
         )
      }
   }
}

private const val MAX_IMAGE_BYTES = 256_000
