package com.matejdro.pebblenotificationcenter.notification

import androidx.datastore.preferences.core.Preferences
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.PauseStatus

class FakePauseController : PauseController {
   val newNotifications = ArrayList<ParsedNotification>()
   val dismissedNotifications = ArrayList<ParsedNotification>()
   val toggledAppNotifications = ArrayList<ParsedNotification>()
   val toggledConversationNotifications = ArrayList<ParsedNotification>()
   val pauseStatuses = HashMap<ParsedNotification, PauseStatus>()

   var becomeAppPausedOnNewCall: Boolean = false
   var becomeConversationPausedOnNewCall: Boolean = false

   override fun onNewNotification(
      notification: ParsedNotification,
      preferences: Preferences,
   ) {
      newNotifications += notification
      pauseStatuses[notification] = PauseStatus(becomeAppPausedOnNewCall, becomeConversationPausedOnNewCall)
   }

   override suspend fun onNotificationDismissed(notification: ParsedNotification) {
      dismissedNotifications += notification
   }

   override fun computePauseStatus(notification: ParsedNotification): PauseStatus {
      return pauseStatuses[notification] ?: PauseStatus()
   }

   override suspend fun toggleAppPause(notification: ParsedNotification) {
      toggledAppNotifications += notification
   }

   override suspend fun toggleConversationPause(notification: ParsedNotification) {
      toggledConversationNotifications += notification
   }
}
