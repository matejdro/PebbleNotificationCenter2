package com.matejdro.notificationcenter.rules.ui.details

import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.notificationcenter.rules.keys.setTo
import com.matejdro.notificationcenter.rules.ui.errors.RuleMissingException
import com.matejdro.pebblenotificationcenter.FakeNotificationServiceController
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.notification.api.AppNameProvider
import com.matejdro.pebblenotificationcenter.notification.model.LightNotificationChannel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.outcome.Outcome
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.shouldBeErrorWith
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager
import si.inova.kotlinova.navigation.instructions.navigateTo
import si.inova.kotlinova.navigation.test.FakeNavigator

class RuleDetailsViewModelTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val rulesRepository = FakeRulesRepository()
   private val appNameProvider: AppNameProvider = AppNameProvider {
      "Name of $it"
   }
   private val notificationServiceController = FakeNotificationServiceController()

   private val defaultScreenKey = RuleDetailsScreenKey(2)
   private val navigator = FakeNavigator(defaultScreenKey)

   private val viewModel = RuleDetailsViewModel(
      scope.testCoroutineResourceManager(),
      {},
      rulesRepository,
      appNameProvider,
      notificationServiceController,
      navigator,
   )

   @BeforeEach
   fun setUp() {
      viewModel.key = defaultScreenKey
   }

   @Test
   fun `Provide rule on startup`() = scope.runTest {
      insertDefaultRules()

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.uiState.value shouldBeSuccessWithData RuleDetailsScreenState(RuleMetadata(2, "Rule B"), emptyPreferences())
   }

   @Test
   fun `Show error on missing rule`() = scope.runTest {
      rulesRepository.insert("Default Rule")

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.uiState.value.shouldBeErrorWith(exceptionType = RuleMissingException::class.java)
   }

   @Test
   fun `Delete rule`() = scope.runTest {
      insertDefaultRules()

      viewModel.onServiceRegistered()
      viewModel.deleteRule()
      runCurrent()

      rulesRepository.getAll().first().data.shouldContainExactly(listOf(RuleMetadata(1, "Default Rule")))
   }

   @Test
   fun `Rename rule`() = scope.runTest {
      insertDefaultRules()

      viewModel.onServiceRegistered()
      viewModel.renameRule("Rule C")
      runCurrent()

      rulesRepository.getSingle(2).first().data shouldBe RuleMetadata(2, "Rule C")
   }

   @Test
   fun `Provide preferences`() = scope.runTest {
      insertDefaultRules()
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "pkg",
      )

      viewModel.onServiceRegistered()

      viewModel.uiState.test {
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.conditionAppPackage] shouldBe "pkg"

         rulesRepository.updateRulePreferences(
            2,
            RuleOption.conditionAppPackage setTo "pkg2",
         )
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.conditionAppPackage] shouldBe "pkg2"
      }
   }

   @Test
   fun `Update preferences`() = scope.runTest {
      insertDefaultRules()
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "pkg",
      )

      viewModel.onServiceRegistered()

      viewModel.uiState.test {
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.conditionAppPackage] shouldBe "pkg"

         viewModel.updatePreference(RuleOption.conditionAppPackage, "pkg2")
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.conditionAppPackage] shouldBe "pkg2"
      }
   }

   @Test
   fun `Provide target app`() = scope.runTest {
      insertDefaultRules()
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "pkg",
         RuleOption.conditionNotificationChannels setTo setOf("C1", "C2"),
      )

      notificationServiceController.putNotificationChannels(
         "pkg",
         listOf(
            LightNotificationChannel("C1", "Channel 1"),
            LightNotificationChannel("C2", "Channel 2"),
            LightNotificationChannel("C3", "Channel 3"),
         )
      )

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.apply {
         targetAppName shouldBe "Name of pkg"
         targetChannelNames shouldBe listOf("Channel 1", "Channel 2")
      }
   }

   @Test
   fun `Update target app`() = scope.runTest {
      insertDefaultRules()
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "pkg",
         RuleOption.conditionNotificationChannels setTo setOf("C1", "C2"),
      )

      notificationServiceController.putNotificationChannels(
         "pkg",
         listOf(
            LightNotificationChannel("C1", "Channel 1"),
            LightNotificationChannel("C2", "Channel 2"),
            LightNotificationChannel("C3", "Channel 3"),
         )
      )

      notificationServiceController.putNotificationChannels(
         "pkg2",
         listOf(
            LightNotificationChannel("C1", "Channelu 1"),
            LightNotificationChannel("C2", "Channelu 2"),
            LightNotificationChannel("C3", "Channelu 3"),
         )
      )

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.changeTargetApp("pkg2", listOf("C3"))
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.apply {
         targetAppName shouldBe "Name of pkg2"
         targetChannelNames shouldBe listOf("Channelu 3")
      }
   }

   @Test
   fun `Update target to null when blank`() = scope.runTest {
      insertDefaultRules()
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "pkg",
      )

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.changeTargetApp("", emptyList())
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.apply {
         targetAppName shouldBe null
         targetChannelNames shouldBe emptyList()
      }
   }

   @Test
   fun `Provide default rule preference when override is not set`() = scope.runTest {
      insertDefaultRules()
      rulesRepository.updateRulePreferences(
         1,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE,
      )

      viewModel.onServiceRegistered()

      viewModel.uiState.test {
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.HIDE

         rulesRepository.updateRulePreferences(
            1,
            RuleOption.masterSwitch setTo RuleOption.MasterSwitch.MUTE,
         )
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.MUTE

         rulesRepository.updateRulePreferences(
            2,
            RuleOption.masterSwitch setTo RuleOption.MasterSwitch.SHOW,
         )
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.SHOW

         rulesRepository.updateRulePreferences(
            1,
            RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE,
         )
         runCurrent()
         expectNoEvents()
      }
   }

   @Test
   fun `Add whitelist regular expression`() = scope.runTest {
      insertDefaultRules()

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.addRegex(whitelist = true, regex = "Test.*")
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.whitelistRegexes
         .shouldContainExactly("Test.*")
   }

   @Test
   fun `Add blacklist regular expression`() = scope.runTest {
      insertDefaultRules()

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.addRegex(whitelist = false, regex = "ABC+")
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.blacklistRegexes
         .shouldContainExactly("ABC+")
   }

   @Test
   fun `Edit whitelist regular expression`() = scope.runTest {
      insertDefaultRules()

      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionWhitelistRegexes setTo setOf("R1", "R2", "R3")
      )

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.editRegex(whitelist = true, index = 1, "R2.5")
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.whitelistRegexes
         .shouldContainExactly("R1", "R2.5", "R3")
   }

   @Test
   fun `Edit blacklist regular expression`() = scope.runTest {
      insertDefaultRules()

      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionBlacklistRegexes setTo setOf("R1", "R2", "R3")
      )

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.editRegex(whitelist = false, index = 1, "R2.5")
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.blacklistRegexes
         .shouldContainExactly("R1", "R2.5", "R3")
   }

   @Test
   fun `Delete whitelist regular expression`() = scope.runTest {
      insertDefaultRules()

      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionWhitelistRegexes setTo setOf("R1", "R2", "R3")
      )

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.editRegex(whitelist = true, index = 0, null)
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.whitelistRegexes
         .shouldContainExactly("R2", "R3")
   }

   @Test
   fun `Delete blacklist regular expression`() = scope.runTest {
      insertDefaultRules()

      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionBlacklistRegexes setTo setOf("R1", "R2", "R3")
      )

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.editRegex(whitelist = false, index = 0, null)
      runCurrent()

      viewModel.uiState.value.shouldBeInstanceOf<Outcome.Success<RuleDetailsScreenState>>().data.blacklistRegexes
         .shouldContainExactly("R2", "R3")
   }

   @Test
   fun `Create a copy of the current rule`() = scope.runTest {
      insertDefaultRules()

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.copyRule("Copy of B")
      runCurrent()

      rulesRepository.getSingle(3).first() shouldBeSuccessWithData RuleMetadata(3, "Copy of B")
   }

   @Test
   fun `Navigate to the details of created copy`() = scope.runTest {
      insertDefaultRules()

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.copyRule("Copy of B")
      runCurrent()

      navigator.backstack.shouldContainExactly(RuleDetailsScreenKey(3))
   }

   @Test
   fun `Navigate back after deleting`() = scope.runTest {
      navigator.navigateTo(RuleDetailsScreenKey(3))
      insertDefaultRules()

      viewModel.onServiceRegistered()
      viewModel.deleteRule()
      runCurrent()

      navigator.backstack.shouldContainExactly(defaultScreenKey)
   }

   private suspend fun insertDefaultRules() {
      rulesRepository.insert("Default Rule")
      rulesRepository.insert("Rule B")
   }
}
