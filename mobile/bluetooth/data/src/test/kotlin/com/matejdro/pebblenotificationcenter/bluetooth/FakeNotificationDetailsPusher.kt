package com.matejdro.pebblenotificationcenter.bluetooth

class FakeNotificationDetailsPusher : NotificationDetailsPusher {
   var lastPushRequestId: Int? = null
   var lastMaxPacketSize: Int? = null
   var lastColorWatch: Boolean? = null

   override fun pushNotificationDetails(bucketId: Int, maxPacketSize: Int, colorWatch: Boolean) {
      lastPushRequestId = bucketId
      lastMaxPacketSize = maxPacketSize
      lastColorWatch = colorWatch
   }
}
