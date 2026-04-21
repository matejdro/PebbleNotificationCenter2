package com.matejdro.pebblenotificationcenter.tasker

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.keys.set
import dev.zacsweers.metro.Inject
import logcat.logcat
import si.inova.kotlinova.core.state.toMap

@Inject
class TaskerActionRunner(
   private val preferencesStore: DataStore<Preferences>,
) {
   suspend fun run(bundle: Bundle) {
      val actionName = bundle.getString(BundleKeys.ACTION) ?: error("Missing action from bundle")
      val action = enumValueOf<TaskerAction>(actionName)

      logcat { "Got tasker action ${bundle.toMap()}" }

      when (action) {
         TaskerAction.TOGGLE_MUTE -> runToggleMuteAction(bundle)
      }
   }

   private suspend fun runToggleMuteAction(bundle: Bundle) {
      preferencesStore.edit { prefs ->
         prefs.set(GlobalPreferenceKeys.muteWatch, bundle.getBoolean(BundleKeys.MUTE_WATCH))
         prefs.set(GlobalPreferenceKeys.mutePhone, bundle.getBoolean(BundleKeys.MUTE_PHONE))
      }
   }
}
