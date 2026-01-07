package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncRepository
import com.matejdro.bucketsync.FakeBucketSyncRepository
import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class WatchSyncerImplTest {
   val bucketSyncRepository = FakeBucketSyncRepository(PROTOCOL_VERSION.toInt())

   val watchSyncer = WatchSyncerImpl(
      bucketSyncRepository,
   )

   @Test
   fun `Sync a notification`() = runTest {
      init()
      watchSyncer.syncNotification(
         ParsedNotification(
            "key",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )
      )

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         1u,
         listOf(2u),
         listOf(
            Bucket(
               2u,
               byteArrayOf(
                  // Timestamp, in 4 bytes
                  0x69,
                  0x5a,
                  0xbd.toByte(),
                  0x01,

                  // UTF8 Bytes for the title, followed by null terminator
                  84,
                  105,
                  116,
                  108,
                  101,
                  0,

                  // UTF8 Bytes for the subtitle, followed by null terminator
                  115,
                  84,
                  105,
                  116,
                  108,
                  101,
                  0,

                  // UTF8 Bytes for the body, NOT followed by null terminator
                  66,
                  111,
                  100,
                  121,
               )
            )
         )
      )
   }

   @Test
   fun `Trim notification texts`() = runTest {
      init()
      watchSyncer.syncNotification(
         ParsedNotification(
            "key",
            "com.app",
            "a".repeat(100),
            "b".repeat(100),
            "c".repeat(1000),
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )
      )

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         1u,
         listOf(2u),
         listOf(
            Bucket(
               2u,
               byteArrayOf(
                  // Timestamp, in 4 bytes
                  0x69,
                  0x5a,
                  0xbd.toByte(),
                  0x01,
               ) +
                  // 17 'a' characters, followed by the ... and null
                  ByteArray(17) { 'a'.code.toByte() } +
                  byteArrayOf(46, 46, 46, 0) +
                  // 17 'b' characters, followed by the ... and null
                  ByteArray(17) { 'b'.code.toByte() } +
                  byteArrayOf(46, 46, 46, 0) +
                  // 207 'c' characters, followed by the ...
                  ByteArray(206) { 'c'.code.toByte() } +
                  byteArrayOf(46, 46, 46)
            )
         )
      )
   }

   @Test
   fun `Body should eat all available space`() = runTest {
      init()
      watchSyncer.syncNotification(
         ParsedNotification(
            "key",
            "com.app",
            "a",
            "b",
            "c".repeat(1000),
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )
      )

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         1u,
         listOf(2u),
         listOf(
            Bucket(
               2u,
               byteArrayOf(
                  // Timestamp, in 4 bytes
                  0x69,
                  0x5a,
                  0xbd.toByte(),
                  0x01,

                  // UTF8 Bytes for the title, followed by null terminator
                  97,
                  0,

                  // UTF8 Bytes for the subtitle, followed by null terminator
                  98,
                  0,
               ) +
                  // 245 'c' characters, followed by the ...
                  ByteArray(244) { 'c'.code.toByte() } +
                  byteArrayOf(46, 46, 46)
            )
         )
      )
   }

   @Test
   fun `Sort later notifications first`() = runTest {
      init()
      watchSyncer.syncNotification(
         ParsedNotification(
            "1",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )
      )

      watchSyncer.syncNotification(
         ParsedNotification(
            "2",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:26 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_306)
         )
      )

      bucketSyncRepository.awaitNextUpdate(0u).activeBuckets shouldBe listOf<UShort>(3u, 2u)
   }

   @Test
   fun `Delete should delete individual notifications`() = runTest {
      init()
      watchSyncer.syncNotification(
         ParsedNotification(
            "1",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )
      )

      watchSyncer.syncNotification(
         ParsedNotification(
            "2",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:26 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_306)
         )
      )

      delay(1.seconds)

      watchSyncer.clearNotification("1")
      delay(1.seconds)

      bucketSyncRepository.awaitNextUpdate(0u).activeBuckets.shouldContainExactly(3u)
   }

   @Test
   fun `Delete all delete all notifications`() = runTest {
      init()
      watchSyncer.syncNotification(
         ParsedNotification(
            "1",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )
      )

      watchSyncer.syncNotification(
         ParsedNotification(
            "2",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:26 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_306)
         )
      )

      delay(1.seconds)

      watchSyncer.clearAllNotifications()
      delay(1.seconds)

      bucketSyncRepository.awaitNextUpdate(0u).activeBuckets.shouldBeEmpty()
   }

   private suspend fun init() {
      bucketSyncRepository.init(1, 2..BucketSyncRepository.MAX_BUCKET_ID)
   }
}
