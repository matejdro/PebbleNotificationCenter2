package com.matejdro.pebblenotificationcenter.notification.model

import java.time.Instant

data class ParsedNotification(
   val key: String,
   val pkg: String,
   val title: String,
   val subtitle: String,
   val body: String,
   val timestamp: Instant,

   /**
    * When true, the notification would not vibrate and/or make sounds from the phone.
    */
   val isSilent: Boolean = true,
   /**
    * When true, the phone has the do not disturb on and this notification would be filtered by it
    */
   val isFilteredByDoNotDisturb: Boolean = false,
)
