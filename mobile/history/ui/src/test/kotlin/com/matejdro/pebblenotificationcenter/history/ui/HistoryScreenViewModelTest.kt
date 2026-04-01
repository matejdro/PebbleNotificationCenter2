package com.matejdro.pebblenotificationcenter.history.ui

import app.cash.turbine.test
import com.matejdro.pebblenotificationcenter.history.FakeHistoryRepository
import com.matejdro.pebblenotificationcenter.history.HistoryEntry
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import si.inova.kotlinova.core.test.outcomes.testCoroutineResourceManager
import java.time.Instant

class HistoryScreenViewModelTest {
   private val scope = TestScope()
   private val repository = FakeHistoryRepository()

   private val viewModel = HistoryScreenViewModel(scope.testCoroutineResourceManager(), repository, {})

   @Test
   fun `Expose history data`() = scope.runTest {
      viewModel.onServiceRegistered()

      repository.addHistoryEntry(
         HistoryEntry(
            "Title A",
            "Subtitle A",
            Instant.ofEpochMilli(1000),
            listOf("R1", "R2"),
            "mute A",
            "hide A"
         ),
      )

      repository.addHistoryEntry(
         HistoryEntry(
            "Title B",
            "Subtitle B",
            Instant.ofEpochMilli(2000),
            listOf("R3"),
         ),
      )

      viewModel.uiState.test {
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            HistoryEntry(
               "Title A",
               "Subtitle A",
               Instant.ofEpochMilli(1000),
               listOf("R1", "R2"),
               "mute A",
               "hide A"
            ),
            HistoryEntry(
               "Title B",
               "Subtitle B",
               Instant.ofEpochMilli(2000),
               listOf("R3"),
            )
         )
      }
   }
}
