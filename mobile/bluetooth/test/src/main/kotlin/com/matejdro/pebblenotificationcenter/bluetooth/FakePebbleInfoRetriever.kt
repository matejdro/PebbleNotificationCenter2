package com.matejdro.pebblenotificationcenter.bluetooth

import io.rebble.pebblekit2.client.PebbleInfoRetriever
import io.rebble.pebblekit2.common.model.WatchIdentifier
import io.rebble.pebblekit2.model.ConnectedWatch
import io.rebble.pebblekit2.model.Watchapp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePebbleInfoRetriever : PebbleInfoRetriever {
   private val connectedWatches = MutableStateFlow<List<ConnectedWatch>>(emptyList())
   private val activeApp = MutableStateFlow<Map<WatchIdentifier, Watchapp?>>(emptyMap())

   override fun getConnectedWatches(): Flow<List<ConnectedWatch>> {
      return connectedWatches
   }

   override fun getActiveApp(watch: WatchIdentifier): Flow<Watchapp?> {
      return activeApp.map { it[watch] }
   }

   fun setConnectedWatches(watches: List<ConnectedWatch>) {
      connectedWatches.value = watches
   }

   fun setConnectedWatchIds(watches: List<WatchIdentifier>) {
      connectedWatches.value = watches.map {
         ConnectedWatch(
            id = it,
            name = it.value,
            platform = "",
            revision = "",
            firmwareVersionMajor = 1,
            firmwareVersionMinor = 1,
            firmwareVersionPatch = 1,
            firmwareVersionTag = ""
         )
      }
   }

   fun setActiveApp(watch: WatchIdentifier, watchapp: Watchapp?) {
      activeApp.update { it + (watch to watchapp) }
   }
}
