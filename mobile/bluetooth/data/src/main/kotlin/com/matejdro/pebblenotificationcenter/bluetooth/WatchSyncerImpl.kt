package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat

@Inject
@ContributesBinding(AppScope::class)
class WatchSyncerImpl(
   private val bucketSyncRepository: BucketSyncRepository,
) : WatchSyncer {
   override suspend fun init() {
      val reloadAllData = !bucketSyncRepository.init(PROTOCOL_VERSION.toInt())
      if (reloadAllData) {
         logcat { "Got different protocol version, resetting all data" }
      }
   }
}
