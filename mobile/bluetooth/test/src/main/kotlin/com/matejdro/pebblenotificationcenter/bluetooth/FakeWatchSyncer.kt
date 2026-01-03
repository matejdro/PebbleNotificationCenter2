package com.matejdro.pebblenotificationcenter.bluetooth

class FakeWatchSyncer : WatchSyncer {
   val syncedDirectories = ArrayList<Int>()
   val deletedDirectories = ArrayList<Int>()

   override suspend fun init() {
   }
}
