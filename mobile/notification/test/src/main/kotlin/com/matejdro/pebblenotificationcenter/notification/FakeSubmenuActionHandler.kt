package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuType

class FakeSubmenuActionHandler : SubmenuActionHandler {
   val handledActions = mutableListOf<HandledAction>()
   var returnValue: Boolean = true

   override fun handleSubmenuAction(
      notificationId: UByte,
      menu: SubmenuType,
      index: Int,
      voiceInputText: String?,
   ): Boolean {
      handledActions += HandledAction(notificationId, menu, index, voiceInputText)
      return returnValue
   }

   data class HandledAction(val notificationId: UByte, val menu: SubmenuType, val index: Int, val voiceInputText: String? = null)
}
