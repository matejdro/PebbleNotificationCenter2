package com.matejdro.pebblenotificationcenter.notification

interface ActionHandler {
   suspend fun handleAction(notificationId: Int, actionIndex: Int): Boolean
}
