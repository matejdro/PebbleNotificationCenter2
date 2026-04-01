package com.matejdro.pebblenotificationcenter.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import si.inova.kotlinova.core.outcome.Outcome

class FakeHistoryRepository : HistoryRepository {
   private val historyStore = MutableStateFlow<List<HistoryEntry>>(emptyList())
   override fun getHistory(): Flow<Outcome<List<HistoryEntry>>> {
      return historyStore.map { Outcome.Success(it) }
   }

   override suspend fun addHistoryEntry(entry: HistoryEntry) {
      historyStore.update { it + entry }
   }
}
