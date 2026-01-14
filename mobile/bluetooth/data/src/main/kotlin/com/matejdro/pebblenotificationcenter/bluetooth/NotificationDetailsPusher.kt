package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.LimitingStringEncoder
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import com.matejdro.pebblenotificationcenter.notification.NotificationRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.DefaultCoroutineScope
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.util.sizeInBytes
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.logcat
import okio.Buffer

@Inject
@ContributesBinding(WatchappConnectionScope::class)
class NotificationDetailsPusherImpl(
   private val queue: PacketQueue,
   private val notificationRepository: NotificationRepository,
   private val scope: DefaultCoroutineScope,
) : NotificationDetailsPusher {
   private val stringEncoder = LimitingStringEncoder()
   private var previousDetailsSendingJob: Job? = null

   override fun pushNotificationDetails(bucketId: Int, maxPacketSize: Int) {
      previousDetailsSendingJob?.cancel()

      val notification = notificationRepository.getNotification(bucketId)
      if (notification == null) {
         logcat { "Watch wanted notification details for unknown notification bucket $bucketId" }
         return
      }

      previousDetailsSendingJob = scope.launch {
         val buffer = Buffer()
         buffer.writeUByte(bucketId.toUByte())

         val packetBeforeText = mapOf(
            0u to PebbleDictionaryItem.UInt8(5u),
            1u to PebbleDictionaryItem.Bytes(ByteArray(buffer.size.toInt()) { 0 })
         )

         val maxTextSize = maxPacketSize - packetBeforeText.sizeInBytes()
         val encodedText = stringEncoder.encodeSizeLimited(notification.systemData.body, maxTextSize).encodedString
         buffer.write(encodedText)

         val packet = packetBeforeText + mapOf(
            1u to PebbleDictionaryItem.Bytes(buffer.readByteArray())
         )

         logcat { "Sending notification details for $bucketId: ${packet.sizeInBytes()}" }
         queue.sendPacket(packet, priority = PRIORITY_WATCH_TEXT)
      }
   }
}

interface NotificationDetailsPusher {
   fun pushNotificationDetails(bucketId: Int, maxPacketSize: Int)
}
