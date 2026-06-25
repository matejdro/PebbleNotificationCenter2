package com.matejdro.pebblenotificationcenter.bluetooth.images

interface ImageSender {
   suspend fun showImageOnTheWatch(notificationId: UByte, icon: Any, fill: Boolean)
}
