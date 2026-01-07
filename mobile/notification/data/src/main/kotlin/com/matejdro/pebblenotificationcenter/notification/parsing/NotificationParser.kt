package com.matejdro.pebblenotificationcenter.notification.parsing

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.Inject
import java.time.Instant

@Inject
class NotificationParser(
   private val appNameProvider: AppNameProvider,
) {
   fun parse(sbn: StatusBarNotification): ParsedNotification? {
      val notification = sbn.notification
      val extras = notification.extras
      val subtitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)
         ?: extras.getCharSequence(NotificationCompat.EXTRA_TITLE_BIG)
         ?: return null

      val title = appNameProvider.getAppName(sbn.packageName)
      val text =
         notification.parseMessagingStyle()
            ?: notification.parseInboxStyle()
            ?: extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_TEXT)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_SUMMARY_TEXT)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_INFO_TEXT)

      return ParsedNotification(
         sbn.key,
         sbn.packageName,
         title,
         subtitle.toString(),
         text?.toString().orEmpty(),
         Instant.ofEpochMilli(sbn.postTime),
      )
   }

   private fun Notification.parseMessagingStyle(): String? {
      val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(this) ?: return null

      val messages = (messagingStyle.messages + messagingStyle.historicMessages).sortedByDescending { it.timestamp }

      return messages.joinToString("\n") {
         val person = it.person
         val text = it.text?.toString().orEmpty()
         if (person != null) {
            "${person.name}: $text"
         } else {
            text
         }
      }
   }

   private fun Notification.parseInboxStyle(): String? {
      val textLines = extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES) ?: return null

      return textLines.joinToString("\n")
   }
}
