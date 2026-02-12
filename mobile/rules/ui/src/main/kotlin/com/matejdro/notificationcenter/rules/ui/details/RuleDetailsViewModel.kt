package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.runtime.Stable
import androidx.datastore.preferences.core.Preferences
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.RulesRepository
import com.matejdro.notificationcenter.rules.ui.errors.RuleMissingException
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import com.matejdro.pebblenotificationcenter.notification.api.AppNameProvider
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
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
   private val appNameProvider: AppNameProvider,
   private val notificationServiceController: NotificationServiceController,
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
                        val targetAppPackage = preferences[RuleOption.conditionAppPackage]
                        val targetAppName = withDefault { targetAppPackage?.let(appNameProvider::getAppName) }

                        val targetChannelNames = targetAppPackage?.let { pkg ->
                           val channelIds = preferences[RuleOption.conditionNotificationChannels]
                           if (!channelIds.isNullOrEmpty()) {
                              val allChannels = withDefault { notificationServiceController.getNotificationChannels(pkg) }

                              channelIds.map { id -> allChannels.firstOrNull { it.id == id }?.title ?: id }
                           } else {
                              emptyList()
                           }
                        }.orEmpty()

                        Outcome.Success(
                           RuleDetailsScreenState(
                              ruleMetadata = rule,
                              preferences = preferences,
                              targetAppName = targetAppName,
                              targetChannelNames = targetChannelNames,
                           )
                        )
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

   fun changeTargetApp(appPkg: String, channelIds: List<String>) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleDetailsViewModel.changeTargetApp(appPkg = $appPkg, channelIds = $channelIds)" }

      rulesRepository.updateRulePreference(this@RuleDetailsViewModel.key.id) {
         if (appPkg.isNotEmpty()) {
            it[RuleOption.conditionAppPackage] = appPkg
         } else {
            it.remove(RuleOption.conditionAppPackage)
         }

         it[RuleOption.conditionNotificationChannels] = channelIds.toSet()
      }
   }
}

@Stable
data class RuleDetailsScreenState(
   val ruleMetadata: RuleMetadata,
   val preferences: Preferences,
   val targetAppName: String? = null,
   val targetChannelNames: List<String> = emptyList(),
)
