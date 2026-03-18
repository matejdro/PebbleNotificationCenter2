package com.matejdro.pebblenotificationcenter.notification

import android.app.createPendingIntent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.matejdro.notificationcenter.common.test.InMemoryDataStore
import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.set
import com.matejdro.notificationcenter.rules.keys.setTo
import com.matejdro.pebblenotificationcenter.bluetooth.FakeWatchSyncer
import com.matejdro.pebblenotificationcenter.bluetooth.FakeWatchappOpenController
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.NativeAction
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.PauseStatus
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
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

   private val globalPreferences = InMemoryDataStore(emptyPreferences())

   private val pauseController = FakePauseController()

   private val processor = NotificationProcessor(
      context,
      watchSyncer,
      openController,
      RuleResolver(rulesRepository),
      globalPreferences,
      pauseController,
   )

   @BeforeEach
   fun setUp() {
      context.resources.putString(R.string.dismiss, "Dismiss")
      context.resources.putString(R.string.pause_app, "Pause app")
      context.resources.putString(R.string.unpause_app, "Unpause app")
      context.resources.putString(R.string.pause_conversation, "Pause conversation")
      context.resources.putString(R.string.unpause_conversation, "Unpause conversation")
      context.resources.putString(R.string.app_suffix) { "${it.elementAt(0)} (App)" }

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
   fun `It should return dismiss and pause actions on every notification`() = runTest {
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
         Action.Dismiss("Dismiss"),
         Action.PauseApp("Pause app"),
         Action.PauseConversation("Pause conversation"),
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
   fun `It should vibrate for the non-silent notifications by default with jackhammer`() = runTest {
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
      processor.pollNextVibration().shouldNotBeNull().toList().shouldContainExactly(
         50, 50, 50, 50, 50, 50, 50, 50, 50, 50
      )
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

      processor.getNotification(1)?.actions.orEmpty().shouldContainAll(
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

      watchSyncer.syncedNotificationReadStatuses.shouldHaveSize(1).first().unread shouldBe false
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
   fun `It should forward received notifications that are a group summary when that filter is disabled`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.hideGroupSummaryNotifications setTo false
      )

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

      watchSyncer.syncedNotifications.shouldNotBeEmpty()
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
   fun `It should forward received notifications that are local only when that filter is disabled`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.hideLocalOnlyNotifications setTo false
      )

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

      watchSyncer.syncedNotifications.shouldNotBeEmpty()
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

   @Test
   fun `It should forward received media notifications when that filter is disabled`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.hideMediaNotifications setTo false
      )

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

      watchSyncer.syncedNotifications.shouldNotBeEmpty()
   }

   @Test
   fun `Force show & vibrate forced notifications regardless of the settings`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isOngoing = true,
         isSilent = true,
         forceVibrate = true
      )

      processor.onNotificationPosted(notification)

      watchSyncer.syncedNotifications.shouldNotBeEmpty()
      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }

   @Test
   fun `It should vibrate with the override pattern for the non-silent notifications by default`() = runTest {
      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         isSilent = false,
         overrideVibrationPattern = listOf(10, 20, 30, 40)
      )

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull().toList().shouldContainExactly(10, 20, 30, 40)
   }

   @Test
   fun `It should obey vibration pattern setting`() = runTest {
      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.vibrationPattern setTo "1, 2, 3, 4"
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

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull().toList().shouldContainExactly(
         1, 2, 3, 4
      )
   }

   @Test
   fun `It should return parsed reply action`() = runTest {
      val intent = createPendingIntent()

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         nativeActions = listOf(
            NativeAction(
               "Action 1",
               intent,
               remoteInputResultKey = "inputKey",
               cannedTexts = listOf("A", "B"),
               allowFreeFormInput = false
            ),
         )
      )

      processor.onNotificationPosted(notification)

      processor.getNotification(1)?.actions.orEmpty().shouldContain(
         Action.Reply(
            title = "Action 1",
            intent = intent,
            remoteInputResultKey = "inputKey",
            cannedTexts = listOf("A", "B"),
            allowFreeFormInput = false
         )
      )
   }

   @Test
   fun `It should not vibrate for any notifications when global watch mute is active`() = runTest {
      globalPreferences.edit {
         it[GlobalPreferenceKeys.muteWatch] = true
      }
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
   fun `It should forward received new notifications to the pause controller`() = runTest {
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

      pauseController.newNotifications.shouldContainExactly(notification)
   }

   @Test
   fun `It should not forward updated notifications to the pause controller`() = runTest {
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
      pauseController.newNotifications.clear()

      processor.onNotificationPosted(notification)
      pauseController.newNotifications.shouldBeEmpty()
   }

   @Test
   fun `It should forward notification deletions to the pause controller`() = runTest {
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

      pauseController.dismissedNotifications.shouldContainExactly(notification)
   }

   @Test
   fun `It should not vibrate for notifications that are app paused`() = runTest {
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

      pauseController.pauseStatuses[notification] = PauseStatus(app = true)

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe false
      processor.pollNextVibration().shouldBeNull()
   }

   @Test
   fun `It should vibrate for notifications that get app paused on insert`() = runTest {
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

      pauseController.becomeAppPausedOnNewCall = true

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }

   @Test
   fun `It should mark app paused notifications as app paused`() = runTest {
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

      pauseController.becomeAppPausedOnNewCall = true

      processor.onNotificationPosted(notification)

      processor.getNotification(1).shouldNotBeNull().apply {
         paused.app shouldBe true

         actions
            .shouldContain(Action.PauseApp("Unpause app"))
            .shouldNotContain(Action.PauseApp("Pause app"))
      }
   }

   @Test
   fun `Refresh app paused status of all notifications of a specific package when notify pause change is called`() = runTest {
      val notificationA = ParsedNotification(
         "keyA",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationB = ParsedNotification(
         "keyB",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationC = ParsedNotification(
         "keyC",
         "another.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notificationA)
      processor.onNotificationPosted(notificationB)
      processor.onNotificationPosted(notificationC)
      runCurrent()

      pauseController.pauseStatuses[notificationA] = PauseStatus(app = true)
      pauseController.pauseStatuses[notificationB] = PauseStatus(app = true)
      processor.notifyPackagePauseStatusChanged("com.app")
      runCurrent()

      processor.getNotification(1).shouldNotBeNull().paused.app shouldBe true
      processor.getNotification(2).shouldNotBeNull().paused.app shouldBe true
      processor.getNotification(3).shouldNotBeNull().paused.app shouldBe false

      processor.getNotification(1)
         .shouldNotBeNull()
         .actions
         .shouldContain(Action.PauseApp("Unpause app"))
         .shouldNotContain(Action.PauseApp("Pause app"))

      processor.getNotification(2)
         .shouldNotBeNull()
         .actions
         .shouldContain(Action.PauseApp("Unpause app"))
         .shouldNotContain(Action.PauseApp("Pause app"))

      processor.getNotification(3)
         .shouldNotBeNull()
         .actions
         .shouldContain(Action.PauseApp("Pause app"))
         .shouldNotContain(Action.PauseApp("Unpause app"))
   }

   @Test
   fun `Re-sync notifications with changed app pause status`() = runTest {
      val notificationA = ParsedNotification(
         "keyA",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationB = ParsedNotification(
         "keyB",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationC = ParsedNotification(
         "keyC",
         "another.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      pauseController.becomeAppPausedOnNewCall = true
      processor.onNotificationPosted(notificationA)
      pauseController.becomeAppPausedOnNewCall = false
      processor.onNotificationPosted(notificationB)
      processor.onNotificationPosted(notificationC)
      runCurrent()

      watchSyncer.syncedNotifications.clear()

      pauseController.pauseStatuses[notificationB] = PauseStatus(app = true)
      processor.notifyPackagePauseStatusChanged("com.app")
      runCurrent()

      watchSyncer.syncedNotifications.map { it.bucketId }.shouldContainExactly(2)
   }

   @Test
   fun `It should not vibrate for notifications that are conversation paused`() = runTest {
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

      pauseController.pauseStatuses[notification] = PauseStatus(conversation = true)

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe false
      processor.pollNextVibration().shouldBeNull()
   }

   @Test
   fun `It should vibrate for notifications that get conversation paused on insert`() = runTest {
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

      pauseController.becomeConversationPausedOnNewCall = true

      processor.onNotificationPosted(notification)

      openController.watchappOpened shouldBe true
      processor.pollNextVibration().shouldNotBeNull()
   }

   @Test
   fun `It should mark conversation paused notifications as conversation paused`() = runTest {
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

      pauseController.becomeConversationPausedOnNewCall = true

      processor.onNotificationPosted(notification)

      processor.getNotification(1).shouldNotBeNull().apply {
         paused.conversation shouldBe true

         actions
            .shouldContain(Action.PauseConversation("Unpause conversation"))
            .shouldNotContain(Action.PauseConversation("Pause conversation"))
      }
   }

   @Test
   fun `Refresh conversation paused status of a specific notification when notify pause change is called`() = runTest {
      val notificationA = ParsedNotification(
         "keyA",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationB = ParsedNotification(
         "keyB",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationC = ParsedNotification(
         "keyC",
         "another.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      processor.onNotificationPosted(notificationA)
      processor.onNotificationPosted(notificationB)
      processor.onNotificationPosted(notificationC)
      runCurrent()

      pauseController.pauseStatuses[notificationA] = PauseStatus(conversation = true)
      pauseController.pauseStatuses[notificationB] = PauseStatus(conversation = true)
      processor.notifyPackagePauseStatusChanged("com.app")
      runCurrent()

      processor.getNotification(1).shouldNotBeNull().paused.conversation shouldBe true
      processor.getNotification(2).shouldNotBeNull().paused.conversation shouldBe true
      processor.getNotification(3).shouldNotBeNull().paused.conversation shouldBe false

      processor.getNotification(1)
         .shouldNotBeNull()
         .actions
         .shouldContain(Action.PauseConversation("Unpause conversation"))
         .shouldNotContain(Action.PauseConversation("Pause conversation"))

      processor.getNotification(2)
         .shouldNotBeNull()
         .actions
         .shouldContain(Action.PauseConversation("Unpause conversation"))
         .shouldNotContain(Action.PauseConversation("Pause conversation"))

      processor.getNotification(3)
         .shouldNotBeNull()
         .actions
         .shouldContain(Action.PauseConversation("Pause conversation"))
         .shouldNotContain(Action.PauseConversation("Unpause conversation"))
   }

   @Test
   fun `Re-sync notifications with changed conversation pause status`() = runTest {
      val notificationA = ParsedNotification(
         "keyA",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationB = ParsedNotification(
         "keyB",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      val notificationC = ParsedNotification(
         "keyC",
         "another.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305)
      )

      pauseController.becomeConversationPausedOnNewCall = true
      processor.onNotificationPosted(notificationA)
      pauseController.becomeConversationPausedOnNewCall = false
      processor.onNotificationPosted(notificationB)
      processor.onNotificationPosted(notificationC)
      runCurrent()

      watchSyncer.syncedNotifications.clear()

      pauseController.pauseStatuses[notificationB] = PauseStatus(conversation = true)
      processor.notifyPackagePauseStatusChanged("com.app")
      runCurrent()

      watchSyncer.syncedNotifications.map { it.bucketId }.shouldContainExactly(2)
   }

   @Test
   fun `It should add App prefix to native actions that clash with the NC actions`() = runTest {
      val intent1 = "intent1"
      val intent2 = "intent2"

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
            NativeAction("Dismiss", intent2),
         )
      )

      processor.onNotificationPosted(notification)

      processor.getNotification(1)?.actions.orEmpty()
         .shouldContainAll(
            Action.Dismiss("Dismiss"),
            Action.Native("Action 1", intent1),
            Action.Native("Dismiss (App)", intent2),
         )
         .shouldNotContain(
            Action.Native("Dismiss", intent2)
         )
   }
}
