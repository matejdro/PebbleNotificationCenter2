package com.matejdro.pebblenotificationcenter.rules

import androidx.datastore.preferences.core.Preferences
import com.matejdro.pebblenotificationcenter.rules.keys.PreferencePair
import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface RulesRepository {
   fun getAll(): Flow<Outcome<List<RuleMetadata>>>
   fun getSingle(id: Int): Flow<Outcome<RuleMetadata?>>
   suspend fun insert(name: String): Int
   suspend fun copyRule(fromId: Int, nameOfCopy: String): Int
   suspend fun edit(ruleMetadata: RuleMetadata)

   suspend fun delete(id: Int)
   suspend fun reorder(id: Int, toIndex: Int)

   /**
    * Replaces the whole rule list with [rules], in the given display order, keeping the given
    * ids and ensuring the default rule (id [RULE_ID_DEFAULT_SETTINGS]) is present and first.
    *
    * [rulePreferences] replaces the corresponding rules' preferences stores; stores of rules
    * that no longer exist are reset to empty (same as [delete]).
    *
    * @throws IllegalArgumentException if a rule id is not positive or ids are duplicated.
    */
   suspend fun importRules(rules: List<RuleMetadata>, rulePreferences: Map<Int, Preferences> = emptyMap())

   fun getRulePreferences(id: Int): Flow<Preferences>

   suspend fun updateRulePreferences(id: Int, vararg preferencesToSet: PreferencePair<*>)
}

const val RULE_ID_DEFAULT_SETTINGS = 1
