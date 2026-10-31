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
    * Replaces the whole rule list with [rules], in the given display order.
    *
    * Rule ids are used verbatim: they also name the per-rule DataStore files. After the call the
    * rule list equals the given list, with the default rule (id [RULE_ID_DEFAULT_SETTINGS])
    * guaranteed present and first, and with `sortOrder` renumbered `0..N-1` from list position.
    *
    * @throws IllegalArgumentException if any rule id is not positive or if ids are duplicated.
    */
   suspend fun importRules(rules: List<RuleMetadata>)

   fun getRulePreferences(id: Int): Flow<Preferences>

   suspend fun updateRulePreferences(id: Int, vararg preferencesToSet: PreferencePair<*>)
}

const val RULE_ID_DEFAULT_SETTINGS = 1
