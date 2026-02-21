package com.matejdro.notificationcenter.rules.ui.list

import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RuleMetadata
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleDetailsScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager
import si.inova.kotlinova.navigation.instructions.navigateTo
import si.inova.kotlinova.navigation.test.FakeNavigator

class RuleListViewModelTest {
   private val scope = TestScope()

   private val repo = FakeRulesRepository()

   private val navigator = FakeNavigator(RuleListScreenKey)

   private val viewModel = RuleListViewModel(
      scope.testCoroutineResourceManager(),
      {},
      repo,
      navigator
   )

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

   @Test
   fun `Reorder rules`() = scope.runTest {
      repo.insert("Rule A")
      repo.insert("Rule B")
      repo.insert("Rule C")

      viewModel.onServiceRegistered()
      viewModel.reorder(2, 0)
      runCurrent()

      viewModel.uiState.value shouldBeSuccessWithData RuleListState(
         listOf(
            RuleMetadata(2, "Rule B"),
            RuleMetadata(1, "Rule A"),
            RuleMetadata(3, "Rule C"),
         )
      )
   }

   @Test
   fun `Open the rule details after adding it`() = scope.runTest {
      repo.insert("Rule A")

      viewModel.onServiceRegistered()
      viewModel.addRule("Rule B")
      runCurrent()

      navigator.backstack.shouldContainExactly(RuleListScreenKey, RuleDetailsScreenKey(2))
   }

   @Test
   fun `Replace the existing rule details on the backstack`() = scope.runTest {
      navigator.navigateTo(RuleDetailsScreenKey(1))

      repo.insert("Rule A")

      viewModel.onServiceRegistered()
      viewModel.addRule("Rule B")
      runCurrent()

      navigator.backstack.shouldContainExactly(RuleListScreenKey, RuleDetailsScreenKey(2))
   }
}
