package com.matejdro.notificationcenter.rules.ui.list

import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RuleMetadata
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager

class RuleListViewModelTest {
   private val scope = TestScope()

   private val repo = FakeRulesRepository()

   private val viewModel = RuleListViewModel(scope.testCoroutineResourceManager(), {}, repo)

   @Test
   fun `Display a list of rules`() = scope.runTest {
      repo.insert("Rule A")
      repo.insert("Rule B")

      viewModel.onServiceRegistered()
      runCurrent()

      viewModel.uiState.value shouldBeSuccessWithData RuleListState(
         listOf(
            RuleMetadata(1, "Rule A"),
            RuleMetadata(2, "Rule B"),
         )
      )
   }

   @Test
   fun `Add a rule`() = scope.runTest {
      repo.insert("Rule A")

      viewModel.onServiceRegistered()
      viewModel.addRule("Rule B")
      runCurrent()

      viewModel.uiState.value shouldBeSuccessWithData RuleListState(
         listOf(
            RuleMetadata(1, "Rule A"),
            RuleMetadata(2, "Rule B"),
         )
      )
   }
}
