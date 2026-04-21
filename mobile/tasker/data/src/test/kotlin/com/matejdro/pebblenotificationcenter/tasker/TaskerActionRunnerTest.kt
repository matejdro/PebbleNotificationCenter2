package com.matejdro.pebblenotificationcenter.tasker

import androidx.core.os.bundleOf
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
         bundleOf(
            BundleKeys.ACTION to TaskerAction.TOGGLE_MUTE.name,
            BundleKeys.MUTE_PHONE to true,
            BundleKeys.MUTE_WATCH to false,
         )
      )

      datastore.data.first().get(GlobalPreferenceKeys.mutePhone) shouldBe true
      datastore.data.first().get(GlobalPreferenceKeys.muteWatch) shouldBe false
   }

   @Test
   fun `Toggle watch mute`() = runTest {
      runner.run(
         bundleOf(
            BundleKeys.ACTION to TaskerAction.TOGGLE_MUTE.name,
            BundleKeys.MUTE_PHONE to false,
            BundleKeys.MUTE_WATCH to true,
         )
      )

      datastore.data.first().get(GlobalPreferenceKeys.mutePhone) shouldBe false
      datastore.data.first().get(GlobalPreferenceKeys.muteWatch) shouldBe true
   }
}
