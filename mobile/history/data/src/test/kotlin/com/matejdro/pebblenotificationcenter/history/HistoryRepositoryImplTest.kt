package com.matejdro.pebblenotificationcenter.history

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.matejdro.pebblenotificationcenter.history.sqldelight.generated.Database
import com.matejdro.pebblenotificationcenter.history.sqldelight.generated.DbHistoryQueries
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.outcome.mapData
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData
import java.time.Instant

class HistoryRepositoryImplTest {
   private val scope = TestScopeWithDispatcherProvider()

   private val repo = HistoryRepositoryImpl(createTestRuleQueries())

   @Test
   fun `Insert entries into the repository`() = scope.runTest {
      repo.addHistoryEntry(
         HistoryEntry(
            "Title A",
            "Subtitle A",
            Instant.ofEpochMilli(1000),
            listOf("R1", "R2"),
            "mute A",
            "hide A"
         ),
      )

      repo.addHistoryEntry(
         HistoryEntry(
            "Title B",
            "Subtitle B",
            Instant.ofEpochMilli(2000),
            emptyList(),
         ),
      )

      repo.getHistory().test {
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            HistoryEntry(
               "Title B",
               "Subtitle B",
               Instant.ofEpochMilli(2000),
               emptyList(),
            ),
            HistoryEntry(
               "Title A",
               "Subtitle A",
               Instant.ofEpochMilli(1000),
               listOf("R1", "R2"),
               "mute A",
               "hide A"
            )
         )
      }
   }

   @Test
   fun `Only keep last 100 entries`() = scope.runTest {
      repeat(200) { index ->
         repo.addHistoryEntry(
            HistoryEntry(
               "Title",
               "Subtitle",
               Instant.ofEpochMilli(index.toLong()),
               emptyList(),
            ),
         )
      }

      val expectedTimes = List(100) {
         (199 - it).toLong()
      }

      repo.getHistory().test {
         runCurrent()

         expectMostRecentItem().mapData { list -> list.map { it.time.toEpochMilli() } } shouldBeSuccessWithData expectedTimes
      }
   }
}

internal fun createTestRuleQueries(
   driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
      Database.Schema.create(
         this
      )
   },
): DbHistoryQueries {
   return Database(driver).dbHistoryQueries
}
