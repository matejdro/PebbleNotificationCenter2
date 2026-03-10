package com.matejdro.pebblenotificationcenter.notification

import androidx.datastore.preferences.core.Preferences
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification

class FakePauseController : PauseController {
   val newNotifications = ArrayList<ParsedNotification>()
   val dismissedNotifications = ArrayList<ParsedNotification>()
   val toggledNotifications = ArrayList<ParsedNotification>()
   val pausedNotifications = ArrayList<ParsedNotification>()

   var becomePausedOnNewCall: Boolean = false

   override fun onNewNotification(
      notification: ParsedNotification,
      preferences: Preferences,
   ) {
      newNotifications += notification
      if (becomePausedOnNewCall) {
         pausedNotifications += notification
      }
   }

   override suspend fun onNotificationDismissed(notification: ParsedNotification) {
      dismissedNotifications += notification
   }

   override fun isNotificationPaused(notification: ParsedNotification): Boolean {
      return pausedNotifications.contains(notification)
   }

   override suspend fun toggleAppPause(notification: ParsedNotification) {
      toggledNotifications += notification
   }
}
