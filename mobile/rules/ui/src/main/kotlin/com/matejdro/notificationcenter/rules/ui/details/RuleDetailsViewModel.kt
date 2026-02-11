package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.runtime.Stable
import androidx.datastore.preferences.core.Preferences
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.RulesRepository
import com.matejdro.notificationcenter.rules.ui.errors.RuleMissingException
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
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
            rulesRepository.getSingle(key.id).flatMapLatest { ruleOutcome ->
               when (ruleOutcome) {
                  is Outcome.Error -> flowOf(Outcome.Error(ruleOutcome.exception))
                  is Outcome.Progress -> flowOf(Outcome.Progress())
                  is Outcome.Success -> {
                     val rule = ruleOutcome.data ?: throw RuleMissingException()

                     rulesRepository.getRulePreferences(key.id).map { preferences ->
                        Outcome.Success(RuleDetailsScreenState(rule, preferences))
                     }
                  }
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

   fun <T> updatePreference(key: Preferences.Key<T>, value: T) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleDetailsViewModel.updatePreference($key, ${value ?: "null"})" }

      rulesRepository.updateRulePreference(this@RuleDetailsViewModel.key.id) {
         it[key] = value
      }
   }
}

@Stable
data class RuleDetailsScreenState(val ruleMetadata: RuleMetadata, val preferences: Preferences)
