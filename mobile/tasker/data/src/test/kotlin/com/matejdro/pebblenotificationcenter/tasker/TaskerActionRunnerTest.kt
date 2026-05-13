package com.matejdro.pebblenotificationcenter.tasker

import android.os.Bundle
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.pebblenotificationcenter.common.test.InMemoryDataStore
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.keys.get
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class TaskerActionRunnerTest {
   private val datastore = InMemoryDataStore(emptyPreferences())
   private val runner = TaskerActionRunner(datastore)

   @Test
   fun `Toggle phone mute`() = runTest {
      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, TaskerAction.TOGGLE_MUTE.name)
            putBoolean(BundleKeys.MUTE_PHONE, true)
            putBoolean(BundleKeys.MUTE_WATCH, false)
         }
      )

      datastore.data.first().get(GlobalPreferenceKeys.mutePhone) shouldBe true
      datastore.data.first().get(GlobalPreferenceKeys.muteWatch) shouldBe false
   }

   @Test
   fun `Toggle watch mute`() = runTest {
      runner.run(
         Bundle().apply {
            putString(BundleKeys.ACTION, TaskerAction.TOGGLE_MUTE.name)
            putBoolean(BundleKeys.MUTE_PHONE, false)
            putBoolean(BundleKeys.MUTE_WATCH, true)
         }
      )

      datastore.data.first().get(GlobalPreferenceKeys.mutePhone) shouldBe false
      datastore.data.first().get(GlobalPreferenceKeys.muteWatch) shouldBe true
   }
}
