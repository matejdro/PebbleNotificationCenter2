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
   val nativeActions: List<NativeAction> = emptyList(),
   val channel: String? = null,
   val isOngoing: Boolean = false,
   val groupSummary: Boolean = false,
   val localOnly: Boolean = false,
   val media: Boolean = false,
   /**
    * When true, notification will always show up and vibrate, regardless of any settings
    */
   val forceVibrate: Boolean = false,
   val overrideVibrationPattern: List<Short>? = null,
)

data class NativeAction(
   val text: String,
   // This is pure kotlin module, so we cannot reference PendingIntent directly.
   // I don't want to change it, so for now we can just use Any and upcast it
   val pendingIntent: Any,
   val remoteInputResultKey: String? = null,
   val cannedTexts: List<String> = emptyList(),
   val allowFreeFormInput: Boolean = true,
)
