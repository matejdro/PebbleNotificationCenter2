package com.matejdro.notificationcenter.rules.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope

fun interface DatastoreFactory {
   fun createDatastore(scope: CoroutineScope, name: String): DataStore<Preferences>
}
