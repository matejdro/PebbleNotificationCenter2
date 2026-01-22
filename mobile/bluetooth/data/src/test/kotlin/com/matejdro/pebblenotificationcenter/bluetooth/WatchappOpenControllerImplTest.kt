package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.time.virtualTimeProvider

class WatchappOpenControllerImplTest {
   private val scope = TestScope()
   private val pebbleSender = FakePebbleSender(scope.virtualTimeProvider())
   private val controller: WatchappOpenController = WatchappOpenControllerImpl(pebbleSender)

   @Test
   fun `Return false by default`() {
      controller.isNextWatchappOpenForAutoSync() shouldBe false
   }

   @Test
   fun `Return true when set`() {
      controller.setNextWatchappOpenForAutoSync()

      controller.isNextWatchappOpenForAutoSync() shouldBe true
   }

   @Test
   fun `Return false when reset`() {
      controller.setNextWatchappOpenForAutoSync()
      controller.resetNextWatchappOpen()

      controller.isNextWatchappOpenForAutoSync() shouldBe false
   }

   @Test
   fun `Start the app on the watch when requested`() = scope.runTest {
      controller.openWatchapp()

      pebbleSender.startedApps.shouldContainExactly(
         FakePebbleSender.AppLifecycleEvent(WATCHAPP_UUID, null)
      )
   }
}
