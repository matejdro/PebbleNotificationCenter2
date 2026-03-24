package com.matejdro.pebblenotificationcenter.navigation.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import si.inova.kotlinova.compose.result.LocalResultPassingStore
import si.inova.kotlinova.compose.result.ResultKey
import si.inova.kotlinova.navigation.instructions.navigateTo
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.screenkeys.DialogKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Composable
fun <P, T : Any, K> Navigator.rememberNavigationPopup(
   navigationKey: Context.(param: P, resultKey: ResultKey<T>) -> K,
   onResult: (T) -> Unit,
): PopupTrigger<P> where K : ScreenKey, K : DialogKey {
   val store = LocalResultPassingStore.current
   val key = currentCompositeKeyHashCode

   val resultKey = remember {
      store.registerCallback(key, onResult)
   }

   DisposableEffect(resultKey) {
      onDispose {
         store.unregisterCallback(resultKey)
      }
   }
   val context = LocalContext.current

   return PopupTrigger<P> { param ->
      navigateTo(context.navigationKey(param, resultKey))
   }
}

fun interface PopupTrigger<P> {
   fun trigger(param: P)
}

fun PopupTrigger<Unit>.trigger() = trigger(Unit)
