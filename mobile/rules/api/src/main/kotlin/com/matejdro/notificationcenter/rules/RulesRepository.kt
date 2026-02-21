package com.matejdro.notificationcenter.rules

import androidx.datastore.preferences.core.Preferences
import com.matejdro.notificationcenter.rules.keys.PreferencePair
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

   fun getRulePreferences(id: Int): Flow<Preferences>

   suspend fun updateRulePreferences(id: Int, vararg preferencesToSet: PreferencePair<*>)
}

const val RULE_ID_DEFAULT_SETTINGS = 1
