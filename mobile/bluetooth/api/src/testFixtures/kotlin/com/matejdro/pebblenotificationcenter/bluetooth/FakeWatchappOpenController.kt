package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncWatchappOpenController
import io.rebble.pebblekit2.common.model.WatchIdentifier

class FakeWatchappOpenController : WatchappOpenController, BucketSyncWatchappOpenController {
   private var nextWatchappOpenForAutoSync: Boolean = false
   var watchappOpened: Boolean = false
   var shouldCloseToLastApp: Boolean = true
   var watchappClosedToTheLastApp: WatchIdentifier? = null

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

   override suspend fun closeWatchappToTheLastApp(watch: WatchIdentifier) {
      watchappClosedToTheLastApp = watch
   }

   override fun shouldCloseToLastApp(watch: WatchIdentifier): Boolean {
      return shouldCloseToLastApp
   }
}
