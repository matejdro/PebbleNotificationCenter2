package com.matejdro.pebblenotificationcenter.history

import app.cash.sqldelight.coroutines.asFlow
import com.matejdro.pebblenotificationcenter.history.sqldelight.generated.DbHistoryQueries
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
class HistoryRepositoryImpl(
   private val db: DbHistoryQueries,
) : HistoryRepository {
   override fun getHistory(): Flow<Outcome<List<HistoryEntry>>> {
      return db.selectAll().asFlow().map { query ->
         Outcome.Success(query.executeAsList().map { item -> item.toHistoryEntry() })
      }.flowOnDefault()
   }

   override suspend fun addHistoryEntry(entry: HistoryEntry) = withDefault<Unit> {
      db.insert(entry.toDb())
      db.deleteOldItems()
   }
}
