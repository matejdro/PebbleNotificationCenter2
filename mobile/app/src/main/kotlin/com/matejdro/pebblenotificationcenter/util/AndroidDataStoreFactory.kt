package com.matejdro.pebblenotificationcenter.util

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.matejdro.pebblenotificationcenter.rules.RuleOption
import com.matejdro.pebblenotificationcenter.rules.keys.set
import com.matejdro.pebblenotificationcenter.rules.util.DatastoreFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@Inject
@ContributesBinding(AppScope::class)
class AndroidDataStoreFactory(
   private val context: Context,
) : DatastoreFactory {
   override fun createDatastore(
      scope: CoroutineScope,
      name: String,
   ): DataStore<Preferences> {
      return PreferenceDataStoreFactory.create(scope = scope, migrations = listOf(ReplyCannedTextsSetToListMigration)) {
         context.preferencesDataStoreFile(name)
      }
   }
}

private object ReplyCannedTextsSetToListMigration : DataMigration<Preferences> {
   override suspend fun shouldMigrate(currentData: Preferences): Boolean {
      return currentData.contains(legacyKey)
   }

   override suspend fun migrate(currentData: Preferences): Preferences {
      return currentData.toMutablePreferences().apply {
         set(RuleOption.replyCannedTexts, currentData[legacyKey].orEmpty().toList())
         remove(legacyKey)
      }
   }

   override suspend fun cleanUp() {}

   private val legacyKey = stringSetPreferencesKey("reply_canned_texts")
}
