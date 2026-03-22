package com.matejdro.pebblenotificationcenter.bluetooth

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.bucketsync.BucketSyncRepository
import com.matejdro.bucketsync.FakeBucketSyncRepository
import com.matejdro.bucketsync.api.Bucket
import com.matejdro.bucketsync.api.BucketUpdate
import com.matejdro.pebblenotificationcenter.common.test.InMemoryDataStore
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.PauseStatus
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.PebbleFont
import com.matejdro.pebblenotificationcenter.rules.RuleOption
import com.matejdro.pebblenotificationcenter.rules.keys.set
import dispatch.core.DefaultCoroutineScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class WatchSyncerImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   val bucketSyncRepository = FakeBucketSyncRepository(PROTOCOL_VERSION.toInt())
   val preferences = InMemoryDataStore(emptyPreferences())

   val watchSyncer = WatchSyncerImpl(
      bucketSyncRepository,
      preferences,
      DefaultCoroutineScope(scope.backgroundScope.coroutineContext),
   )

   @Test
   fun `Sync a notification`() = scope.runTest {
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

                  // Fonts
                  5,
                  1,
                  0,

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
   fun `Trim notification texts`() = scope.runTest {
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

                  // Fonts
                  5,
                  1,
                  0,
               ) +

                  // 17 'a' characters, followed by the ... and null
                  ByteArray(17) { 'a'.code.toByte() } +
                  byteArrayOf(46, 46, 46, 0) +
                  // 17 'b' characters, followed by the ... and null
                  ByteArray(17) { 'b'.code.toByte() } +
                  byteArrayOf(46, 46, 46, 0) +
                  // 203 'c' characters, followed by the ...
                  ByteArray(203) { 'c'.code.toByte() } +
                  byteArrayOf(46, 46, 46)
            )
         )
      )
   }

   @Test
   fun `Body should eat all available space`() = scope.runTest {
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

                  // Fonts
                  5,
                  1,
                  0,

                  // UTF8 Bytes for the title, followed by null terminator
                  97,
                  0,

                  // UTF8 Bytes for the subtitle, followed by null terminator
                  98,
                  0,
               ) +
                  // 241 'c' characters, followed by the ...
                  ByteArray(241) { 'c'.code.toByte() } +
                  byteArrayOf(46, 46, 46)
            )
         )
      )
   }

   @Test
   fun `Sort later notifications first`() = scope.runTest {
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
   fun `Delete should delete individual notifications`() = scope.runTest {
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
   fun `Delete all delete all notifications`() = scope.runTest {
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

   @Test
   fun `Set unread flag when the notification is unread`() = scope.runTest {
      init()
      watchSyncer.syncNotification(
         ProcessedNotification(
            ParsedNotification(
               "key",
               "com.app",
               "Title",
               "sTitle",
               "Body",
               // 19:18:25 GMT | Sunday, January 4, 2026
               Instant.ofEpochSecond(1_767_554_305)
            ),
            unread = true
         ),
         emptyPreferences()
      )

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         toVersion = 1u,
         activeBuckets = listOf(2u),
         activeBucketFlags = listOf(1u),
         bucketsToUpdate = listOf(
            Bucket(
               2u,
               byteArrayOf(
                  // Timestamp, in 4 bytes
                  0x69,
                  0x5a,
                  0xbd.toByte(),
                  0x01,

                  // Fonts
                  5,
                  1,
                  0,

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
   fun `Update notification flags`() = scope.runTest {
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

      watchSyncer.prepareNotificationReadStatus(
         ProcessedNotification(
            ParsedNotification(
               "key",
               "com.app",
               "Title",
               "sTitle",
               "Body",
               // 19:18:25 GMT | Sunday, January 4, 2026
               Instant.ofEpochSecond(1_767_554_305)
            ),
            bucketId = 2,
            unread = false
         )
      )

      bucketSyncRepository.awaitNextUpdate(0u).activeBucketFlags.shouldContainExactly(0u)
   }

   @Test
   fun `Do not trigger sync when just preparing flags`() = scope.runTest {
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

      delay(1.seconds)

      watchSyncer.prepareNotificationReadStatus(
         ProcessedNotification(
            ParsedNotification(
               "key",
               "com.app",
               "Title",
               "sTitle",
               "Body",
               // 19:18:25 GMT | Sunday, January 4, 2026
               Instant.ofEpochSecond(1_767_554_305)
            ),
            bucketId = 2,
            unread = false
         )
      )

      bucketSyncRepository.checkForNextUpdate(1u).shouldBeNull()
   }

   @Test
   fun `Fix indentation of the body`() = scope.runTest {
      init()
      watchSyncer.syncNotification(
         ParsedNotification(
            "key",
            "com.app",
            "a",
            "b",
            " c",
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

                  // Fonts
                  5,
                  1,
                  0,

                  // UTF8 Bytes for the title, followed by null terminator
                  97,
                  0,

                  // UTF8 Bytes for the subtitle, followed by null terminator
                  98,
                  0,

                  // UTF8 Bytes for the body
                  194.toByte(), // UTF8 marker
                  160.toByte(), // Non-breaking space, not the input regular space
                  99, // c
               )
            )
         )
      )
   }

   @Test
   fun `Send default preferences`() = scope.runTest {
      init(enablePreferences = true)

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         1u,
         listOf(1u),
         listOf(
            Bucket(
               1u,
               byteArrayOf(
                  0b00000000, // Both mutes by default
                  0,
                  0,
               )
            )
         )
      )
   }

   @Test
   fun `Send updated mute watch preference`() = scope.runTest {
      init(enablePreferences = true)
      delay(2.seconds)

      preferences.edit {
         it[GlobalPreferenceKeys.muteWatch] = true
      }
      delay(2.seconds)

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         2u,
         listOf(1u),
         listOf(
            Bucket(
               1u,
               byteArrayOf(
                  0b00000001, // Only mute watch enabled
                  0,
                  0,
               )
            )
         )
      )
   }

   @Test
   fun `Send updated mute phone preference`() = scope.runTest {
      init(enablePreferences = true)
      delay(2.seconds)

      preferences.edit {
         it[GlobalPreferenceKeys.mutePhone] = true
      }
      delay(2.seconds)

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         2u,
         listOf(1u),
         listOf(
            Bucket(
               1u,
               byteArrayOf(
                  0b00000010, // Only mute phone enabled
                  0,
                  0,
               )
            )
         )
      )
   }

   @Test
   fun `Send updated auto close`() = scope.runTest {
      init(enablePreferences = true)
      delay(2.seconds)

      preferences.edit {
         it[GlobalPreferenceKeys.autoCloseSeconds] = 260
      }
      delay(2.seconds)

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         2u,
         listOf(1u),
         listOf(
            Bucket(
               1u,
               byteArrayOf(
                  0b00000000, // Both mutes disabled by default
                  0x01,
                  0x04,
               )
            )
         )
      )
   }

   @Test
   fun `Set paused flag when the notification is app paused`() = scope.runTest {
      init()
      watchSyncer.syncNotification(
         ProcessedNotification(
            ParsedNotification(
               "key",
               "com.app",
               "Title",
               "sTitle",
               "Body",
               // 19:18:25 GMT | Sunday, January 4, 2026
               Instant.ofEpochSecond(1_767_554_305)
            ),
            paused = PauseStatus(app = true, conversation = false)
         ),
         emptyPreferences()
      )

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         toVersion = 1u,
         activeBuckets = listOf(2u),
         activeBucketFlags = listOf(2u),
         bucketsToUpdate = listOf(
            Bucket(
               2u,
               byteArrayOf(
                  // Timestamp, in 4 bytes
                  0x69,
                  0x5a,
                  0xbd.toByte(),
                  0x01,

                  // Fonts
                  5,
                  1,
                  0,

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
   fun `Set paused flag when the notification is conversation paused`() = scope.runTest {
      init()
      watchSyncer.syncNotification(
         ProcessedNotification(
            ParsedNotification(
               "key",
               "com.app",
               "Title",
               "sTitle",
               "Body",
               // 19:18:25 GMT | Sunday, January 4, 2026
               Instant.ofEpochSecond(1_767_554_305)
            ),
            paused = PauseStatus(app = false, conversation = true)
         ),
         emptyPreferences(),
      )

      bucketSyncRepository.awaitNextUpdate(0u) shouldBe BucketUpdate(
         toVersion = 1u,
         activeBuckets = listOf(2u),
         activeBucketFlags = listOf(2u),
         bucketsToUpdate = listOf(
            Bucket(
               2u,
               byteArrayOf(
                  // Timestamp, in 4 bytes
                  0x69,
                  0x5a,
                  0xbd.toByte(),
                  0x01,

                  // Fonts
                  5,
                  1,
                  0,

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
   fun `Send a different font when different one is set`() = scope.runTest {
      val preferences = emptyPreferences().toMutablePreferences().apply {
         set(RuleOption.titleFont, PebbleFont.GOTHIC_18_BOLD)
         set(RuleOption.subtitleFont, PebbleFont.GOTHIC_24_BOLD)
         set(RuleOption.bodyFont, PebbleFont.GOTHIC_14_BOLD)
      }

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
         ),
         preferences
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

                  // Fonts
                  3,
                  5,
                  1,

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

   private suspend fun init(enablePreferences: Boolean = false) {
      bucketSyncRepository.init(1, 2..BucketSyncRepository.MAX_BUCKET_ID)
      watchSyncer.init(enablePreferences)
   }
}

private suspend fun WatchSyncer.syncNotification(
   parsedNotification: ParsedNotification,
   preferences: Preferences = emptyPreferences(),
): Int {
   return syncNotification(ProcessedNotification(parsedNotification), preferences)
}
