package com.matejdro.notificationcenter.rules

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface RulesRepository {
   fun getAll(): Flow<Outcome<List<RuleMetadata>>>
   fun getSingle(id: Int): Flow<Outcome<RuleMetadata?>>
   suspend fun insert(name: String)
   suspend fun edit(ruleMetadata: RuleMetadata)

   suspend fun delete(id: Int)
   suspend fun reorder(id: Int, toIndex: Int)

   fun getRulePreferences(id: Int): Flow<Preferences>

   suspend fun updateRulePreference(id: Int, transform: suspend (MutablePreferences) -> Unit)
}
