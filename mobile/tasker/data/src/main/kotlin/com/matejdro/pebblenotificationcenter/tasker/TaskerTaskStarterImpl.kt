package com.matejdro.pebblenotificationcenter.tasker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.matejdro.notificationcenter.tasker.R
import com.matejdro.pebblenotificationcenter.common.NotificationsKeys
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat
import net.dinglisch.android.tasker.TaskerIntent

@Inject
@ContributesBinding(AppScope::class)
class TaskerTaskStarterImpl(private val context: Context) : TaskerTaskStarter {
   override fun startTask(task: String, notification: ParsedNotification): Boolean {
      val status = TaskerIntent.testStatus(context)
      logcat { "Tasker status $status" }

      val success = when (status) {
         TaskerIntent.Status.NotInstalled,
         TaskerIntent.Status.NoReceiver,
         -> {
            showErrorNotification(context.getString(R.string.tasker_not_installed))
            false
         }

         TaskerIntent.Status.NotEnabled -> {
            showErrorNotification(context.getString(R.string.tasker_is_disabled))
            false
         }

         TaskerIntent.Status.AccessBlocked -> {
            showErrorNotification(context.getString(R.string.tasker_access_blocked))
            false
         }

         TaskerIntent.Status.OK, null -> true
      }

      if (!success) {
         return false
      }

      val intent = TaskerIntent(task)

      intent.addLocalVariable("%ncpackage", notification.pkg)
      intent.addLocalVariable("%ncid", notification.id.toString())
      intent.addLocalVariable("%nctag", notification.tag.orEmpty())
      intent.addLocalVariable("%nctitle", notification.title)
      intent.addLocalVariable("%ncsubtitle", notification.subtitle)
      intent.addLocalVariable("%nctext", notification.body)

      // Regular broadcasts are sometimes delayed on Android 14+. Use ordered ones instead.
      // https://stackoverflow.com/questions/77842817/slow-intent-broadcast-delivery-on-android-14
      context.sendOrderedBroadcast(intent, null)

      return true
   }

   private fun showErrorNotification(message: String) {
      val notification = NotificationCompat.Builder(context, NotificationsKeys.CHANNEL_ID_ERRORS)
         .setContentTitle(
            context.getString(
               R.string.notification_title_error,
            )
         )
         .setContentText(message)
         .setSmallIcon(com.matejdro.pebblenotificationcenter.sharedresources.R.drawable.ic_launcher)
         .build()

      context.getSystemService<NotificationManager>()!!.notify(NotificationsKeys.NOTIFICATION_ID_ERROR, notification)
   }
}
