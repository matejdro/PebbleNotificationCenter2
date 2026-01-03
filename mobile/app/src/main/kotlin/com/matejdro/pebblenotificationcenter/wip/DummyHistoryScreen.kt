package com.matejdro.pebblenotificationcenter.wip

import androidx.compose.runtime.Composable
import com.matejdro.pebblenotificationcenter.navigation.keys.HistoryScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class DummyHistoryScreen : Screen<HistoryScreenKey>() {
   @Composable
   override fun Content(key: HistoryScreenKey) {
   }
}
