package com.matejdro.notificationcenter.rules.ui.details

import androidx.compose.runtime.Stable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.RulesRepository
import com.matejdro.notificationcenter.rules.keys.PreferenceKeyWithDefault
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.notificationcenter.rules.keys.setTo
import com.matejdro.notificationcenter.rules.ui.errors.RuleMissingException
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.common.preferences.plus
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceController
import com.matejdro.pebblenotificationcenter.notification.api.AppNameProvider
import dev.zacsweers.metro.Inject
import dispatch.core.withDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.outcome.mapDataSuspend
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
         val ruleFlow = rulesRepository.getSingle(key.id).distinctUntilChanged()

         val defaultSettingsPreferenceFlow = if (key.id != RULE_ID_DEFAULT_SETTINGS) {
            rulesRepository.getRulePreferences(RULE_ID_DEFAULT_SETTINGS)
         } else {
            flowOf(emptyPreferences())
         }

         val preferencesFlow = defaultSettingsPreferenceFlow.flatMapLatest { defaultPreferences ->
            rulesRepository.getRulePreferences(key.id).map { overridePreferences ->
               defaultPreferences + overridePreferences
            }
         }.distinctUntilChanged()

         emitAll(
            combine(ruleFlow, preferencesFlow) { ruleOutcome, preferences ->
               ruleOutcome.mapDataSuspend { rule ->
                  if (rule == null) {
                     throw RuleMissingException()
                  }

                  val targetAppPackage = preferences[RuleOption.conditionAppPackage]
                  val targetAppName = withDefault { targetAppPackage?.let(appNameProvider::getAppName) }

                  val targetChannelNames = targetAppPackage?.let { pkg ->
                     val channelIds = preferences[RuleOption.conditionNotificationChannels]
                     if (!channelIds.isEmpty()) {
                        val allChannels = withDefault { notificationServiceController.getNotificationChannels(pkg) }

                        channelIds.map { id -> allChannels.firstOrNull { it.id == id }?.title ?: id }
                     } else {
                        emptyList()
                     }
                  }.orEmpty()

                  RuleDetailsScreenState(
                     ruleMetadata = rule,
                     preferences = preferences,
                     targetAppName = targetAppName,
                     targetChannelNames = targetChannelNames,
                     whitelistRegexes = preferences[RuleOption.conditionWhitelistRegexes].toList(),
                     blacklistRegexes = preferences[RuleOption.conditionBlacklistRegexes].toList()
                  )
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

   fun <T> updatePreference(key: PreferenceKeyWithDefault<T>, value: T) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleDetailsViewModel.updatePreference($key, ${value ?: "null"})" }

      rulesRepository.updateRulePreferences(this@RuleDetailsViewModel.key.id, key setTo value)
   }

   fun changeTargetApp(appPkg: String, channelIds: List<String>) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleDetailsViewModel.changeTargetApp(appPkg = $appPkg, channelIds = $channelIds)" }

      val nullableAppPkg = appPkg.takeIf(String::isNotBlank)

      rulesRepository.updateRulePreferences(
         this@RuleDetailsViewModel.key.id,
         RuleOption.conditionAppPackage setTo nullableAppPkg,
         RuleOption.conditionNotificationChannels setTo channelIds.toSet()
      )
   }

   fun addRegex(whitelist: Boolean, regex: String) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleDetailsViewModel.addRegex(whitelist = $whitelist, regex = $regex)" }

      updateRegexes(whitelist) {
         it + regex
      }
   }

   fun editRegex(whitelist: Boolean, index: Int, newValue: String?) = resources.launchWithExceptionReporting {
      actionLogger.logAction {
         "RuleDetailsViewModel.editRegex(whitelist = $whitelist, index = $index, newValue = ${newValue ?: "null"})"
      }

      updateRegexes(whitelist) { list ->
         if (newValue != null) {
            list.mapIndexed { listIndex, existingValue -> if (listIndex == index) newValue else existingValue }
         } else {
            list.filterIndexed { listIndex, _ -> listIndex != index }
         }
      }
   }

   private suspend fun updateRegexes(whitelist: Boolean, update: (List<String>) -> List<String>) {
      val data = uiState.value.data ?: return
      val existingValue = if (whitelist) data.whitelistRegexes else data.blacklistRegexes
      val targetPreference = if (whitelist) RuleOption.conditionWhitelistRegexes else RuleOption.conditionBlacklistRegexes
      val newSet = update(existingValue).toSet()

      rulesRepository.updateRulePreferences(key.id, targetPreference setTo newSet)
   }
}

@Stable
data class RuleDetailsScreenState(
   val ruleMetadata: RuleMetadata,
   val preferences: Preferences,
   val targetAppName: String? = null,
   val targetChannelNames: List<String> = emptyList(),
   val whitelistRegexes: List<String> = emptyList(),
   val blacklistRegexes: List<String> = emptyList(),
)
