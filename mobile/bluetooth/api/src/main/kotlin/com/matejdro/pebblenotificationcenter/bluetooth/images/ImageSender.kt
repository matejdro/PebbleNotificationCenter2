package com.matejdro.pebblenotificationcenter.bluetooth.images

interface ImageSender {
   suspend fun showImageOnTheWatch(
      notificationId: UByte,
      icon: Any?,
      imageIndex: UByte,
      imageCount: UByte,
      zoomLevel: UByte,
      initialPush: Boolean,
   )
}
