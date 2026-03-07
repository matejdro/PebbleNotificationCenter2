package com.matejdro.pebblenotificationcenter.navigation.instructions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import si.inova.kotlinova.navigation.di.NavigationContext
import si.inova.kotlinova.navigation.instructions.NavigationInstruction
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

/**
 * If the screen already exists on the backstack, it moves it to the top. Otherwise, it adds it to the backstack.
 */
@Serializable
data class OpenScreenOrReplaceExistingType(val screen: @Contextual ScreenKey) : NavigationInstruction() {
   override fun performNavigation(backstack: List<ScreenKey>, context: NavigationContext): NavigationResult {
      return if (backstack.isNotEmpty() && backstack.last().javaClass == screen.javaClass) {
         val newBackstack = backstack.dropLast(1) + screen
         NavigationResult(newBackstack)
      } else {
         val backstackWithoutTargetScreen = backstack.filter { it != screen }
         NavigationResult(backstackWithoutTargetScreen + screen)
      }
   }
}

/**
 * If the type of the provided screen already exists on top of backstack, it replaces it with the target. Otherwise,
 * it adds the target to the top
 */

fun Navigator.navigateToOrReplaceType(screen: ScreenKey) {
   navigate(OpenScreenOrReplaceExistingType(screen))
}
