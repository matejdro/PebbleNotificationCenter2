package com.matejdro.pebblenotificationcenter.wip

import androidx.compose.runtime.Composable
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import si.inova.kotlinova.navigation.screens.InjectNavigationScreen
import si.inova.kotlinova.navigation.screens.Screen

@InjectNavigationScreen
class DummyRuleListScreen : Screen<RuleListScreenKey>() {
   @Composable
   override fun Content(key: RuleListScreenKey) {
   }
}
