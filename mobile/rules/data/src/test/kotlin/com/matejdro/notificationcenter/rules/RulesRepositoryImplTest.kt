package com.matejdro.notificationcenter.rules

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.matejdro.notificationcenter.rules.sqldelight.generated.Database
import com.matejdro.notificationcenter.rules.sqldelight.generated.DbRuleQueries
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData

class RulesRepositoryImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val repo = RulesRepositoryImpl(createTestRuleQueries())

   @Test
   fun `Return starting rule by default`() = scope.runTest {
      repo.getAll().test {
         runCurrent()
         expectMostRecentItem() shouldBeSuccessWithData listOf(RuleMetadata(1, "Default Settings"))
      }
   }

   @Test
   fun `Return added rules`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            RuleMetadata(1, "Default Settings"),
            RuleMetadata(2, "Rule A")
         )
      }
   }

   @Test
   fun `Allow editing rules`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         runCurrent()

         repo.edit(RuleMetadata(2, "Rule B"))
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            RuleMetadata(1, "Default Settings"),
            RuleMetadata(2, "Rule B")
         )
      }
   }

   @Test
   fun `Allow deleting rules`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         runCurrent()

         repo.delete(2)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            RuleMetadata(1, "Default Settings"),
         )
      }
   }

   @Test
   fun `Disallow deleting default rule`() = scope.runTest {
      repo.getAll().test {
         runCurrent()
         cancelAndIgnoreRemainingEvents()
      }

      assertThrows<IllegalArgumentException>() {
         repo.delete(1)
         runCurrent()
      }
   }
}

internal fun createTestRuleQueries(
   driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
      Database.Schema.create(
         this
      )
   },
): DbRuleQueries {
   return Database(driver).dbRuleQueries
}
