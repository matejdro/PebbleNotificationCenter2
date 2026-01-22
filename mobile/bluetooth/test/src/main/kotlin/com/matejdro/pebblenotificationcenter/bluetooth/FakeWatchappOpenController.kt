package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncWatchappOpenController

class FakeWatchappOpenController : WatchappOpenController, BucketSyncWatchappOpenController {
   private var nextWatchappOpenForAutoSync: Boolean = false
   var watchappOpened: Boolean = false

   override fun isNextWatchappOpenForAutoSync(): Boolean {
      return nextWatchappOpenForAutoSync
   }

   override fun setNextWatchappOpenForAutoSync() {
      nextWatchappOpenForAutoSync = true
   }

   override fun resetNextWatchappOpen() {
      nextWatchappOpenForAutoSync = false
   }

   override suspend fun openWatchapp() {
      watchappOpened = true
   }
}
