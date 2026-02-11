package com.matejdro.pebblenotificationcenter.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.matejdro.notificationcenter.rules.util.DatastoreManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@Inject
@ContributesBinding(AppScope::class)
class AndroidDataStoreManager(
   private val context: Context,
) : DatastoreManager {
   override fun createDatastore(
      scope: CoroutineScope,
      name: String,
   ): DataStore<Preferences> {
      return PreferenceDataStoreFactory.create(scope = scope) {
         context.preferencesDataStoreFile(name)
      }
   }

   override fun deleteDataStore(name: String) {
      context.preferencesDataStoreFile(name).delete()
   }
}
