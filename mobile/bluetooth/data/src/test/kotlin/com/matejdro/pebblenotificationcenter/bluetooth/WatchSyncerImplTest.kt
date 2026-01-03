package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.FakeBucketSyncRepository

class WatchSyncerImplTest {
   val bucketSyncRepository = FakeBucketSyncRepository(PROTOCOL_VERSION.toInt())

   val watchSyncer = WatchSyncerImpl(
      bucketSyncRepository,
   )
}
