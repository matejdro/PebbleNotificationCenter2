package com.matejdro.pebblenotificationcenter.rules

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.pebblenotificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.pebblenotificationcenter.rules.keys.PreferencePair
import com.matejdro.pebblenotificationcenter.rules.keys.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import si.inova.kotlinova.core.outcome.Outcome

class FakeRulesRepository : RulesRepository {
   private val rules = MutableStateFlow<List<RuleMetadata>>(emptyList())
   private val preferences = HashMap<Int, MutableStateFlow<Preferences>>()

   override fun getAll(): Flow<Outcome<List<RuleMetadata>>> {
      return rules.map { Outcome.Success(it) }
   }

   override suspend fun insert(name: String): Int {
      rules.update { it + RuleMetadata(it.size + 1, name) }

      return rules.value.size
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
                  mutablePrefs[key as PreferenceKeyWithDefault<Any?>] = value
               }
            }.toPreferences()
         }
   }

   override suspend fun copyRule(fromId: Int, nameOfCopy: String): Int {
      val newRuleId = insert(nameOfCopy)

      preferences.getOrPut(newRuleId) { MutableStateFlow(emptyPreferences()) }
         .value = getRulePreferences(fromId).first()

      return newRuleId
   }

   override suspend fun importRules(
      rules: List<RuleMetadata>,
      rulePreferences: Map<Int, Preferences>,
   ) {
      require(rules.all { it.id > 0 }) { "Rule ids must be positive, but were: ${rules.map { it.id }}" }
      require(rules.distinctBy { it.id }.size == rules.size) { "Rule ids must be unique, but were: ${rules.map { it.id }}" }

      val ordered = if (rules.any { it.id == RULE_ID_DEFAULT_SETTINGS }) {
         rules.filter { it.id == RULE_ID_DEFAULT_SETTINGS } + rules.filter { it.id != RULE_ID_DEFAULT_SETTINGS }
      } else {
         listOf(RuleMetadata(RULE_ID_DEFAULT_SETTINGS, "Default Settings")) + rules
      }

      this.rules.update { ordered }

       val existingKeys = preferences.keys.toSet()

       for ((ruleId, imported) in rulePreferences) {
          preferences.getOrPut(ruleId) { MutableStateFlow(emptyPreferences()) }.value = imported
       }

       val importedIds = ordered.map { it.id }.toSet()
       (existingKeys - importedIds).forEach { id ->
          preferences[id]?.value = emptyPreferences()
       }
   }
}
