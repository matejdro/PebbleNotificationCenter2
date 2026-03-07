package com.matejdro.notificationcenter.rules.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.notificationcenter.common.test.InMemoryDataStore
import kotlinx.coroutines.CoroutineScope

class FakeDatastoreFactory : DatastoreFactory {
   override fun createDatastore(
      scope: CoroutineScope,
      name: String,
   ): DataStore<Preferences> {
      return InMemoryDataStore(emptyPreferences())
   }
}
