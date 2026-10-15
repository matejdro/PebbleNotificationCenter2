package com.matejdro.pebblenotificationcenter.bluetooth.images

class FakeImageSender : ImageSender {
   var lastSentIcon: Any? = null
   var lastSentNotificationId: UByte? = null
   var lastSentImageIndex: UByte? = null
   var lastSentImageCount: UByte? = null
   var lastSentZoomLevel: UByte? = null
   var lastSentInitialPush: Boolean? = null

   override suspend fun showImageOnTheWatch(
      notificationId: UByte,
      icon: Any?,
      imageIndex: UByte,
      imageCount: UByte,
      zoomLevel: UByte,
      initialPush: Boolean,
   ) {
      lastSentNotificationId = notificationId
      lastSentIcon = icon
      lastSentImageIndex = imageIndex
      lastSentImageCount = imageCount
      lastSentZoomLevel = zoomLevel
      lastSentInitialPush = initialPush
   }
}