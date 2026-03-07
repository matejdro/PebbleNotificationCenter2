package com.matejdro.pebblenotificationcenter.notification

class FakeActionHandler : ActionHandler {
   var returnValue: Boolean = true
   var lastHandledAction: HandledAction? = null

   override suspend fun handleAction(notificationId: Int, actionIndex: Int): Boolean {
      lastHandledAction = HandledAction(notificationId, actionIndex)
      return returnValue
   }

   data class HandledAction(val notificationId: Int, val actionIndex: Int)
}
