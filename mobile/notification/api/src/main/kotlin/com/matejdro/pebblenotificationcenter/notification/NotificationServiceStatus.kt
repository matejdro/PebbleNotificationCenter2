package com.matejdro.pebblenotificationcenter.notification

import androidx.compose.runtime.Stable

@Stable
interface NotificationServiceStatus {
   fun isEnabled(): Boolean

   fun isPermissionGranted(): Boolean

   fun requestNotificationAccess()
}
