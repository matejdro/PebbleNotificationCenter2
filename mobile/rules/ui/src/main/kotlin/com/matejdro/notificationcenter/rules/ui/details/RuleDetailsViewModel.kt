package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.runtime.Stable
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.RulesRepository
import com.matejdro.notificationcenter.rules.ui.errors.RuleMissingException
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
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
class RuleDetailsViewModel(
   private val resources: CoroutineResourceManager,
   private val actionLogger: ActionLogger,
   private val rulesRepository: RulesRepository,
) : SingleScreenViewModel<RuleDetailsScreenKey>(resources.scope) {
   private val _uiState = MutableStateFlow<Outcome<RuleDetailsScreenState>>(Outcome.Progress())
   val uiState: StateFlow<Outcome<RuleDetailsScreenState>> = _uiState

   override fun onServiceRegistered() {
      actionLogger.logAction { "RuleDetailsViewModel.onServiceRegistered()" }
      resources.launchResourceControlTask(_uiState) {
         emitAll(
            rulesRepository.getSingle(key.id).map { rule ->
               rule.mapData {
                  RuleDetailsScreenState(it ?: throw RuleMissingException())
               }
            }
         )
      }
   }

   fun deleteRule() = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleDetailsViewModel.deleteRule()" }

      rulesRepository.delete(key.id)
   }

   fun renameRule(newName: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleDetailsViewModel.renameRule()" }

      rulesRepository.edit(RuleMetadata(key.id, newName))
   }
}

@Stable
data class RuleDetailsScreenState(val ruleMetadata: RuleMetadata)
