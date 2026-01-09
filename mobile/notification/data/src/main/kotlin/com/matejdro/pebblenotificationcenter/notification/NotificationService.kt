package com.matejdro.pebblenotificationcenter.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.matejdro.pebblenotificationcenter.common.di.NavigationInjectingApplication
import com.matejdro.pebblenotificationcenter.notification.di.NotificationInject
import com.matejdro.pebblenotificationcenter.notification.parsing.NotificationParser
import dev.zacsweers.metro.Inject
import dispatch.core.DefaultCoroutineScope
import kotlinx.coroutines.launch
import logcat.logcat

class NotificationService : NotificationListenerService() {
   @Inject
   private lateinit var notificationProcessor: NotificationProcessor

   @Inject
   private lateinit var notificationParser: NotificationParser

   @Inject
   private lateinit var coroutineScope: DefaultCoroutineScope

   private var bound = false

   override fun onCreate() {
      logcat { "Starting notification service" }
      (application!! as NavigationInjectingApplication)
         .applicationGraph
         .let { it as NotificationInject }
         .inject(this)
      active = true

      super.onCreate()
   }

   override fun onDestroy() {
      logcat { "Stopping notification service" }
      active = false
      bound = false
      super.onDestroy()
   }

   override fun onListenerConnected() {
      super.onListenerConnected()

      if (bound) {
         // Prevent duplicate calls
         return
      }
      bound = true

      coroutineScope.launch {
         notificationProcessor.onNotificationsCleared()
         for (sbn in activeNotifications) {
            if (!sbn.shouldShow()) {
               logcat { "Skipping notification ${sbn.key} ${sbn.key}" }
               continue
            }

            val parsed = notificationParser.parse(sbn)
            if (parsed != null) {
               notificationProcessor.onNotificationPosted(parsed)
            } else {
               logcat { "Notification ${sbn.key} has no text. Skipping..." }
            }
         }
      }
   }

   override fun onNotificationPosted(sbn: StatusBarNotification) {
      logcat { "Notification ${sbn.key} posted" }
      coroutineScope.launch {
         if (sbn.shouldShow()) {
            val parsed = notificationParser.parse(sbn)
            if (parsed == null) {
               logcat { "Notification ${sbn.key} has no text. Skipping..." }
               return@launch
            }
            notificationProcessor.onNotificationPosted(parsed)
         } else {
            logcat { "Skipping notification ${sbn.key} ${sbn.key}" }
         }
      }
   }

   override fun onNotificationRemoved(sbn: StatusBarNotification) {
      logcat { "Notification ${sbn.key} removed" }

      coroutineScope.launch {
         notificationProcessor.onNotificationDismissed(sbn.key)
      }
   }

   companion object {
      var active: Boolean = false
   }
}

private fun StatusBarNotification.shouldShow(): Boolean {
   // Temporary filter until Rules are ready

   return !isOngoing &&
      !NotificationCompat.isGroupSummary(notification) &&
      !NotificationCompat.getLocalOnly(notification) &&
      !notification.extras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION)
}
