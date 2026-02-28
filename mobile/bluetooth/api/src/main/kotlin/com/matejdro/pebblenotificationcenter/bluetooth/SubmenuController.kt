package com.matejdro.pebblenotificationcenter.bluetooth

interface SubmenuController {
   suspend fun showSubmenuOnTheWatch(notificationId: UByte, type: SubmenuType, items: List<SubmenuItem<*>>)
   fun <T> getPayloadForMenuItem(notificationId: UByte, menu: SubmenuType, index: Int): T?
}

enum class SubmenuType {
   REPLY_ANSWERS,

   /**
    * For tests only
    */
   OTHER,
}

data class SubmenuItem<T>(val text: String, val payload: T)
