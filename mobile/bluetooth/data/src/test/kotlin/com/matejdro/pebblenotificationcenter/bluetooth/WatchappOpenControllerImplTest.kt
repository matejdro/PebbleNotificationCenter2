package com.matejdro.pebblenotificationcenter.bluetooth

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WatchappOpenControllerImplTest {
   private val controller: WatchappOpenController = WatchappOpenControllerImpl()

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
}
