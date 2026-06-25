package com.matejdro.pebblenotificationcenter.bluetooth.images

class FakeImageSender : ImageSender {
   var lastSentIcon: Any? = null
   var lastSentNotificationId: UByte? = null
   var lastFilled: Boolean? = null

   override suspend fun showImageOnTheWatch(notificationId: UByte, icon: Any, fill: Boolean) {
      lastSentNotificationId = notificationId
      lastSentIcon = icon
      lastFilled = fill
   }
}
