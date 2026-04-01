package com.matejdro.pebblenotificationcenter.history.ui

import androidx.compose.runtime.Stable
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.history.HistoryEntry
import com.matejdro.pebblenotificationcenter.history.HistoryRepository
import com.matejdro.pebblenotificationcenter.navigation.keys.HistoryScreenKey
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.SingleScreenViewModel

@Stable
@Inject
@ContributesScopedService
class HistoryScreenViewModel(
   private val resources: CoroutineResourceManager,
   private val repository: HistoryRepository,
   private val actionLogger: ActionLogger,
) : SingleScreenViewModel<HistoryScreenKey>(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<List<HistoryEntry>>>(Outcome.Progress())
   val uiState: StateFlow<Outcome<List<HistoryEntry>>> = _uiState

   override fun onServiceRegistered() {
      actionLogger.logAction { "HistoryScreenViewModel.onServiceRegistered()" }
      resources.launchResourceControlTask(_uiState) {
         emitAll(repository.getHistory())
      }
   }
}
