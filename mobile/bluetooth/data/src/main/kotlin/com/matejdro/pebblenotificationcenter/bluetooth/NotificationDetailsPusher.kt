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

      previousDetailsSendingJob = scope.launch {
         val buffer = Buffer()
         buffer.writeUByte(bucketId.toUByte())

         val actionsToSend = notification?.actions.orEmpty().take(MAX_ACTIONS_TO_SEND)
         buffer.writeUByte(actionsToSend.size.toUByte())

         for (action in actionsToSend) {
            buffer.write(stringEncoder.encodeSizeLimited(action.title, MAX_ACTIONS_TEXT_BYTES).encodedString)
            buffer.writeUByte(0u)
         }

         val packetBeforeText = mapOf(
            0u to PebbleDictionaryItem.UInt8(5u),
            1u to PebbleDictionaryItem.Bytes(ByteArray(buffer.size.toInt()))
         )

         val maxTextSize = maxPacketSize - packetBeforeText.sizeInBytes()
         val encodedText = stringEncoder.encodeSizeLimited(notification?.systemData?.body.orEmpty(), maxTextSize).encodedString
         buffer.write(encodedText)

         val packet = packetBeforeText + mapOf(
            1u to PebbleDictionaryItem.Bytes(buffer.readByteArray())
         )

         logcat { "Sending notification details for $bucketId: ${packet.sizeInBytes()} (${actionsToSend.size} actions)" }
         queue.sendPacket(packet, priority = PRIORITY_WATCH_TEXT)
      }
   }
}

private const val MAX_ACTIONS_TO_SEND = 20
private const val MAX_ACTIONS_TEXT_BYTES = 20

interface NotificationDetailsPusher {
   fun pushNotificationDetails(bucketId: Int, maxPacketSize: Int)
}
