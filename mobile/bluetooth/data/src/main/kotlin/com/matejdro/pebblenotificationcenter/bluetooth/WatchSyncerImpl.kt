package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncRepository
import com.matejdro.bucketsync.BucketSyncRepository.Companion.MAX_BUCKET_ID
import com.matejdro.pebble.bluetooth.common.util.LimitingStringEncoder
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import com.matejdro.pebble.bluetooth.common.util.writeUInt
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
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
      val reloadAllData = !bucketSyncRepository.init(
         BUCKET_DATA_VERSION.toInt(),
         dynamicPool = 2..MAX_BUCKET_ID
      )
      if (reloadAllData) {
         logcat { "Got different protocol version, resetting all data" }
      }
   }

   override suspend fun syncNotification(notification: ProcessedNotification): Int {
      val buffer = Buffer()

      val notificationData = notification.systemData
      logcat { "Syncing notification ${notificationData.key} ${notificationData.title}" }

      val epochSecond = notificationData.timestamp.epochSecond
      buffer.writeUInt(epochSecond.toUInt())
      buffer.write(
         utf8Encoder.encodeSizeLimited(
            notificationData.title,
            MAX_TITLE_TEXT_LENGTH,
            true
         ).encodedString
      )
      buffer.writeUByte(0u)
      buffer.write(
         utf8Encoder.encodeSizeLimited(
            notificationData.subtitle,
            MAX_TITLE_TEXT_LENGTH,
            true
         ).encodedString
      )
      buffer.writeUByte(0u)
      val leftoverSize = BucketSyncRepository.MAX_BUCKET_SIZE_BYTES - buffer.size.toInt()
      buffer.write(
         utf8Encoder.encodeSizeLimited(
            notificationData.body,
            leftoverSize,
            true
         ).encodedString
      )

      val id = bucketSyncRepository.updateBucketDynamic(notificationData.key, buffer.readByteArray(), sortKey = -epochSecond)

      logcat { "Synced" }

      return id
   }

   override suspend fun clearAllNotifications() {
      bucketSyncRepository.clearAllDynamic()
   }

   override suspend fun clearNotification(key: String) {
      bucketSyncRepository.deleteBucketDynamic(key)
      logcat { "Deleting Notification $key from the store" }
   }
}

private const val MAX_TITLE_TEXT_LENGTH = 20
