package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncWatchLoop
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.WatchAppConnection
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionGraph
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.requireUint
import com.matejdro.pebble.bluetooth.common.util.writeUShort
import com.matejdro.pebblenotificationcenter.notification.ActionHandler
import com.matejdro.pebblenotificationcenter.notification.NotificationRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem.UInt16
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem.UInt8
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.logcat
import okio.Buffer

@Inject
@ContributesBinding(WatchappConnectionScope::class)
@Suppress("MagicNumber") // Packet processing involves a lot of numbers, it would be less readable to make consts
class WatchappConnectionImpl(
   coroutineScope: CoroutineScope,
   private val watchappOpenController: WatchappOpenController,
   private val packetQueue: PacketQueue,
   private val bucketSyncWatchLoop: BucketSyncWatchLoop,
   private val notificationDetailsPusher: NotificationDetailsPusher,
   private val actionHandler: ActionHandler,
   private val notificationRepository: NotificationRepository,
   private val watch: WatchIdentifier,
) : WatchAppConnection {
   private var watchBufferSize: Int = 0

   init {
      coroutineScope.launch {
         packetQueue.runQueue()
      }
   }

   override suspend fun onPacketReceived(data: PebbleDictionary): ReceiveResult {
      val id = (data.get(0u) as PebbleDictionaryItem.UInt32?)?.value
      logcat { "Received packet ${id ?: "null"}" }

      return when (id) {
         0u -> {
            processWatchWelcomePacket(data)
         }

         4u -> {
            if (watchBufferSize > 0) {
               notificationDetailsPusher.pushNotificationDetails(data.requireUint(1u).toInt(), watchBufferSize)
            }

            ReceiveResult.Ack
         }

         6u -> {
            val success = actionHandler.handleAction(
               data.requireUint(1u).toInt(),
               data.requireUint(2u).toInt()
            )

            logcat { "Action handling success: $success" }

            if (success) ReceiveResult.Ack else ReceiveResult.Nack
         }

         8u -> {
            watchappOpenController.closeWatchappToTheLastApp(watch)
            ReceiveResult.Ack
         }

         else -> {
            logcat { "Unknown packet ID. Nacking..." }
            ReceiveResult.Nack
         }
      }
   }

   private suspend fun processWatchWelcomePacket(data: PebbleDictionary): ReceiveResult {
      val watchProtocolVersion = data.requireUint(1u)
      if (watchProtocolVersion != PROTOCOL_VERSION.toUInt()) {
         logcat { "Mismatch protocol version $watchProtocolVersion" }
         packetQueue.sendPacket(
            mapOf(
               0u to PebbleDictionaryItem.UInt8(1u),
               1u to PebbleDictionaryItem.UInt16(PROTOCOL_VERSION)
            )
         )
         return ReceiveResult.Ack
      }

      val watchVersion = data.requireUint(2u).toUShort()
      watchBufferSize = data.requireUint(3u).toInt()
      logcat { "Watch data: version=$watchVersion, buffer size=$watchBufferSize" }

      bucketSyncWatchLoop.sendFirstPacketAndStartLoop(
         mapOfNotNull(
            0u to UInt8(1u),
            1u to UInt16(PROTOCOL_VERSION),
            (3u to UInt8(1u)).takeIf { watchappOpenController.isNextWatchappOpenForAutoSync() },
         ),
         watchVersion,
         watchBufferSize,
         onBucketsChanged = { pushVibration() }
      )

      return ReceiveResult.Ack
   }

   private suspend fun pushVibration() {
      val vibrationPattern = notificationRepository.pollNextVibration()
      logcat { "Next vibration after packets change: ${vibrationPattern?.contentToString() ?: "null"}" }
      if (vibrationPattern == null) {
         return
      }

      val buffer = Buffer()
      for (entry in vibrationPattern) {
         buffer.writeUShort(entry.toUShort())
      }

      val packet = mapOf(
         0u to PebbleDictionaryItem.UInt8(7u),
         1u to PebbleDictionaryItem.Bytes(buffer.readByteArray())
      )
      packetQueue.sendPacket(packet, priority = PRIORITY_VIBRATION)
   }

   @Inject
   @ContributesBinding(AppScope::class)
   class Factory(
      private val subgraphFactory: WatchappConnectionGraph.Factory,
   ) : WatchAppConnection.Factory {
      override fun create(watch: WatchIdentifier, scope: CoroutineScope): WatchAppConnection {
         return subgraphFactory.create(scope, watch).createWatchappConnection()
      }
   }
}

internal const val PRIORITY_WATCH_TEXT = 1

// This should be sent last, so user has everything visible before watch vibrates
internal const val PRIORITY_VIBRATION = -1

private fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V>?): Map<K, V> =
   pairs.filterNotNull().toMap()
