package com.matejdro.pebblenotificationcenter.tasker

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.matejdro.notificationcenter.tasker.R
import com.matejdro.pebblenotificationcenter.common.NotificationsKeys
import logcat.logcat

class LegacyTaskerReceiver : BroadcastReceiver() {
   // ForegroundServiceStartNotAllowedException is API 31+ so direct catch is unsafe on older APIs
   @Suppress("InstanceOfCheckForException")
   override fun onReceive(context: Context, intent: Intent) {
      if (isOrderedBroadcast()) {
         setResultCode(TaskerPluginConstants.RESULT_CODE_PENDING)
      }

      val serviceIntent: Intent = Intent(context, TaskerActionService::class.java)
      intent.extras?.let { extras ->
         extras.putBoolean(TaskerPluginConstants.EXTRA_CAN_BIND_FIRE_SETTING, false)

         serviceIntent.putExtras(extras)
      }
      logcat { "Received tasker broadcast, starting service" }
      try {
         ContextCompat.startForegroundService(context, serviceIntent)
      } catch (e: Exception) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
            // There is no easy way to solve this, so we just work around by telling user to not use zero-timeout
            // actions, which trigger broadcast receivers instead of services
            // See https://github.com/joaomgcd/TaskerPluginSample/issues/20
            logcat { "Foreground start failed, showing error notification" }
            showErrorNotification(context)
         } else {
            throw e
         }
      }
   }

   private fun showErrorNotification(context: Context) {
      val notification = NotificationCompat.Builder(context, NotificationsKeys.CHANNEL_ID_ERRORS)
         .setContentTitle(
            context.getString(
               R.string.notification_title_error,
            )
         )
         .setContentText(context.getString(R.string.error_tasker_zero_timeout))
         .setSmallIcon(com.matejdro.pebblenotificationcenter.sharedresources.R.drawable.ic_launcher)
         .build()

      context.getSystemService<NotificationManager>()!!.notify(NotificationsKeys.NOTIFICATION_ID_ERROR, notification)
   }
}
