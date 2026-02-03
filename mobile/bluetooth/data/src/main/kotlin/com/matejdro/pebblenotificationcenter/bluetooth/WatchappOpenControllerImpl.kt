package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.bucketsync.BucketSyncWatchappOpenController
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import io.rebble.pebblekit2.client.PebbleInfoRetriever
import io.rebble.pebblekit2.client.PebbleSender
import io.rebble.pebblekit2.common.model.WatchIdentifier
import kotlinx.coroutines.flow.first
import logcat.logcat
import java.util.UUID

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding<WatchappOpenController>())
@ContributesBinding(AppScope::class, binding<BucketSyncWatchappOpenController>())
class WatchappOpenControllerImpl(
   private val pebbleSender: PebbleSender,
   private val pebbleInfoRetriever: PebbleInfoRetriever,
) : WatchappOpenController, BucketSyncWatchappOpenController {
   private var nextWatchappOpenForAutoSync: Boolean = false
   private val lastOpenedApps = HashMap<WatchIdentifier, UUID?>()

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
      val connectedWatches = pebbleInfoRetriever.getConnectedWatches().first()
      for (watch in connectedWatches) {
         val watchId = watch.id
         lastOpenedApps[watchId] = pebbleInfoRetriever.getActiveApp(watchId).first()?.id

         pebbleSender.startAppOnTheWatch(WATCHAPP_UUID, listOf(watchId))
      }
   }

   override suspend fun closeWatchappToTheLastApp(watch: WatchIdentifier) {
      val lastApp = lastOpenedApps.remove(watch)
      logcat { "Last open app: ${lastApp ?: "null"}" }
      if (lastApp != null) {
         pebbleSender.startAppOnTheWatch(lastApp, listOf(watch))
      } else {
         pebbleSender.stopAppOnTheWatch(WATCHAPP_UUID, listOf(watch))
      }
   }
}
