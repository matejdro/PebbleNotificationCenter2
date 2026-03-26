package com.matejdro.pebblenotificationcenter.notification.parsing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import com.matejdro.pebblenotificationcenter.notification.NotificationConstants
import com.matejdro.pebblenotificationcenter.notification.api.AppNameProvider
import com.matejdro.pebblenotificationcenter.notification.model.NativeAction
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.utils.parseVibrationPattern
import dev.zacsweers.metro.Inject
import java.time.Instant

@Inject
class NotificationParser(
   private val context: Context,
   private val appNameProvider: AppNameProvider,
) {
   fun parse(
      sbn: StatusBarNotification,
      channel: Any?,
      ranking: NotificationListenerService.Ranking? = null,
   ): ParsedNotification? {
      val notification = sbn.notification
      val title = appNameProvider.getAppName(sbn.packageName)

      val (imageUri, messagingStyleText) = notification.parseMessagingStyle()
      val (subtitle, text) = parseSubtitleAndBody(notification, messagingStyleText)

      if (subtitle.isBlank() && text.isNullOrBlank()) {
         return null
      }

      val (channelId, isSilent) = processChannel(notification, channel)

      val timestampMillis = if (NotificationCompat.getShowWhen(notification)) {
         notification.`when`
      } else {
         sbn.postTime
      }

      val largeImage = imageUri?.let { Icon.createWithContentUri(it) }
         ?: BundleCompat.getParcelable<Bitmap>(notification.extras, NotificationCompat.EXTRA_PICTURE, Bitmap::class.java)
            ?.let { Icon.createWithBitmap(it) }
         ?: BundleCompat.getParcelable<Icon>(notification.extras, NotificationCompat.EXTRA_PICTURE_ICON, Icon::class.java)

      return ParsedNotification(
         key = sbn.key,
         pkg = sbn.packageName,
         title = title,
         subtitle = subtitle,
         body = text.orEmpty(),
         timestamp = Instant.ofEpochMilli(notification.parseMessagingStyleTimestamp() ?: timestampMillis),
         isSilent = isSilent,
         isFilteredByDoNotDisturb = ranking?.matchesInterruptionFilter() == false,
         nativeActions = notification.parseActions(),
         channel = channelId,
         isOngoing = sbn.isOngoing,
         groupSummary = NotificationCompat.isGroupSummary(notification),
         localOnly = NotificationCompat.getLocalOnly(notification),
         media = notification.extras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION),
         forceVibrate = sbn.packageName == context.packageName &&
            notification.extras.getBoolean(NotificationConstants.KEY_FORCE_VIBRATE, false),
         overrideVibrationPattern = parseVibrationPattern(notification),
         iconDrawable = notification.smallIcon?.loadDrawable(context),
         largeImage = largeImage
      )
   }

   private fun parseSubtitleAndBody(
      notification: Notification,
      messagingStyleText: String?,
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
            messagingStyleText
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
      return updatedSubtitle to updatedText
   }

   private fun processChannel(notification: Notification, channel: Any?): Pair<String?, Boolean> {
      val channelId: String?
      val isSilentChannel: Boolean
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         val channelCast: NotificationChannel? = channel as NotificationChannel?

         channelId = channelCast?.id
         isSilentChannel = channelCast == null ||
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

   private fun Notification.parseMessagingStyle(): Pair<Uri?, String?> {
      val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(this) ?: return (null to null)

      val messages = (messagingStyle.messages + messagingStyle.historicMessages).sortedByDescending { it.timestamp }

      var lastName: CharSequence? = null
      var firstImage: Uri? = null

      val text = messages.joinToString("\n") { message ->
         if (firstImage == null && message.dataMimeType?.startsWith("image/") == true) {
            firstImage = message.dataUri
         }

         val personName = message.person?.name ?: messagingStyle.user.name
         val text = message.text?.toString().orEmpty()
         if (personName != null && lastName != personName) {
            "$personName: $text"
         } else {
            text
         }.also {
            lastName = personName
         }
      }

      return firstImage to text
   }

   private fun Notification.parseMessagingStyleTimestamp(): Long? {
      val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(this) ?: return null

      val messages = (messagingStyle.messages + messagingStyle.historicMessages)

      return messages.maxOf { it.timestamp }
   }

   private fun Notification.parseInboxStyle(): String? {
      val textLines = extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES) ?: return null

      return textLines.joinToString("\n")
   }

   private fun Notification.parseActions(): List<NativeAction> {
      return actions.orEmpty().mapNotNull { action ->
         val remoteInput = action.remoteInputs?.firstOrNull()
         val actionIntent = action.actionIntent ?: return@mapNotNull null

         NativeAction(
            text = action.title.toString(),
            pendingIntent = actionIntent,
            remoteInputResultKey = remoteInput?.resultKey,
            cannedTexts = remoteInput?.choices?.map { it.toString() }.orEmpty(),
            allowFreeFormInput = remoteInput?.allowFreeFormInput != false
         )
      }
   }

   private fun CharSequence.removeUselessCharacaters(): String {
      return CONTROL_CHARACTERS.replace(this, "")
   }
}

private fun parseVibrationPattern(notification: Notification): List<Short>? {
   notification.extras.getShortArray(NotificationConstants.KEY_VIBRATION_PATTERN)?.toList()?.let { return it }

   val stringPattern = notification.extras.getString(NotificationConstants.KEY_VIBRATION_PATTERN) ?: return null

   return parseVibrationPattern(stringPattern)
}

private const val MAX_TITLE_LENGTH = 20
private val CONTROL_CHARACTERS = Regex("\\p{Cf}")
