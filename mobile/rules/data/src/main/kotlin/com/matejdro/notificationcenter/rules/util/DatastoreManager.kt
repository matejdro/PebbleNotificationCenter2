package com.matejdro.notificationcenter.rules.util

import androidx.annotation.WorkerThread
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope

interface DatastoreManager {
   fun createDatastore(scope: CoroutineScope, name: String): DataStore<Preferences>

   @WorkerThread
   fun deleteDataStore(name: String)
}
