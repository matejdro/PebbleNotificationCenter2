package com.matejdro.pebblenotificationcenter.bluetooth

import com.matejdro.pebble.bluetooth.common.test.FakePebbleSender
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rebble.pebblekit2.common.model.WatchIdentifier
import io.rebble.pebblekit2.model.Watchapp
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.time.virtualTimeProvider
import java.util.UUID

class WatchappOpenControllerImplTest {
   private val scope = TestScope()
   private val pebbleSender = FakePebbleSender(scope.virtualTimeProvider())
   private val pebbleInfoRetriever = FakePebbleInfoRetriever()
   private val controller: WatchappOpenController = WatchappOpenControllerImpl(pebbleSender, pebbleInfoRetriever)

   @BeforeEach
   fun setUp() {
      pebbleInfoRetriever.setConnectedWatchIds(listOf(WatchIdentifier("TheWatch")))
   }

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
         FakePebbleSender.AppLifecycleEvent(WATCHAPP_UUID, listOf(WatchIdentifier("TheWatch")))
      )
   }

   @Test
   fun `Close the watchapp when no open call was made`() = scope.runTest {
      controller.closeWatchappToTheLastApp(WatchIdentifier("TheWatch"))

      pebbleSender.startedApps.shouldBeEmpty()
      pebbleSender.stoppedApps.shouldContainExactly(
         FakePebbleSender.AppLifecycleEvent(WATCHAPP_UUID, listOf(WatchIdentifier("TheWatch")))
      )
   }

   @Test
   fun `Close the watchapp when open call was made with unknown watchapp`() = scope.runTest {
      pebbleInfoRetriever.setActiveApp(WatchIdentifier("TheWatch"), null)

      controller.openWatchapp()
      pebbleSender.startedApps.clear()

      controller.closeWatchappToTheLastApp(WatchIdentifier("TheWatch"))

      pebbleSender.startedApps.shouldBeEmpty()
      pebbleSender.stoppedApps.shouldContainExactly(
         FakePebbleSender.AppLifecycleEvent(WATCHAPP_UUID, listOf(WatchIdentifier("TheWatch")))
      )
   }

   @Test
   fun `Open the previous app when it is known from the previous open call`() = scope.runTest {
      val otherApp = UUID.fromString("caf5e298-d9e7-44a9-9177-d5ed6acb719a")
      pebbleInfoRetriever.setActiveApp(WatchIdentifier("TheWatch"), Watchapp(otherApp, "Important app", Watchapp.Type.WATCHAPP))

      controller.openWatchapp()
      pebbleSender.startedApps.clear()

      controller.closeWatchappToTheLastApp(WatchIdentifier("TheWatch"))

      pebbleSender.startedApps.shouldContainExactly(
         FakePebbleSender.AppLifecycleEvent(otherApp, listOf(WatchIdentifier("TheWatch")))
      )
      pebbleSender.stoppedApps.shouldBeEmpty()
   }
}
