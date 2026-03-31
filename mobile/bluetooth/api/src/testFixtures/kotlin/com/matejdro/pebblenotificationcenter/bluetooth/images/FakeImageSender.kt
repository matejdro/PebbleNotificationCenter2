package com.matejdro.pebblenotificationcenter.bluetooth.images

class FakeImageSender : ImageSender {
   var lastSentIcon: Any? = null

   override suspend fun showImageOnTheWatch(icon: Any) {
      lastSentIcon = icon
   }
}
