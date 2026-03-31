package com.matejdro.pebblenotificationcenter.bluetooth

import android.graphics.drawable.Drawable
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.LimitingStringEncoder
import com.matejdro.pebble.bluetooth.common.util.fixPebbleIndentation
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import com.matejdro.pebble.bluetooth.common.util.writeUShort
import com.matejdro.pebblenotificationcenter.bluetooth.images.DrawableExtractor
import com.matejdro.pebblenotificationcenter.notification.ActionOrderRepository
import com.matejdro.pebblenotificationcenter.notification.NotificationRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.DefaultCoroutineScope
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.util.sizeInBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.logcat
import okio.Buffer
import si.inova.kotlinova.core.exceptions.UnknownCauseException
import si.inova.kotlinova.core.reporting.ErrorReporter

@Inject
@ContributesBinding(WatchappConnectionScope::class)
class NotificationDetailsPusherImpl(
   private val queue: PacketQueue,
   private val notificationRepository: NotificationRepository,
   private val actionOrderRepository: ActionOrderRepository,
   private val drawableExtractor: DrawableExtractor,
   private val scope: DefaultCoroutineScope,
   private val errorReporter: ErrorReporter,
) : NotificationDetailsPusher {
   private val stringEncoder = LimitingStringEncoder()
   private var previousDetailsSendingJob: Job? = null
   private var previousVibrationSendingJob: Job? = null

   // Magic numbers are a whole point of this function (protocol constants).
   // Use is not required for memory-only Buffer
   @Suppress("MagicNumber", "MissingUseCall")
   override fun pushNotificationDetails(bucketId: Int, maxPacketSize: Int, colorWatch: Boolean) {
      previousDetailsSendingJob?.cancel()

      val notification = notificationRepository.getNotification(bucketId)

      previousDetailsSendingJob = scope.launch {
         try {
            notificationRepository.markAsRead(bucketId)

            val buffer = Buffer()
            buffer.writeUByte(bucketId.toUByte())

            val actionsToSend = notification?.actions.orEmpty().take(MAX_ACTIONS_TO_SEND)
            val sortedActions = actionOrderRepository.sort(actionsToSend)

            buffer.writeUByte(sortedActions.size.toUByte())

            for (action in sortedActions) {
               buffer.writeUByte(action.id)
               buffer.write(stringEncoder.encodeSizeLimited(action.title, MAX_ACTIONS_TEXT_BYTES).encodedString)
               buffer.writeUByte(0u)
            }

            val iconData = notification?.systemData?.iconDrawable?.let { icon ->
               drawableExtractor.convertIconDrawableToBitmapBytes(
                  icon as Drawable,
                  ICON_SIZE_PIXELS,
                  ICON_SIZE_PIXELS,
                  colorWatch
               )
            }
            if (iconData != null) {
               buffer.writeUShort(iconData.size.toUShort())
               buffer.write(iconData)
            } else {
               buffer.writeUShort(0u)
            }

            val packetBeforeText = mapOf(
               0u to PebbleDictionaryItem.UInt8(5u),
               1u to PebbleDictionaryItem.Bytes(ByteArray(buffer.size.toInt()))
            )

            val maxTextSize = maxPacketSize - packetBeforeText.sizeInBytes()
            val encodedText = stringEncoder.encodeSizeLimited(
               notification?.systemData?.body.orEmpty().fixPebbleIndentation(),
               maxTextSize
            ).encodedString
            buffer.write(encodedText)

            val packet = packetBeforeText + mapOf(
               1u to PebbleDictionaryItem.Bytes(buffer.readByteArray())
            )

            logcat { "Sending notification details for $bucketId: ${packet.sizeInBytes()} (${sortedActions.size} actions)" }

            launch {
               queue.sendPacket(packet, priority = PRIORITY_WATCH_TEXT)
            }

            pushVibration()
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            errorReporter.report(UnknownCauseException("Failed to push notification details", e))
         }
      }
   }

   // Magic numbers are a whole point of this function (protocol constants).
   // Use is not required for memory-only Buffer
   @Suppress("MagicNumber", "MissingUseCall")
   private fun pushVibration() {
      val vibrationPattern = notificationRepository.pollNextVibration()
      logcat { "Next vibration: ${vibrationPattern?.contentToString() ?: "null"}" }
      if (vibrationPattern == null) {
         return
      }

      previousVibrationSendingJob?.cancel()
      previousVibrationSendingJob = scope.launch {
         val buffer = Buffer()
         for (entry in vibrationPattern) {
            buffer.writeUShort(entry.toUShort())
         }

         val packet = mapOf(
            0u to PebbleDictionaryItem.UInt8(7u),
            1u to PebbleDictionaryItem.Bytes(buffer.readByteArray())
         )
         @Suppress("SuspendFunSwallowedCancellation") // Reset vibration before re-throwing
         try {
            queue.sendPacket(packet, priority = PRIORITY_VIBRATION)
         } catch (e: CancellationException) {
            notificationRepository.resetNextVibration(vibrationPattern)
            throw e
         }
      }
   }
}

private const val MAX_ACTIONS_TO_SEND = 20
private const val MAX_ACTIONS_TEXT_BYTES = 20
private const val ICON_SIZE_PIXELS = 32

interface NotificationDetailsPusher {
   fun pushNotificationDetails(bucketId: Int, maxPacketSize: Int, colorWatch: Boolean)
}
