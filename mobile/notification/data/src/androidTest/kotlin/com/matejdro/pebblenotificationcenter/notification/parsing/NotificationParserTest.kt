package com.matejdro.pebblenotificationcenter.notification.parsing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.Person
import androidx.test.core.app.ApplicationProvider
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.time.Instant

class NotificationParserTest {

   private val context = ApplicationProvider.getApplicationContext<Context>()
   private val notificationParser = NotificationParser({ "SMS App" })

   @Test
   fun parseNotificationWithASimpleText() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseNotificationWithoutTitle() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun returnNullWhenNotificationHasNoParsableProperties() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe null
   }

   @Test
   fun parseBigText() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setStyle(NotificationCompat.BigTextStyle().bigText("Long description"))
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Long description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseMessagingStyle() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setStyle(
            NotificationCompat.MessagingStyle(Person.Builder().setName("Group Chat A").build())
               .setConversationTitle("Group Chat A")
               .addMessage("Message 2", 2L, Person.Builder().setName("Alice").build())
               .addMessage("Message 3", 3L, Person.Builder().setName("Bob").build())
               .addHistoricMessage(
                  NotificationCompat.MessagingStyle.Message("Message 1", 1L, Person.Builder().setName("Bob").build())
               )
         )
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Group Chat A",
         "Bob: Message 3\n" +
            "Alice: Message 2\n" +
            "Bob: Message 1",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun doNotRepeatNameOfTheSamePerson() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setStyle(
            NotificationCompat.MessagingStyle(Person.Builder().setName("Group Chat A").build())
               .setConversationTitle("Group Chat A")
               .addMessage("Message 1", 1L, Person.Builder().setName("Alice").build())
               .addMessage("Message 2", 2L, Person.Builder().setName("Alice").build())
               .addMessage("Message 3", 3L, Person.Builder().setName("Bob").build())
         )
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Group Chat A",
         "Bob: Message 3\n" +
            "Alice: Message 2\n" +
            "Message 1",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseInboxStyle() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setStyle(
            NotificationCompat.InboxStyle()
               .addLine("Message 1")
               .addLine("Message 2")
               .addLine("Message 3")
               .setBigContentTitle("Group Chat A")
         )
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Group Chat A",
         "Message 1\n" +
            "Message 2\n" +
            "Message 3",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseIdTagAndTimestamp() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(
         notification.toSbn(id = 123, tag = "tags", timestamp = 1234L),
         createDefaultSilentChannel()
      ) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|123|tags|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(1234L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseNotificationWithLongSubtitleIntoText() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("A very very long long title title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "",
         "A very very long long title title\nDescription",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun removeUselessControllCharacters() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("\u202CTitle")
         .setContentText("\u200EDescription")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseNotificationWithLegacyDefaultVibration() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         throw AssumptionViolatedException("Legacy defaults can only be tested on pre-O")
      }

      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
         isSilent = false
      )
   }

   @Test
   fun parseNotificationWithLegacyDefaultSound() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         throw AssumptionViolatedException("Legacy defaults can only be tested on pre-O")
      }

      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .setDefaults(NotificationCompat.DEFAULT_SOUND)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
         isSilent = false
      )
   }

   @Test
   fun parseNotificationWithLegacyCustomSound() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         throw AssumptionViolatedException("Legacy defaults can only be tested on pre-O")
      }

      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .setSound(Uri.parse("android.resource://dummy_uri"))
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
         isSilent = false
      )
   }

   @Test
   fun parseNotificationWithLegacyCustomVibration() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         throw AssumptionViolatedException("Legacy defaults can only be tested on pre-O")
      }

      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .setVibrate(longArrayOf(1, 1, 1, 1))
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
         isSilent = false
      )
   }

   @Test
   fun parseNotificationWithChannelVibration() {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
         throw AssumptionViolatedException("Notification Channels are not supported pre-O")
      }

      val channel = NotificationChannel("TEST_CHANNEL_VIB", "Test", NotificationManager.IMPORTANCE_LOW).apply {
         enableVibration(true)
      }

      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL_VIB")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), channel) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         isSilent = false,
         channel = "TEST_CHANNEL_VIB",
      )
   }

   @Test
   fun parseNotificationWithNoisyChannel() {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
         throw AssumptionViolatedException("Notification Channels are not supported pre-O")
      }

      val channel = NotificationChannel("TEST_CHANNEL_NOISE", "Test", NotificationManager.IMPORTANCE_DEFAULT)

      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL_NOISE")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), channel) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         isSilent = false,
         channel = "TEST_CHANNEL_NOISE",
      )
   }

   @Test
   fun handleMissingChannel() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL_MISSING")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseNotificationWithDoNotDisturb() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      val ranking = NotificationListenerService.Ranking()
      rankingInterruptionField.set(ranking, false)

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel(), ranking) shouldBe
         ParsedNotification(
            "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
            TEST_PACKAGE,
            "SMS App",
            "Title",
            "Description",
            Instant.ofEpochMilli(0L),
            channel = testChannelOrNull(),
            isFilteredByDoNotDisturb = true,
         )
   }

   @Test
   fun parseNotificationExcludedFromDoNotDisturb() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      val ranking = NotificationListenerService.Ranking()
      rankingInterruptionField.set(ranking, true)

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel(), ranking) shouldBe
         ParsedNotification(
            "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
            TEST_PACKAGE,
            "SMS App",
            "Title",
            "Description",
            Instant.ofEpochMilli(0L),
            channel = testChannelOrNull(),
            isFilteredByDoNotDisturb = false,
         )
   }

   @Test
   fun prioritiseInnerTimestamp() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .setWhen(4321L)
         .setShowWhen(true)
         .build()

      notificationParser.parse(
         notification.toSbn(id = 123, timestamp = 1234L),
         createDefaultSilentChannel()
      ) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|123|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(4321L),
         channel = testChannelOrNull(),
      )
   }

   @Test
   fun parseActions() {
      val notification = NotificationCompat.Builder(context, "TEST_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .addAction(
            123,
            "Action 1",
            PendingIntentCompat.getActivity(context, 0, Intent(), 0, false)
         )
         .addAction(
            123,
            "Action 2",
            PendingIntentCompat.getActivity(context, 0, Intent(), 0, false)
         )
         .setSmallIcon(0)
         .setShowWhen(false)
         .build()

      notificationParser.parse(notification.toSbn(), createDefaultSilentChannel())
         .shouldNotBeNull()
         .nativeActions
         .map { it.text }
         .shouldContainExactly("Action 1", "Action 2")
   }

   private fun createDefaultSilentChannel(): Any? {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
         return null
      }

      return NotificationChannel("TEST_CHANNEL", "Test", NotificationManager.IMPORTANCE_LOW)
   }

   private fun testChannelOrNull(): String? {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
         return null
      }

      return "TEST_CHANNEL"
   }
}

private fun Notification.toSbn(id: Int = 0, tag: String? = null, timestamp: Long = 0): StatusBarNotification {
   @Suppress("DEPRECATION") // We have to call this for testing reasons
   return StatusBarNotification(
      TEST_PACKAGE,
      TEST_PACKAGE,
      id,
      tag,
      0,
      0,
      0,
      this,
      UserHandle(Parcel.obtain().apply { writeInt(0) }),
      timestamp
   )
}

private const val TEST_PACKAGE = "com.matejdro.pebblenotificationcenter.notification.parsing"

private val rankingInterruptionField = NotificationListenerService.Ranking::class.java
   .getDeclaredField("mMatchesInterruptionFilter")
   .apply { isAccessible = true }
