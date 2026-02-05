package com.matejdro.notificationcenter.rules

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import si.inova.kotlinova.core.outcome.Outcome

class FakeRulesRepository : RulesRepository {
   private val rules = MutableStateFlow<List<RuleMetadata>>(emptyList())

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
   }
}
