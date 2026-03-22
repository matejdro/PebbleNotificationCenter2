package com.matejdro.pebblenotificationcenter.bluetooth

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.matejdro.bucketsync.BucketSyncRepository
import com.matejdro.bucketsync.BucketSyncRepository.Companion.MAX_BUCKET_ID
import com.matejdro.pebble.bluetooth.common.util.LimitingStringEncoder
import com.matejdro.pebble.bluetooth.common.util.fixPebbleIndentation
import com.matejdro.pebble.bluetooth.common.util.writeUByte
import com.matejdro.pebble.bluetooth.common.util.writeUInt
import com.matejdro.pebble.bluetooth.common.util.writeUShort
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import com.matejdro.pebblenotificationcenter.notification.model.any
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.RuleOption
import com.matejdro.pebblenotificationcenter.rules.keys.get
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.DefaultCoroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import logcat.logcat
import okio.Buffer
import kotlin.experimental.or
import kotlin.time.Duration.Companion.milliseconds

@Inject
@ContributesBinding(AppScope::class)
class WatchSyncerImpl(
   private val bucketSyncRepository: BucketSyncRepository,
   private val preferenceStore: DataStore<Preferences>,
   private val defaultScope: DefaultCoroutineScope,
) : WatchSyncer {
   private val utf8Encoder = LimitingStringEncoder()

   override suspend fun init() {
      init(enablePreferences = true)
   }

   @VisibleForTesting
   internal suspend fun init(enablePreferences: Boolean) {
      val reloadAllData = !bucketSyncRepository.init(
         BUCKET_DATA_VERSION.toInt(),
         dynamicPool = 2..MAX_BUCKET_ID
      )
      if (reloadAllData) {
         logcat { "Got different protocol version, resetting all data" }
      }

      if (enablePreferences) {
         syncPreferences()
      }
   }

   // Magic numbers are a whole point of this function (protocol constants).
   // Use is not required for memory-only Buffer
   @Suppress("MagicNumber", "MissingUseCall")
   override suspend fun syncNotification(notification: ProcessedNotification, preferences: Preferences): Int {
      val buffer = Buffer()

      val notificationData = notification.systemData
      logcat { "Syncing notification ${notificationData.key} ${notificationData.title}" }

      val epochSecond = notificationData.timestamp.epochSecond
      buffer.writeUInt(epochSecond.toUInt())

      buffer.writeUByte(preferences[RuleOption.titleFont].ordinal.toUByte())
      buffer.writeUByte(preferences[RuleOption.subtitleFont].ordinal.toUByte())
      buffer.writeUByte(preferences[RuleOption.bodyFont].ordinal.toUByte())

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
            notificationData.body.fixPebbleIndentation(),
            leftoverSize,
            true
         ).encodedString
      )

      val flags: UByte = getNotificationFlags(notification)

      val id = bucketSyncRepository.updateBucketDynamic(
         notificationData.key,
         buffer.readByteArray(),
         sortKey = -epochSecond,
         flags = flags
      )

      logcat { "Synced" }

      return id
   }

   private fun getNotificationFlags(notification: ProcessedNotification): UByte {
      var flags: UByte = 0u

      if (notification.unread) {
         flags = flags or 0x01u
      }

      if (notification.paused.any) {
         flags = flags or 0x02u
      }

      return flags
   }

   override suspend fun clearAllNotifications() {
      bucketSyncRepository.clearAllDynamic()
   }

   override suspend fun clearNotification(key: String) {
      bucketSyncRepository.deleteBucketDynamic(key)
      logcat { "Deleting Notification $key from the store" }
   }

   override suspend fun prepareNotificationReadStatus(notification: ProcessedNotification) {
      bucketSyncRepository.updateBucketFlagsSilently(
         id = notification.bucketId.toUByte(),
         flags = getNotificationFlags(notification)
      )
   }

   @Suppress("MissingUseCall") // Buffer does not need to be closed
   private fun syncPreferences() {
      defaultScope.launch {
         preferenceStore.data.debounce(50.milliseconds).collect { preferences ->
            var flags: Byte = 0
            if (preferences[GlobalPreferenceKeys.muteWatch]) {
               flags = flags or 0x01
            }
            if (preferences[GlobalPreferenceKeys.mutePhone]) {
               flags = flags or 0x02
            }

            val autoClose = preferences[GlobalPreferenceKeys.autoCloseSeconds]

            val buffer = Buffer()

            buffer.writeByte(flags.toInt())
            buffer.writeUShort(autoClose.toUShort())

            bucketSyncRepository.updateBucket(
               1u,
               buffer.readByteArray()
            )
         }
      }
   }
}

private const val MAX_TITLE_TEXT_LENGTH = 20
