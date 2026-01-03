package com.matejdro.pebblenotificationcenter.common.di

import si.inova.kotlinova.navigation.di.NavigationContext
import si.inova.kotlinova.navigation.di.NavigationInjection

interface NavigationInjectingApplication {
   val applicationGraph: NavigationInjectingGraph
}

interface NavigationInjectingGraph {
   fun getNavigationInjectionFactory(): NavigationInjection.Factory
   fun getNavigationContext(): NavigationContext
}
