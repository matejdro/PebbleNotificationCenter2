package com.matejdro.pebblenotificationcenter.notification

interface ActionHandler {
   suspend fun handleAction(notificationId: Int, actionIndex: Int): Boolean

   fun lastShownImageIndex(notificationId: Int): Int = 0
}
