package com.matejdro.pebblenotificationcenter.common.preferences

import androidx.datastore.preferences.core.Preferences

operator fun Preferences.plus(other: Preferences): Preferences {
   return toMutablePreferences().apply {
      for ((key, value) in other.asMap()) {
         @Suppress("UNCHECKED_CAST")
         set(key as Preferences.Key<Any>, value)
      }
   }
}
