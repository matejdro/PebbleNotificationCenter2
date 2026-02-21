package com.matejdro.pebblenotificationcenter.notification

import android.app.createPendingIntent
import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.setTo
import com.matejdro.pebblenotificationcenter.bluetooth.FakeWatchSyncer
import com.matejdro.pebblenotificationcenter.bluetooth.FakeWatchappOpenController
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.NativeAction
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.fakes.FakeActivity
import java.time.Instant

class NotificationProcessorTest {
   private val watchSyncer = FakeWatchSyncer()

   private val context = FakeActivity()

   private val openController = FakeWatchappOpenController()

   private val rulesRepository = FakeRulesRepository()

   private val processor = NotificationProcessor(context, watchSyncer, openController, RuleResolver(rulesRepository))

   @BeforeEach
   fun setUp() {
      context.resources.putString(R.string.dismiss, "Dismiss")

      runBlocking {
         rulesRepository.insert("Default Rule")
      }
   }

   @Test
   fun `It should forward received notifications to the watch syncer`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.map { it.systemData }.shouldContainExactly(notification)
   }

   @Test
   fun `It should forward notification deletions to the watch syncer`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationDismissed("key")

      watchSyncer.clearedNotifications.shouldContainExactly("key")
   }

   @Test
   fun `It should forward notification clears to the watch syncer`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationsCleared()

      watchSyncer.clearAllCalled shouldBe true
   }

   @Test
   fun `It should allow getting received notifications`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      assertSoftly(processor.getNotification(1).shouldNotBeNull()) {
         systemData shouldBe ParsedNotification(
            "key",
            "com.app",
            "Title",
            "sTitle",
            "Body",
            // 19:18:25 GMT | Sunday, January 4, 2026
            Instant.ofEpochSecond(1_767_554_305)
         )

         bucketId shouldBe 1
      }
   }

   @Test
   fun `Dismissing notification should delete it from getNotification query`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationDismissed(notification.key)

      processor.getNotification(1).shouldBeNull()
   }

   @Test
   fun `Clearing should clear internal notifications`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      processor.onNotificationsCleared()

      processor.getNotification(1).shouldBeNull()
   }

   @Test
   fun `It should return dismiss action on every notification`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      processor.getNotification(1)?.actions shouldBe listOf(
         Action.Dismiss("Dismiss")
      )
   }

   @Test
   fun `It should not vibrate for the silent notifications by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe false
      processor.pollNextVibration() shouldBe null
   }

   @Test
   fun `It should vibrate for the silent notifications if that filter is disabled`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.muteSilentNotifications setTo false
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }

   @Test
   fun `It should not vibrate for the loud but filtered by dnd notifications by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false,
         isFilteredByDoNotDisturb = true
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe false
      processor.pollNextVibration() shouldBe null
   }

   @Test
   fun `It should vibrate for the loud but filtered by dnd notifications if that filter is disabled`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.muteDndNotifications setTo false
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false,
         isFilteredByDoNotDisturb = true
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }

   @Test
   fun `It should vibrate for the non-silent notifications by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }

   @Test
   fun `It should not vibrate for the loud notifications with the suppress flag on`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false,
      )

      processor.onNotificationPosted(notification, suppressVibration = true)

      openController.watchappOpened shouldBe false
      processor.pollNextVibration() shouldBe null
   }

   @Test
   fun `It should not vibrate for the duplicate loud notifications `() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false,
      )

      processor.onNotificationPosted(notification)
      openController.watchappOpened = false
      processor.pollNextVibration()

      processor.onNotificationPosted(notification)
      openController.watchappOpened shouldBe false
      processor.pollNextVibration() shouldBe null
   }

   @Test
   fun `It should vibrate for the duplicate loud notifications if that filter is disabled`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.muteIdenticalNotifications setTo false
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false,
      )

      processor.onNotificationPosted(notification)
      openController.watchappOpened = false
      processor.pollNextVibration()

      processor.onNotificationPosted(notification)
      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }

   @Test
   fun `It should return parsed native actions`() = runTest {
      val intent1 = createPendingIntent()
      val intent2 = createPendingIntent()

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         nativeActions = listOf(
            NativeAction("Action 1", intent1),
            NativeAction("Action 2", intent2),
         )
      )

      processor.onNotificationPosted(notification)

      processor.getNotification(1)?.actions.orEmpty() shouldBe listOf(
         Action.Dismiss("Dismiss"),
         Action.Native("Action 1", intent1),
         Action.Native("Action 2", intent2),
      )
   }

   @Test
   fun `Notification should have unread flag set by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      assertSoftly(processor.getNotification(1).shouldNotBeNull()) {
         unread shouldBe true
      }
   }

   @Test
   fun `Notification with suppressed vibration should not have unread flag set by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification, suppressVibration = true)

      assertSoftly(processor.getNotification(1).shouldNotBeNull()) {
         unread shouldBe false
      }
   }

   @Test
   fun `Notification should not be unread after marking as read`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      watchSyncer.nextBucketId = 1
      processor.onNotificationPosted(notification)
      processor.markAsRead(1)

      assertSoftly(processor.getNotification(1).shouldNotBeNull()) {
         unread shouldBe false
      }
      assertSoftly(processor.getNotificationByKey("key").shouldNotBeNull()) {
         unread shouldBe false
      }

      watchSyncer.syncedNotificationReadStatuses.shouldContainExactly(
         ProcessedNotification(
            notification,
            unread = false,
            actions = listOf(Action.Dismiss("Dismiss")),
            bucketId = 1
         )
      )
   }

   @Test
   fun `It should forward received notifications to the watch syncer even with master switch set to MUTE`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.MUTE
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.map { it.systemData }.shouldContainExactly(notification)
   }

   @Test
   fun `It should ignore received notifications with master switch set to hide`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldBeEmpty()
   }

   @Test
   fun `It should not vibrate when the master switch is set to mute`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.MUTE
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe false
      processor.pollNextVibration().shouldBeNull()
   }

   @Test
   fun `It should hide existing notification when an update has a hide master switch set`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notification)
      runCurrent()

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE
      )
      processor.onNotificationPosted(notification)

      watchSyncer.clearedNotifications.shouldContainExactly("key")
   }

   @Test
   fun `It should not forward received notifications that are ongoing by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isOngoing = true
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldBeEmpty()
   }

   @Test
   fun `It should forward received notifications that are ongoing if this filter is disabled`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.hideOngoingNotifications setTo false
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isOngoing = true
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldNotBeEmpty()
   }

   @Test
   fun `It should not forward received notifications that are a group summary by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         groupSummary = true
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldBeEmpty()
   }

   @Test
   fun `It should not forward received notifications that are local only by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         localOnly = true
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldBeEmpty()
   }

   @Test
   fun `It should not forward received media notifications by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         media = true
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldBeEmpty()
   }
}
