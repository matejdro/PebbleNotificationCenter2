package com.matejdro.pebblenotificationcenter.bluetooth.di

import com.matejdro.pebble.bluetooth.WatchMetadata
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WatchappConnectionScope::class)
interface WatchappConnectionScopeProviders {
   @Provides
   @SingleIn(WatchappConnectionScope::class)
   fun getWatchMetadata(): WatchMetadata = WatchMetadata()
}
