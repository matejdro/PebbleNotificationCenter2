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

class RuleDetailsViewModelTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val rulesRepository = FakeRulesRepository()
   private val appNameProvider: AppNameProvider = AppNameProvider {
      "Name of $it"
   }
   private val notificationServiceController = FakeNotificationServiceController()

   private val viewModel = RuleDetailsViewModel(
      scope.testCoroutineResourceManager(),
      {},
      rulesRepository,
      appNameProvider,
      notificationServiceController,
   )

   @BeforeEach
   fun setUp() {
      viewModel.key = RuleDetailsScreenKey(2)
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
      rulesRepository.insert("Rule A")

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

      rulesRepository.getAll().first().data.shouldContainExactly(listOf(RuleMetadata(1, "Rule A")))
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

   private suspend fun insertDefaultRules() {
      rulesRepository.insert("Rule A")
      rulesRepository.insert("Rule B")
   }
}
