package com.matejdro.pebblenotificationcenter.bluetooth

class FakeNotificationDetailsPusher : NotificationDetailsPusher {
   var lastPushRequestId: Int? = null
   var lastMaxPacketSize: Int? = null

   override fun pushNotificationDetails(bucketId: Int, maxPacketSize: Int) {
      lastPushRequestId = bucketId
      lastMaxPacketSize = maxPacketSize
   }
}
