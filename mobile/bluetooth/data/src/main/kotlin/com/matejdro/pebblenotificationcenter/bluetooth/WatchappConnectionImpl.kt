package com.matejdro.pebblenotificationcenter.bluetooth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.matejdro.bucketsync.BucketSyncWatchLoop
import com.matejdro.notificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.notificationcenter.rules.keys.set
import com.matejdro.pebble.bluetooth.common.PacketQueue
import com.matejdro.pebble.bluetooth.common.WatchAppConnection
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionGraph
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import com.matejdro.pebble.bluetooth.common.util.requireUint
import com.matejdro.pebble.bluetooth.common.util.writeUShort
import com.matejdro.pebblenotificationcenter.notification.ActionHandler
import com.matejdro.pebblenotificationcenter.notification.NotificationRepository
import com.matejdro.pebblenotificationcenter.notification.SubmenuActionHandler
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
   private val submenuActionHandler: SubmenuActionHandler,
   private val notificationRepository: NotificationRepository,
   private val watch: WatchIdentifier,
   private val preferenceStore: DataStore<Preferences>,
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
               notificationDetailsPusher.pushNotificationDetails(
                  bucketId = data.requireUint(1u).toInt(),
                  maxPacketSize = watchBufferSize
               )
            }

            ReceiveResult.Ack
         }

         6u -> {
            processActionPacket(data)
         }

         8u -> {
            watchappOpenController.closeWatchappToTheLastApp(watch)
            ReceiveResult.Ack
         }

         10u -> {
            processSettingSetPacket(data)
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

   private suspend fun processActionPacket(data: PebbleDictionary): ReceiveResult {
      val menuId = (data[3u] as? PebbleDictionaryItem.UInt32)?.value ?: 0u
      val success = if (menuId == 0u) {
         actionHandler.handleAction(
            notificationId = data.requireUint(1u).toInt(),
            actionIndex = data.requireUint(2u).toInt()
         )
      } else {
         val submenuType = SubmenuType.entries.elementAtOrNull(menuId.toInt() - 1)
         if (submenuType == null) {
            logcat { "Submenu type ${menuId.toInt() - 1} not found" }
            return ReceiveResult.Nack
         }
         submenuActionHandler.handleSubmenuAction(
            data.requireUint(1u).toUByte(),
            submenuType,
            data.requireUint(2u).toInt(),
            (data[4u] as PebbleDictionaryItem.Text?)?.value,
         )
      }
      logcat { "Action handling success: $success" }

      return if (success) ReceiveResult.Ack else ReceiveResult.Nack
   }

   private suspend fun processSettingSetPacket(data: PebbleDictionary): ReceiveResult {
      val preference = when (val settingId = data.requireUint(1u)) {
         0u -> GlobalPreferenceKeys.muteWatch
         1u -> GlobalPreferenceKeys.mutePhone
         else -> {
            logcat { "Unknown setting $settingId" }
            return ReceiveResult.Nack
         }
      }

      val value = data.requireUint(2u) != 0u

      preferenceStore.edit {
         it[preference] = value
      }

      return ReceiveResult.Ack
   }

   // Magic numbers are a whole point of this function (protocol constants).
   // Use is not required for memory-only Buffer
   @Suppress("MagicNumber", "MissingUseCall")
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

internal const val PRIORITY_USER_INTERACTION = 2
internal const val PRIORITY_WATCH_TEXT = 1

// This should be sent last, so user has everything visible before watch vibrates
internal const val PRIORITY_VIBRATION = -1

private fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V>?): Map<K, V> =
   pairs.filterNotNull().toMap()
