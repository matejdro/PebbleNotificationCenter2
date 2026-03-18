package com.matejdro.pebblenotificationcenter.rules.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.pebblenotificationcenter.common.test.InMemoryDataStore
import kotlinx.coroutines.CoroutineScope

class FakeDatastoreFactory : DatastoreFactory {
   override fun createDatastore(
      scope: CoroutineScope,
      name: String,
   ): DataStore<Preferences> {
      return InMemoryDataStore(emptyPreferences())
   }
}
