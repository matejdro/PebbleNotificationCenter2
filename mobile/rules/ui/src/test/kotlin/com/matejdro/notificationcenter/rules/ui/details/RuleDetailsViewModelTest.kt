package com.matejdro.notificationcenter.rules.ui.details

import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.ui.errors.RuleMissingException
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.outcomes.shouldBeErrorWith
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager

class RuleDetailsViewModelTest {
   private val scope = TestScope()
   private val rulesRepository = FakeRulesRepository()

   private val viewModel = RuleDetailsViewModel(
      scope.testCoroutineResourceManager(),
      {},
      rulesRepository
   )

   @BeforeEach
   fun setUp() {
      viewModel.key = RuleDetailsScreenKey(2)
   }

   @Test
   fun `Provide rule on startup`() = scope.runTest {
      rulesRepository.insert("Rule A")
      rulesRepository.insert("Rule B")

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
      rulesRepository.insert("Rule A")
      rulesRepository.insert("Rule B")

      viewModel.onServiceRegistered()
      viewModel.deleteRule()
      runCurrent()

      rulesRepository.getAll().first().data.shouldContainExactly(listOf(RuleMetadata(1, "Rule A")))
   }

   @Test
   fun `Rename rule`() = scope.runTest {
      rulesRepository.insert("Rule A")
      rulesRepository.insert("Rule B")

      viewModel.onServiceRegistered()
      viewModel.renameRule("Rule C")
      runCurrent()

      rulesRepository.getSingle(2).first().data shouldBe RuleMetadata(2, "Rule C")
   }

   @Test
   fun `Provide preferences`() = scope.runTest {
      rulesRepository.insert("Rule A")
      rulesRepository.insert("Rule B")
      rulesRepository.updateRulePreference(2) {
         it[RuleOption.filterAppPackage] = "pkg"
      }

      viewModel.onServiceRegistered()

      viewModel.uiState.test {
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.filterAppPackage] shouldBe "pkg"

         rulesRepository.updateRulePreference(2) {
            it[RuleOption.filterAppPackage] = "pkg2"
         }
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.filterAppPackage] shouldBe "pkg2"
      }
   }

   @Test
   fun `Update preferences`() = scope.runTest {
      rulesRepository.insert("Rule A")
      rulesRepository.insert("Rule B")
      rulesRepository.updateRulePreference(2) {
         it[RuleOption.filterAppPackage] = "pkg"
      }

      viewModel.onServiceRegistered()

      viewModel.uiState.test {
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.filterAppPackage] shouldBe "pkg"

         viewModel.updatePreference(RuleOption.filterAppPackage, "pkg2")
         runCurrent()
         expectMostRecentItem().data.shouldNotBeNull().preferences[RuleOption.filterAppPackage] shouldBe "pkg2"
      }
   }
}
