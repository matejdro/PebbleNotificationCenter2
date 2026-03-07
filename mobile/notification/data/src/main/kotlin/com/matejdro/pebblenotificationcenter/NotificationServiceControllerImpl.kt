package com.matejdro.pebblenotificationcenter

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.os.Build
import android.os.Process
import androidx.core.os.bundleOf
import com.matejdro.pebblenotificationcenter.notification.NotificationService
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import com.matejdro.pebblenotificationcenter.notification.model.LightNotificationChannel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat

@Inject
@ContributesBinding(AppScope::class)
class NotificationServiceControllerImpl : NotificationServiceController {
   override fun cancelNotification(key: String): Boolean {
      val service = NotificationService.instance ?: return false

      logcat { "Canceling notification for $key" }

      service.cancelNotification(key)

      return true
   }

   override fun triggerAction(pendingIntent: Any): Boolean {
      if (NotificationService.instance == null) return false

      pendingIntent as PendingIntent

      pendingIntent.send()

      return true
   }

   override fun triggerReplyAction(
      pendingIntent: Any,
      remoteInputKey: String,
      text: String,
   ): Boolean {
      val service = NotificationService.instance ?: return false

      pendingIntent as PendingIntent

      val resultsData = bundleOf(remoteInputKey to text)
      val remoteInputIntent = Intent().apply {
         putExtra(RemoteInput.EXTRA_RESULTS_DATA, resultsData)
      }

      val clipData = ClipData(
         RemoteInput.RESULTS_CLIP_LABEL,
         arrayOf(ClipDescription.MIMETYPE_TEXT_INTENT),
         ClipData.Item(remoteInputIntent)
      )

      pendingIntent.send(
         service,
         0,
         Intent().apply { this.clipData = clipData }
      )

      return true
   }

   override fun getNotificationChannels(pkg: String): List<LightNotificationChannel> {
      val service = NotificationService.instance ?: return emptyList()

      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         service.getNotificationChannels(pkg, Process.myUserHandle()).orEmpty().map {
            LightNotificationChannel(id = it.id, title = it.name.toString())
         }
      } else {
         emptyList()
      }
   }
}
