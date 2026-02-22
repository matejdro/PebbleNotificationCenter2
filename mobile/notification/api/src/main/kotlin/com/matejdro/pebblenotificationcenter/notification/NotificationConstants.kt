package com.matejdro.pebblenotificationcenter.notification

object NotificationConstants {
   /**
    * When a notification from the NC app has this extra, it always pops up and vibrates, regardless of the user's settings.
    */
   const val KEY_FORCE_VIBRATE: String = "com.matejdro.pebblenotificationcenter.FORCE_VIBRATE"

   /**
    * Notification can contain a short array extra with this key, representing a vibration pattern. When it does,
    * NC will vibrate with the provided pattern instead of a default one
    */
   const val KEY_VIBRATION_PATTERN: String = "com.matejdro.pebblenotificationcenter.VIBRATION_PATTERN"
}
