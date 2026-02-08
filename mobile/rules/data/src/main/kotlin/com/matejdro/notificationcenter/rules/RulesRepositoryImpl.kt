package com.matejdro.notificationcenter.rules

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.notificationcenter.rules.sqldelight.generated.DbRuleQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dispatch.core.flowOnDefault
import dispatch.core.withDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.Outcome

@Inject
@ContributesBinding(AppScope::class)
class RulesRepositoryImpl(
   private val queries: DbRuleQueries,
) : RulesRepository {
   override fun getAll(): Flow<Outcome<List<RuleMetadata>>> {
      return queries.selectAll().asFlow().map { query ->
         val list = query.awaitAsList()

         if (list.isEmpty()) {
            insert("Default Settings")
         }

         Outcome.Success(list.map { it.toRuleMetadata() })
      }.flowOnDefault()
   }

   override suspend fun insert(name: String) = withDefault<Unit> {
      queries.insert(name)
   }

   override suspend fun edit(ruleMetadata: RuleMetadata) = withDefault<Unit> {
      queries.update(ruleMetadata.name, ruleMetadata.id.toLong())
   }

   override suspend fun delete(id: Int) {
      require(id > 1) { "Default rule cannot be deleted" }
      queries.delete(id.toLong())
   }

   override suspend fun reorder(id: Int, toIndex: Int) = withDefault<Unit> {
      require(id > 1 && toIndex > 0) { "Default rule cannot be reordered" }
      val rule = queries.selectSingle(id.toLong()).executeAsOne()

      if (rule.sortOrder > toIndex) {
         queries.reorderUpwards(fromIndex = rule.sortOrder, toIndex = toIndex.toLong(), id = id.toLong())
      } else {
         queries.reorderDownwards(toIndex = toIndex.toLong(), fromIndex = rule.sortOrder, id = id.toLong())
      }
   }
}
