package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncRepository
import com.matejdro.pebble.bluetooth.common.util.LimitingStringEncoder
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import com.matejdro.pebble.bluetooth.common.util.writeUInt
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat
import okio.Buffer

@Inject
@ContributesBinding(AppScope::class)
class WatchSyncerImpl(
   private val bucketSyncRepository: BucketSyncRepository,
) : WatchSyncer {
   private val utf8Encoder = LimitingStringEncoder()

   override suspend fun init() {
      val reloadAllData = !bucketSyncRepository.init(PROTOCOL_VERSION.toInt())
      if (reloadAllData) {
         logcat { "Got different protocol version, resetting all data" }
      }
   }

   override suspend fun syncNotification(notification: ParsedNotification) {
      val buffer = Buffer()

      logcat { "Syncing notification ${notification.title}" }

      val epochSecond = notification.timestamp.epochSecond
      buffer.writeUInt(epochSecond.toUInt())
      buffer.write(utf8Encoder.encodeSizeLimited(notification.title, MAX_TITLE_TEXT_LENGTH, true))
      buffer.writeUByte(0u)
      buffer.write(utf8Encoder.encodeSizeLimited(notification.subtitle, MAX_TITLE_TEXT_LENGTH, true))
      buffer.writeUByte(0u)
      val leftoverSize = BucketSyncRepository.MAX_BUCKET_SIZE_BYTES - buffer.size.toInt()
      buffer.write(utf8Encoder.encodeSizeLimited(notification.body, leftoverSize, true))

      bucketSyncRepository.updateBucketDynamic(notification.key, buffer.readByteArray(), sortKey = -epochSecond)
   }

   override suspend fun clearAllNotifications() {
      bucketSyncRepository.clearAllDynamic()
   }

   override suspend fun clearNotification(key: String) {
      bucketSyncRepository.deleteBucketDynamic(key)
   }
}

private const val MAX_TITLE_TEXT_LENGTH = 20
