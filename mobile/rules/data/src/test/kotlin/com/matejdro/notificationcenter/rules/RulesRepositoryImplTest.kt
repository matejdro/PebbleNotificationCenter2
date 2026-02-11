package com.matejdro.notificationcenter.rules

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.matejdro.notificationcenter.rules.sqldelight.generated.Database
import com.matejdro.notificationcenter.rules.sqldelight.generated.DbRuleQueries
import com.matejdro.notificationcenter.rules.util.FakeDatastoreManager
import dispatch.core.IOCoroutineScope
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import si.inova.kotlinova.core.test.TestScopeWithDispatcherProvider
import si.inova.kotlinova.core.test.outcomes.shouldBeSuccessWithData

class RulesRepositoryImplTest {
   private val scope = TestScopeWithDispatcherProvider()
   private val repo = RulesRepositoryImpl(
      IOCoroutineScope(scope.backgroundScope.coroutineContext),
      createTestRuleQueries(),
      FakeDatastoreManager()
   )

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

   @Test
   fun `Move rule downwards`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         repo.insert("Rule B")
         repo.insert("Rule C")
         repo.insert("Rule D")
         runCurrent()

         repo.reorder(3, 5)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            RuleMetadata(1, "Default Settings"),
            RuleMetadata(2, "Rule A"),
            RuleMetadata(4, "Rule C"),
            RuleMetadata(5, "Rule D"),
            RuleMetadata(3, "Rule B"),
         )
      }
   }

   @Test
   fun `Move rule upwards`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         repo.insert("Rule B")
         repo.insert("Rule C")
         repo.insert("Rule D")
         runCurrent()

         repo.reorder(5, 1)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            RuleMetadata(1, "Default Settings"),
            RuleMetadata(2, "Rule A"),
            RuleMetadata(5, "Rule D"),
            RuleMetadata(3, "Rule B"),
            RuleMetadata(4, "Rule C"),
         )
      }
   }

   @Test
   fun `Disallow moving to the first place`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         repo.insert("Rule B")
         repo.insert("Rule C")
         repo.insert("Rule D")
         runCurrent()

         shouldThrow<IllegalArgumentException> {
            repo.reorder(5, 0)
            runCurrent()
         }

         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun `Handle reordering after delete`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         repo.insert("Rule B")
         repo.insert("Rule C")
         repo.insert("Rule D")
         runCurrent()

         repo.delete(3)
         runCurrent()

         repo.reorder(2, 3)
         runCurrent()

         expectMostRecentItem() shouldBeSuccessWithData listOf(
            RuleMetadata(1, "Default Settings"),
            RuleMetadata(4, "Rule C"),
            RuleMetadata(2, "Rule A"),
            RuleMetadata(5, "Rule D"),
         )
      }
   }

   @Test
   fun `Get a single rule`() = scope.runTest {
      repo.getAll().test {
         runCurrent()
         cancelAndIgnoreRemainingEvents()
      }

      repo.insert("Rule A")
      repo.insert("Rule B")
      runCurrent()

      repo.getSingle(2).test {
         runCurrent()
         expectMostRecentItem() shouldBeSuccessWithData RuleMetadata(2, "Rule A")
      }
   }

   @Test
   fun `Return empty preferences for added rules`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         runCurrent()

         repo.getRulePreferences(2).first().asMap().shouldBeEmpty()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun `Store preferences`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         runCurrent()

         repo.updateRulePreference(1) {
            it[RuleOption.filterAppPackage] = "package.default"
         }
         repo.updateRulePreference(2) {
            it[RuleOption.filterAppPackage] = "package.A"
         }
         runCurrent()

         repo.getRulePreferences(1).first()[RuleOption.filterAppPackage] shouldBe "package.default"
         repo.getRulePreferences(2).first()[RuleOption.filterAppPackage] shouldBe "package.A"
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun `Clear preferences after deleting`() = scope.runTest {
      repo.getAll().test {
         runCurrent()

         repo.insert("Rule A")
         runCurrent()

         repo.updateRulePreference(2) {
            it[RuleOption.filterAppPackage] = "package.default"
         }

         repo.delete(2)
         runCurrent()
         repo.insert("Rule A")
         runCurrent()

         repo.getRulePreferences(2).first().asMap().shouldBeEmpty()
         cancelAndIgnoreRemainingEvents()
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
