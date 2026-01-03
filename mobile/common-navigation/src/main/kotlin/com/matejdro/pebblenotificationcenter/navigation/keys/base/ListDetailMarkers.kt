package com.matejdro.pebblenotificationcenter.navigation.keys.base

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen key that can be shown as the list part of the list-detail pattern.
 */
interface ListKey {
   val minListWidth: Dp
      get() = 200.dp

   val minDetailWidth: Dp
      get() = 400.dp
}

/**
 * Screen key that can be shown as the detail part of the list-detail pattern.
 */
interface DetailKey
