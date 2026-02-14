package com.matejdro.pebblenotificationcenter.notification.parsing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.matejdro.pebblenotificationcenter.notification.api.AppNameProvider
import com.matejdro.pebblenotificationcenter.notification.model.NativeAction
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.Inject
import java.time.Instant

@Inject
class NotificationParser(
   private val appNameProvider: AppNameProvider,
) {
   fun parse(
      sbn: StatusBarNotification,
      channel: Any?,
      ranking: NotificationListenerService.Ranking? = null,
   ): ParsedNotification? {
      val notification = sbn.notification
      val title = appNameProvider.getAppName(sbn.packageName)

      val (subtitle, text) = parseSubtitleAndBody(notification)

      if (subtitle.isBlank() && text.isNullOrBlank()) {
         return null
      }

      val (channelId, isSilent) = processChannel(notification, channel)

      val timestampMillis = if (NotificationCompat.getShowWhen(notification)) {
         notification.`when`
      } else {
         sbn.postTime
      }
      return ParsedNotification(
         sbn.key,
         sbn.packageName,
         title,
         subtitle,
         text.orEmpty(),
         Instant.ofEpochMilli(timestampMillis),
         isSilent = isSilent,
         isFilteredByDoNotDisturb = ranking?.matchesInterruptionFilter() == false,
         nativeActions = notification.parseActions(),
         channel = channelId
      )
   }

   private fun parseSubtitleAndBody(
      notification: Notification,
   ): Pair<String, String?> {
      val extras = notification.extras

      val subtitle = (
         extras.getCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_HIDDEN_CONVERSATION_TITLE)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_TITLE_BIG)
         )?.removeUselessCharacaters().orEmpty()

      val text =
         (
            notification.parseMessagingStyle()
               ?: notification.parseInboxStyle()
               ?: extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)
               ?: extras.getCharSequence(NotificationCompat.EXTRA_TEXT)
               ?: extras.getCharSequence(NotificationCompat.EXTRA_SUMMARY_TEXT)
               ?: extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)
               ?: extras.getCharSequence(NotificationCompat.EXTRA_INFO_TEXT)
            )
            ?.removeUselessCharacaters()

      val updatedSubtitle: CharSequence
      val updatedText: CharSequence?
      if (subtitle.length > MAX_TITLE_LENGTH) {
         updatedSubtitle = ""
         updatedText = if (text != null) "$subtitle\n$text" else subtitle
      } else {
         updatedSubtitle = subtitle
         updatedText = text
      }
      return Pair(updatedSubtitle, updatedText)
   }

   private fun processChannel(notification: Notification, channel: Any?): Pair<String?, Boolean> {
      val channelId: String?
      val isSilentChannel: Boolean
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         val channelCast: NotificationChannel? = channel as NotificationChannel?

         channelId = channelCast?.id
         @Suppress("Indentation") // Ktlint false positive
         isSilentChannel =
            channelCast == null ||
            (channelCast.importance < NotificationManager.IMPORTANCE_DEFAULT && !channelCast.shouldVibrate())
      } else {
         channelId = null
         isSilentChannel = true
      }

      @Suppress("DEPRECATION") // We still need to support legacy pre-channel notifications
      val isSilent = isSilentChannel &&
         notification.vibrate == null &&
         notification.sound == null &&
         (notification.defaults and (NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)) == 0

      return channelId to isSilent
   }

   private fun Notification.parseMessagingStyle(): String? {
      val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(this) ?: return null

      val messages = (messagingStyle.messages + messagingStyle.historicMessages).sortedByDescending { it.timestamp }

      var lastName: CharSequence? = null
      return messages.joinToString("\n") {
         val person = it.person
         val text = it.text?.toString().orEmpty()
         if (person != null && lastName != person.name) {
            "${person.name}: $text"
         } else {
            text
         }.also {
            lastName = person?.name
         }
      }
   }

   private fun Notification.parseInboxStyle(): String? {
      val textLines = extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES) ?: return null

      return textLines.joinToString("\n")
   }

   private fun Notification.parseActions(): List<NativeAction> {
      return actions.orEmpty().map { action ->
         NativeAction(action.title.toString(), action.actionIntent)
      }
   }

   private fun CharSequence.removeUselessCharacaters(): String {
      return CONTROL_CHARACTERS.replace(this, "")
   }
}

private const val MAX_TITLE_LENGTH = 20
private val CONTROL_CHARACTERS = Regex("\\p{Cf}")
