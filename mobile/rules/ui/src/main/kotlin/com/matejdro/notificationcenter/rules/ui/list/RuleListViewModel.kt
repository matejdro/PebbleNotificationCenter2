package com.matejdro.notificationcenter.rules.ui.list

import androidx.compose.runtime.Stable
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.RulesRepository
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.outcome.mapData
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.SingleScreenViewModel

@Stable
@Inject
@ContributesScopedService
class RuleListViewModel(
   private val resources: CoroutineResourceManager,
   private val actionLogger: ActionLogger,
   private val rulesRepository: RulesRepository,
) : SingleScreenViewModel<RuleListScreenKey>(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<RuleListState>>(Outcome.Progress())
   val uiState: StateFlow<Outcome<RuleListState>> = _uiState

   override fun onServiceRegistered() {
      actionLogger.logAction { "RuleListViewModel.onServiceRegistered()" }

      resources.launchResourceControlTask(_uiState) {
         val rulesFlow = rulesRepository.getAll().map { rulesOutcome ->
            rulesOutcome.mapData { RuleListState(it) }
         }

         emitAll(rulesFlow)
      }
   }

   fun addRule(ruleName: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleListViewModel.addRule($ruleName)" }
      rulesRepository.insert(ruleName)
   }

   fun reorder(id: Int, toIndex: Int) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleListViewModel.reorder($id, $toIndex)" }
      rulesRepository.reorder(id, toIndex)
   }
}

data class RuleListState(
   val rules: List<RuleMetadata>,
)
