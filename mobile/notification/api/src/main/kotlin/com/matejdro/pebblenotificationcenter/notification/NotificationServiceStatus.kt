package com.matejdro.pebblenotificationcenter.notification

interface NotificationServiceStatus {
   fun isEnabled(): Boolean

   fun isPermissionGranted(): Boolean

   fun requestNotificationAccess()
}
