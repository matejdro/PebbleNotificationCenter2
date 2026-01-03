package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncWatchappOpenController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding<WatchappOpenController>())
@ContributesBinding(AppScope::class, binding<BucketSyncWatchappOpenController>())
class WatchappOpenControllerImpl : WatchappOpenController, BucketSyncWatchappOpenController {
   private var nextWatchappOpenForAutoSync: Boolean = false

   override fun isNextWatchappOpenForAutoSync(): Boolean {
      return nextWatchappOpenForAutoSync
   }

   override fun setNextWatchappOpenForAutoSync() {
      nextWatchappOpenForAutoSync = true
   }

   override fun resetNextWatchappOpen() {
      nextWatchappOpenForAutoSync = false
   }
}
