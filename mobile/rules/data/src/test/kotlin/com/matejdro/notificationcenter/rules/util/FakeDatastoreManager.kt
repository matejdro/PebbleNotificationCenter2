package com.matejdro.notificationcenter.rules.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeDatastoreManager : DatastoreManager {
   override fun createDatastore(
      scope: CoroutineScope,
      name: String,
   ): DataStore<Preferences> {
      return InMemoryDataStore(emptyPreferences())
   }

   override fun deleteDataStore(name: String) {
   }
}

/**
 * Data store that stores its data in memory. Used for testing.
 */
private class InMemoryDataStore<T>(defaultValue: T) : DataStore<T> {
   override val data = MutableStateFlow(defaultValue)

   override suspend fun updateData(transform: suspend (t: T) -> T): T {
      data.update {
         transform(it)
      }

      return data.value
   }
}
