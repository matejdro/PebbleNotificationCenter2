package com.matejdro.notificationcenter.rules

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.notificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.PreferencePair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import si.inova.kotlinova.core.outcome.Outcome

class FakeRulesRepository : RulesRepository {
   private val rules = MutableStateFlow<List<RuleMetadata>>(emptyList())
   private val preferences = HashMap<Int, MutableStateFlow<Preferences>>()

   override fun getAll(): Flow<Outcome<List<RuleMetadata>>> {
      return rules.map { Outcome.Success(it) }
   }

   override suspend fun insert(name: String) {
      rules.update { it + RuleMetadata(it.size + 1, name) }
   }

   override suspend fun edit(ruleMetadata: RuleMetadata) {
      rules.update { ruleList ->
         ruleList.map { ruleToCheck ->
            if (ruleToCheck.id == ruleMetadata.id) {
               ruleMetadata
            } else {
               ruleToCheck
            }
         }
      }
   }

   override suspend fun delete(id: Int) {
      rules.update { ruleList ->
         ruleList.filter { rule -> rule.id != id }
      }

      preferences.remove(id)
   }

   override suspend fun reorder(id: Int, toIndex: Int) {
      rules.update { list ->
         val existing = list.first { it.id == id }
         list.toMutableList().apply {
            remove(existing)
            add(toIndex, existing)
         }
      }
   }

   override fun getSingle(id: Int): Flow<Outcome<RuleMetadata?>> {
      return rules.map { list ->
         Outcome.Success(list.firstOrNull { it.id == id })
      }
   }

   override fun getRulePreferences(id: Int): Flow<Preferences> {
      return preferences.getOrPut(id) { MutableStateFlow(emptyPreferences()) }
   }

   override suspend fun updateRulePreferences(
      id: Int,
      vararg preferencesToSet: PreferencePair<*>,
   ) {
      preferences.getOrPut(id) { MutableStateFlow(emptyPreferences()) }
         .update { preference ->
            preference.toMutablePreferences().also { mutablePrefs ->
               for ((key, value) in preferencesToSet) {
                  @Suppress("UNCHECKED_CAST")
                  mutablePrefs[key as PreferenceKeyWithDefault<Any>] = value
               }
            }.toPreferences()
         }
   }
}
