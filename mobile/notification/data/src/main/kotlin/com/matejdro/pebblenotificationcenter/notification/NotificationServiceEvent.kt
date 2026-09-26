package com.matejdro.pebblenotificationcenter.notification

import android.service.notification.StatusBarNotification

sealed interface NotificationServiceEvent {
   data class Posted(val sbn: StatusBarNotification) : NotificationServiceEvent
   data class Dismissed(val sbn: StatusBarNotification) : NotificationServiceEvent
   data object AllDismissed : NotificationServiceEvent
   data object ReloadRequest : NotificationServiceEvent
}
