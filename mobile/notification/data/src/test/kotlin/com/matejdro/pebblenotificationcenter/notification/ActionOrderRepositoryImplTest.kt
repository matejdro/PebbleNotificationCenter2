package com.matejdro.pebblenotificationcenter.notification

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.matejdro.pebblenotificationcenter.common.test.InMemoryDataStore
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.keys.set
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.fakes.FakeActivity

class ActionOrderRepositoryImplTest {
   private val context = FakeActivity()
   private val preferenceStore = InMemoryDataStore(emptyPreferences())
   private lateinit var repo: ActionOrderRepositoryImpl

   @BeforeEach
   fun setUp() {
      context.resources.putString(R.string.dismiss, "Dismiss")
      context.resources.putString(R.string.pause_app, "Pause app")
      context.resources.putString(R.string.unpause_app, "Unpause app")
      context.resources.putString(R.string.pause_conversation, "Pause conversation")
      context.resources.putString(R.string.unpause_conversation, "Unpause conversation")
      context.resources.putString(R.string.other_actions, "Other actions")

      repo = ActionOrderRepositoryImpl(context, preferenceStore)
   }

   @Test
   fun `Start with default actions`() = runTest {
      repo.getList().test {
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Dismiss",
            "Other actions",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
         )
      }
   }

   @Test
   fun `Add missing default actions at the end`() = runTest {
      preferenceStore.edit { prefs ->
         prefs[GlobalPreferenceKeys.actionOrder] = listOf(
            "Other actions",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
         )
      }

      repo.getList().test {
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Other actions",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
            "Dismiss",
         )
      }
   }

   @Test
   fun `Do not remove non-default entries after adding default ones`() = runTest {
      preferenceStore.edit { prefs ->
         prefs[GlobalPreferenceKeys.actionOrder] = listOf(
            "Reply",
            "Other actions",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
         )
      }

      repo.getList().test {
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Reply",
            "Other actions",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
            "Dismiss",
         )
      }
   }

   @Test
   fun `Reorder list items up`() = runTest {
      repo.getList().test {
         runCurrent()
         repo.moveOrder("Pause app", 0)
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Pause app",
            "Dismiss",
            "Other actions",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
         )
      }
   }

   @Test
   fun `Reorder list items down`() = runTest {
      repo.getList().test {
         runCurrent()
         repo.moveOrder("Dismiss", 3)
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Other actions",
            "Pause app",
            "Unpause app",
            "Dismiss",
            "Pause conversation",
            "Unpause conversation",
         )
      }
   }

   @Test
   fun `Sort provided list according to the default order`() = runTest {
      val inputList = listOf(
         Action.Dismiss("Pause app"),
         Action.Dismiss("Dismiss"),
         Action.Dismiss("Other actions"),
      )

      repo.sort(inputList) shouldBe listOf(
         Action.Dismiss("Dismiss"),
         Action.Dismiss("Other actions"),
         Action.Dismiss("Pause app"),
      )
   }

   @Test
   fun `Sort provided list according to the stored action order`() = runTest {
      repo.getList().test {
         runCurrent()
         cancelAndIgnoreRemainingEvents()
      }

      repo.moveOrder("Dismiss", 5)

      val inputList = listOf(
         Action.Dismiss("Pause app"),
         Action.Dismiss("Dismiss"),
         Action.Dismiss("Other actions"),
      )

      repo.sort(inputList) shouldBe listOf(
         Action.Dismiss("Other actions"),
         Action.Dismiss("Pause app"),
         Action.Dismiss("Dismiss"),
      )
   }

   @Test
   fun `Unknown actions should be sorted according to the 'Other Actions' entry`() = runTest {
      val inputList = listOf(
         Action.Dismiss("Pause app"),
         Action.Dismiss("Dismiss"),
         Action.Dismiss("Reply"),
      )

      repo.sort(inputList) shouldBe listOf(
         Action.Dismiss("Dismiss"),
         Action.Dismiss("Reply"),
         Action.Dismiss("Pause app"),
      )
   }

   @Test
   fun `Unknown actions should be added to the list after the 'Other Actions' entry`() = runTest {
      val inputList = listOf(
         Action.Dismiss("Pause app"),
         Action.Dismiss("Dismiss"),
         Action.Dismiss("Reply"),
      )

      repo.sort(inputList)

      repo.getList().test {
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Dismiss",
            "Other actions",
            "Reply",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
         )
      }
   }
}
