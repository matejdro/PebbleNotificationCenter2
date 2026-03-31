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
      context.resources.putString(R.string.snooze, "Snooze")
      context.resources.putString(R.string.show_image, "Show image")

      repo = ActionOrderRepositoryImpl(context, preferenceStore)
   }

   @Test
   fun `Start with default actions`() = runTest {
      repo.getList().test {
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Dismiss",
            "Show image",
            "Other actions",
            "Snooze",
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
            "Show image",
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
            "Show image",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
            "Dismiss",
            "Snooze",
         )
      }
   }

   @Test
   fun `Do not remove non-default entries after adding default ones`() = runTest {
      preferenceStore.edit { prefs ->
         prefs[GlobalPreferenceKeys.actionOrder] = listOf(
            "Reply",
            "Other actions",
            "Snooze",
            "Show image",
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
            "Snooze",
            "Show image",
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
            "Show image",
            "Other actions",
            "Snooze",
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
         repo.moveOrder("Dismiss", 5)
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Show image",
            "Other actions",
            "Snooze",
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
         Action.Dismiss("Pause app", 0u),
         Action.Dismiss("Dismiss", 1u),
         Action.Dismiss("Other actions", 2u),
      )

      repo.sort(inputList) shouldBe listOf(
         Action.Dismiss("Dismiss", 1u),
         Action.Dismiss("Other actions", 2u),
         Action.Dismiss("Pause app", 0u),
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
         Action.Dismiss("Pause app", 0u),
         Action.Dismiss("Dismiss", 1u),
         Action.Dismiss("Other actions", 2u),
      )

      repo.sort(inputList) shouldBe listOf(
         Action.Dismiss("Other actions", 2u),
         Action.Dismiss("Pause app", 0u),
         Action.Dismiss("Dismiss", 1u),
      )
   }

   @Test
   fun `Unknown actions should be sorted according to the 'Other Actions' entry`() = runTest {
      val inputList = listOf(
         Action.Dismiss("Pause app", 0u),
         Action.Dismiss("Dismiss", 1u),
         Action.Dismiss("Reply", 3u),
      )

      repo.sort(inputList) shouldBe listOf(
         Action.Dismiss("Dismiss", 1u),
         Action.Dismiss("Reply", 3u),
         Action.Dismiss("Pause app", 0u),
      )
   }

   @Test
   fun `Unknown actions should be added to the list after the 'Other Actions' entry`() = runTest {
      val inputList = listOf(
         Action.Dismiss("Pause app", 0u),
         Action.Dismiss("Dismiss", 1u),
         Action.Dismiss("Reply", 3u),
      )

      repo.sort(inputList)

      repo.getList().test {
         runCurrent()

         expectMostRecentItem() shouldBe listOf(
            "Dismiss",
            "Show image",
            "Other actions",
            "Reply",
            "Snooze",
            "Pause app",
            "Unpause app",
            "Pause conversation",
            "Unpause conversation",
         )
      }
   }
}
