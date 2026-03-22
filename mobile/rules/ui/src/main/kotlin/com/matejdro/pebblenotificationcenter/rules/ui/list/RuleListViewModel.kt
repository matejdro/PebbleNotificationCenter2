package com.matejdro.pebblenotificationcenter.rules.ui.list

import android.content.res.Resources
import androidx.compose.runtime.Stable
import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import com.matejdro.pebblenotificationcenter.navigation.instructions.OpenScreenOrReplaceExistingType
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.notification.api.AppNameProvider
import com.matejdro.pebblenotificationcenter.rules.MasterSwitch
import com.matejdro.pebblenotificationcenter.rules.RuleMetadata
import com.matejdro.pebblenotificationcenter.rules.RuleOption
import com.matejdro.pebblenotificationcenter.rules.RulesRepository
import com.matejdro.pebblenotificationcenter.rules.keys.setTo
import com.matejdro.pebblenotificationcenter.rules.ui.R
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import si.inova.kotlinova.core.outcome.CoroutineResourceManager
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.outcome.mapData
import si.inova.kotlinova.navigation.navigator.Navigator
import si.inova.kotlinova.navigation.services.ContributesScopedService
import si.inova.kotlinova.navigation.services.SingleScreenViewModel

@Stable
@Inject
@ContributesScopedService
class RuleListViewModel(
   private val resources: CoroutineResourceManager,
   private val actionLogger: ActionLogger,
   private val rulesRepository: RulesRepository,
   private val navigator: Navigator,
   private val appNameProvider: AppNameProvider,
   private val androidResources: Resources,
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
      val newRuleId = rulesRepository.insert(ruleName)

      navigator.navigate(OpenScreenOrReplaceExistingType(RuleDetailsScreenKey(newRuleId)))
   }

   fun addRuleWithAppMute(appPkg: String, channelIds: List<String>) = resources.launchWithExceptionReporting {
      actionLogger.logAction {
         "RuleListViewModel.addRuleWithAppMute(appPkg = $appPkg, channelIds = $channelIds)"
      }

      val ruleName = androidResources.getString(R.string.mute, appNameProvider.getAppName(appPkg))
      addRuleWithMasterSwitchSet(ruleName, appPkg, channelIds, MasterSwitch.MUTE)
   }

   fun addRuleWithAppHide(appPkg: String, channelIds: List<String>) = resources.launchWithExceptionReporting {
      actionLogger.logAction {
         "RuleListViewModel.addRuleWithAppHide(appPkg = $appPkg, channelIds = $channelIds)"
      }

      val ruleName = androidResources.getString(R.string.hide, appNameProvider.getAppName(appPkg))
      addRuleWithMasterSwitchSet(ruleName, appPkg, channelIds, MasterSwitch.HIDE)
   }

   private suspend fun addRuleWithMasterSwitchSet(
      ruleName: String,
      appPkg: String,
      channelIds: List<String>,
      masterSwitch: MasterSwitch,
   ) {
      val newRuleId = rulesRepository.insert(ruleName)
      rulesRepository.updateRulePreferences(
         newRuleId,
         RuleOption.conditionAppPackage setTo appPkg,
         RuleOption.conditionNotificationChannels setTo channelIds.toSet(),
         RuleOption.masterSwitch setTo masterSwitch,
      )
   }

   fun reorder(id: Int, toIndex: Int) = resources.launchWithExceptionReporting {
      actionLogger.logAction { "RuleListViewModel.reorder($id, $toIndex)" }
      rulesRepository.reorder(id, toIndex)
   }
}

@Stable
data class RuleListState(
   val rules: List<RuleMetadata>,
)
