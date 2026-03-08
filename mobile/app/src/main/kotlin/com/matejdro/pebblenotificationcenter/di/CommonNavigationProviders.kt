package com.matejdro.pebblenotificationcenter.di

import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.flow.StateFlow
import si.inova.kotlinova.navigation.backstack.Backstack
import si.inova.kotlinova.navigation.di.BackstackScope
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@ContributesTo(BackstackScope::class)
interface CommonNavigationProviders {
   @Provides
   fun provideFlowOfCurrentBackstack(backstack: Backstack): StateFlow<List<ScreenKey>> = backstack.backstack
}
