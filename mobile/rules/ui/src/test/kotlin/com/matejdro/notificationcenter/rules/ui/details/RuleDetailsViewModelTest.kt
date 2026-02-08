package com.matejdro.notificationcenter.rules.ui.details

import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.notificationcenter.rules.ui.errors.RuleMissingException
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
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

      viewModel.uiState.value shouldBeSuccessWithData RuleDetailsScreenState(RuleMetadata(2, "Rule B"))
   }

   @Test
   fun `Show error on missing rule`() = scope.runTest {
      rulesRepository.insert("Rule A")

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.uiState.value.shouldBeErrorWith(exceptionType = RuleMissingException::class.java)
   }
}
