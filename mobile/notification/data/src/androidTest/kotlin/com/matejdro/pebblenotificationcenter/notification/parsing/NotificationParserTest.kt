package com.matejdro.pebblenotificationcenter.notification.parsing

import android.app.Notification
import android.content.Context
import android.os.Parcel
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.test.core.app.ApplicationProvider
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant

class NotificationParserTest {
   private val notificationParser = NotificationParser({ "SMS App" })

   private val context = ApplicationProvider.getApplicationContext<Context>()

   @Test
   fun parseNotificationWithASimpleText() {
      val notification = NotificationCompat.Builder(context, "FAKE_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .build()

      notificationParser.parse(notification.toSbn()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(0L)
      )
   }

   @Test
   fun returnNullWhenNotificationHasNoParsableProperties() {
      val notification = NotificationCompat.Builder(context, "FAKE_CHANNEL")
         .setSmallIcon(0)
         .build()

      notificationParser.parse(notification.toSbn()) shouldBe null
   }

   @Test
   fun parseBigText() {
      val notification = NotificationCompat.Builder(context, "FAKE_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setStyle(NotificationCompat.BigTextStyle().bigText("Long description"))
         .setSmallIcon(0)
         .build()

      notificationParser.parse(notification.toSbn()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Long description",
         Instant.ofEpochMilli(0L)
      )
   }

   @Test
   fun parseMessagingStyle() {
      val notification = NotificationCompat.Builder(context, "FAKE_CHANNEL")
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
         .build()

      notificationParser.parse(notification.toSbn()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Group Chat A: Bob",
         "Bob: Message 3\n" +
            "Alice: Message 2\n" +
            "Bob: Message 1",
         Instant.ofEpochMilli(0L),
      )
   }

   @Test
   fun parseInboxStyle() {
      val notification = NotificationCompat.Builder(context, "FAKE_CHANNEL")
         .setStyle(
            NotificationCompat.InboxStyle()
               .addLine("Message 1")
               .addLine("Message 2")
               .addLine("Message 3")
               .setBigContentTitle("Group Chat A")
         )
         .setSmallIcon(0)
         .build()

      notificationParser.parse(notification.toSbn()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "Group Chat A",
         "Message 1\n" +
            "Message 2\n" +
            "Message 3",
         Instant.ofEpochMilli(0L),
      )
   }

   @Test
   fun parseIdTagAndTimestamp() {
      val notification = NotificationCompat.Builder(context, "FAKE_CHANNEL")
         .setContentTitle("Title")
         .setContentText("Description")
         .setSmallIcon(0)
         .build()

      notificationParser.parse(notification.toSbn(id = 123, tag = "tags", timestamp = 1234L)) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|123|tags|0",
         TEST_PACKAGE,
         "SMS App",
         "Title",
         "Description",
         Instant.ofEpochMilli(1234L)
      )
   }

   @Test
   fun parseNotificationWithLongSubtitleIntoText() {
      val notification = NotificationCompat.Builder(context, "FAKE_CHANNEL")
         .setContentTitle("A very very long long title title")
         .setContentText("Description")
         .setSmallIcon(0)
         .build()

      notificationParser.parse(notification.toSbn()) shouldBe ParsedNotification(
         "0|com.matejdro.pebblenotificationcenter.notification.parsing|0|null|0",
         TEST_PACKAGE,
         "SMS App",
         "",
         "A very very long long title title\nDescription",
         Instant.ofEpochMilli(0L)
      )
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
