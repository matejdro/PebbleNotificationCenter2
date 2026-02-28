package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuType

interface SubmenuActionHandler {
   fun handleSubmenuAction(notificationId: UByte, menu: SubmenuType, index: Int, voiceInputText: String? = null): Boolean
}
