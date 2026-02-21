package com.matejdro.notificationcenter.rules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.notificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.PreferencePair
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.notificationcenter.rules.keys.remove
import com.matejdro.notificationcenter.rules.keys.set
import com.matejdro.notificationcenter.rules.sqldelight.generated.DbRuleQueries
import com.matejdro.notificationcenter.rules.util.DatastoreFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dispatch.core.IOCoroutineScope
import dispatch.core.flowOnDefault
import dispatch.core.withDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import si.inova.kotlinova.core.outcome.Outcome

@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class RulesRepositoryImpl(
   private val ioScope: IOCoroutineScope,
   private val queries: DbRuleQueries,
   private val dataStoreFactory: DatastoreFactory,
) : RulesRepository {
   private val stores = HashMap<Int, DataStore<Preferences>>()

   override fun getAll(): Flow<Outcome<List<RuleMetadata>>> {
      return queries.selectAll().asFlow().map { query ->
         val list = query.awaitAsList()

         if (list.isEmpty()) {
            insert("Default Settings")
         }

         Outcome.Success(list.map { it.toRuleMetadata() })
      }.flowOnDefault()
   }

   override fun getSingle(id: Int): Flow<Outcome<RuleMetadata?>> {
      return queries.selectSingle(id.toLong()).asFlow().map {
         Outcome.Success(it.awaitAsOneOrNull()?.toRuleMetadata())
      }.flowOnDefault()
   }

   override suspend fun insert(name: String) = withDefault<Int> {
      queries.transactionWithResult {
         queries.insert(name)
         queries.lastInsertRowId().executeAsOne().toInt()
      }
   }

   override suspend fun edit(ruleMetadata: RuleMetadata) = withDefault<Unit> {
      queries.update(ruleMetadata.name, ruleMetadata.id.toLong())
   }

   override suspend fun delete(id: Int) = withDefault<Unit> {
      require(id > RULE_ID_DEFAULT_SETTINGS) { "Default rule cannot be deleted" }
      queries.delete(id.toLong())
      getDataStore(id).updateData { emptyPreferences() }
   }

   override suspend fun reorder(id: Int, toIndex: Int) = withDefault<Unit> {
      require(id > RULE_ID_DEFAULT_SETTINGS && toIndex > 0) { "Default rule cannot be reordered" }
      val rule = queries.selectSingle(id.toLong()).executeAsOne()

      if (rule.sortOrder > toIndex) {
         queries.reorderUpwards(fromIndex = rule.sortOrder, toIndex = toIndex.toLong(), id = id.toLong())
      } else {
         queries.reorderDownwards(toIndex = toIndex.toLong(), fromIndex = rule.sortOrder, id = id.toLong())
      }
   }

   override fun getRulePreferences(id: Int): Flow<Preferences> {
      return flow {
         val dataStore = getDataStore(id)
         emitAll(dataStore.data)
      }
   }

   override suspend fun updateRulePreferences(
      id: Int,
      vararg preferencesToSet: PreferencePair<*>,
   ) {
      val defaultRules = if (id != RULE_ID_DEFAULT_SETTINGS) getDataStore(RULE_ID_DEFAULT_SETTINGS).data.first() else null

      getDataStore(id).edit { mutablePrefs ->
         for ((key, value) in preferencesToSet) {
            if (defaultRules != null && value == defaultRules[key]) {
               mutablePrefs.remove(key)
            } else {
               @Suppress("UNCHECKED_CAST")
               mutablePrefs[key as PreferenceKeyWithDefault<Any?>] = value
            }
         }
      }
   }

   private fun getDataStore(id: Int): DataStore<Preferences> {
      stores[id]?.let { return it }

      return synchronized(stores) {
         stores.getOrPut(id) {
            val dataStoreScope = CoroutineScope(ioScope.coroutineContext + SupervisorJob(ioScope.coroutineContext.job))
            val dataStore = dataStoreFactory.createDatastore(dataStoreScope, id.toString())
            dataStore
         }
      }
   }

   override suspend fun copyRule(fromId: Int, nameOfCopy: String): Int {
      val newRuleId = insert(nameOfCopy)

      getDataStore(newRuleId).updateData { getRulePreferences(fromId).first() }

      return newRuleId
   }
}
