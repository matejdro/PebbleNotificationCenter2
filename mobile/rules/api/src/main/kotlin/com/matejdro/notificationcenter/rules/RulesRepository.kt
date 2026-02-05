package com.matejdro.notificationcenter.rules

import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface RulesRepository {
   fun getAll(): Flow<Outcome<List<RuleMetadata>>>
   suspend fun insert(name: String)
   suspend fun edit(ruleMetadata: RuleMetadata)

   suspend fun delete(id: Int)
}
