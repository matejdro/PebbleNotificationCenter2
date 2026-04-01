package com.matejdro.pebblenotificationcenter.history

import kotlinx.coroutines.flow.Flow
import si.inova.kotlinova.core.outcome.Outcome

interface HistoryRepository {
   fun getHistory(): Flow<Outcome<List<HistoryEntry>>>
   suspend fun addHistoryEntry(entry: HistoryEntry)
}
