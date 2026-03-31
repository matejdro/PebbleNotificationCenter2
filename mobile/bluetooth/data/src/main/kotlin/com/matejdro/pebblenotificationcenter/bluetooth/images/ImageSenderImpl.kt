package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.drawable.Icon
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
   override suspend fun showImageOnTheWatch(icon: Any) {
      icon as Icon

      val pebbleBitmapData = drawableExtractor.convertIconToBitmapBytes(icon)
      if (pebbleBitmapData.size > MAX_IMAGE_BYTES) {
         error("Image too large: ${pebbleBitmapData.size}")
      }

      val packetOverhead = mapOf(
         0u to PebbleDictionaryItem.UInt8(11),
         1u to PebbleDictionaryItem.Bytes(byteArrayOf())
      ).sizeInBytes()

      if (watchMetadata.watchBufferSize == 0) {
         return
      }

      val maxPacketSize = watchMetadata.watchBufferSize - packetOverhead

      val splits = pebbleBitmapData.toList().chunked(maxPacketSize)

      splits.forEachIndexed { index, split ->
         var flags = 0
         if (index == 0) {
            flags = flags or 1
         }
         if (index == splits.lastIndex) {
            flags = flags or 2
         }

         val header = byteArrayOf(
            (pebbleBitmapData.size shr 8).toByte(),
            pebbleBitmapData.size.toByte(),
            flags.toByte(),
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

private const val MAX_IMAGE_BYTES = 16000
