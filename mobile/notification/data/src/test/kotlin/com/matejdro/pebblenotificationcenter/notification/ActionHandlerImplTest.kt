package com.matejdro.pebblenotificationcenter.notification

import android.app.createPendingIntent
import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.setTo
import com.matejdro.pebblenotificationcenter.FakeNotificationServiceController
import com.matejdro.pebblenotificationcenter.bluetooth.FakeSubmenuController
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuItem
import com.matejdro.pebblenotificationcenter.bluetooth.SubmenuType
import com.matejdro.pebblenotificationcenter.notification.model.Action
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.model.ProcessedNotification
import com.matejdro.pebblenotificationcenter.submenus.ReplySubmenuPayload
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import si.inova.kotlinova.core.test.fakes.FakeResources
import java.time.Instant

class ActionHandlerImplTest {
   private val repo = FakeNotificationRepository()
   private val servicecontroller = FakeNotificationServiceController()
   private val submenuController = FakeSubmenuController()

   private val resources = FakeResources()

   private val rulesRepository = FakeRulesRepository()

   private val pauseController = FakePauseController()

   private val handler = ActionHandlerImpl(
      repo,
      servicecontroller,
      submenuController,
      RuleResolver(rulesRepository),
      resources,
      pauseController,
   )

   @BeforeEach
   fun setUp() {
      resources.putString(R.string.voice, "- Voice -")
   }

   @Test
   fun `Return false when notification does not exist in the repo`() = runTest {
      insertDefaultRules()

      handler.handleAction(2, 2) shouldBe false
   }

   @Test
   fun `Return false when notification exists but the action does not exist`() = runTest {
      insertDefaultRules()

      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = emptyList()
         ),
      )

      handler.handleAction(2, 2) shouldBe false
   }

   @Test
   fun `Return false when notification and actions exist but the controller fails with dismissal`() = runTest {
      insertDefaultRules()

      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = listOf(
               Action.Dismiss("Dismiss")
            )
         ),
      )

      servicecontroller.returnValue = false

      handler.handleAction(2, 0) shouldBe false
   }

   @Test
   fun `Forward dismiss action to controller`() = runTest {
      insertDefaultRules()

      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "keyNotification",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = listOf(
               Action.Dismiss("Dismiss")
            )
         ),
      )

      servicecontroller.returnValue = true

      handler.handleAction(2, 0) shouldBe true

      servicecontroller.lastCancelledNotification shouldBe "keyNotification"
   }

   @Test
   fun `Forward native action to controller`() = runTest {
      insertDefaultRules()

      val intent = createPendingIntent()

      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "keyNotification",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = listOf(
               Action.Native("Mark as read", intent)
            )
         ),
      )

      servicecontroller.returnValue = true

      handler.handleAction(2, 0) shouldBe true

      servicecontroller.lastTriggeredIntent shouldBeSameInstanceAs intent
   }

   @Test
   fun `Open submenu on the watch with reply and voice options`() = runTest {
      insertDefaultRules()

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.replyCannedTexts setTo emptySet()
      )

      val intent = createPendingIntent()

      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "keyNotification",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = listOf(
               Action.Reply(
                  title = "Mark as read",
                  intent = intent,
                  "ResultKey",
                  listOf("Message A", "Message B", "Message C"),
                  allowFreeFormInput = true
               )
            ),
            bucketId = 2
         ),
      )

      handler.handleAction(2, 0) shouldBe true

      submenuController.sentMenus.toMap().shouldBe(
         mapOf(
            FakeSubmenuController.MenuItemKey(2u, SubmenuType.REPLY_ANSWERS) to listOf(
               SubmenuItem(
                  "- Voice -",
                  ReplySubmenuPayload("", intent, "ResultKey"),
                  voiceInput = true
               ),
               SubmenuItem(
                  "Message A",
                  ReplySubmenuPayload("Message A", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Message B",
                  ReplySubmenuPayload("Message B", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Message C",
                  ReplySubmenuPayload("Message C", intent, "ResultKey")
               )
            )
         )
      )

      servicecontroller.lastTriggeredIntent shouldBe null
   }

   @Test
   @Suppress("LongMethod") // Lots of test examples
   fun `Add user provided canned texts`() = runTest {
      insertDefaultRules()

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.replyCannedTexts setTo setOf("Custom A", "Custom B")
      )

      val intent = createPendingIntent()

      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "keyNotification",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = listOf(
               Action.Reply(
                  title = "Mark as read",
                  intent = intent,
                  "ResultKey",
                  listOf("Message A", "Message B", "Message C"),
                  allowFreeFormInput = true
               )
            ),
            bucketId = 2
         ),
      )

      handler.handleAction(2, 0) shouldBe true

      submenuController.sentMenus.toMap().shouldContainExactly(
         mapOf(
            FakeSubmenuController.MenuItemKey(2u, SubmenuType.REPLY_ANSWERS) to listOf(
               SubmenuItem(
                  "- Voice -",
                  ReplySubmenuPayload("", intent, "ResultKey"),
                  voiceInput = true
               ),
               SubmenuItem(
                  "Custom A",
                  ReplySubmenuPayload("Custom A", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Custom B",
                  ReplySubmenuPayload("Custom B", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Message A",
                  ReplySubmenuPayload("Message A", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Message B",
                  ReplySubmenuPayload("Message B", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Message C",
                  ReplySubmenuPayload("Message C", intent, "ResultKey")
               )
            )
         )
      )

      servicecontroller.lastTriggeredIntent shouldBe null
   }

   @Test
   fun `Disable user provided and voice entries when app does not allow freeform`() = runTest {
      insertDefaultRules()

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.replyCannedTexts setTo setOf("Custom A", "Custom B")
      )

      val intent = createPendingIntent()

      repo.putNotification(
         2,
         ProcessedNotification(
            ParsedNotification(
               "keyNotification",
               "",
               "",
               "",
               "Hello",
               Instant.MIN,
            ),
            actions = listOf(
               Action.Reply(
                  title = "Mark as read",
                  intent = intent,
                  "ResultKey",
                  listOf("Message A", "Message B", "Message C"),
                  allowFreeFormInput = false
               )
            ),
            bucketId = 2
         ),
      )

      handler.handleAction(2, 0) shouldBe true

      submenuController.sentMenus.toMap().shouldContainExactly(
         mapOf(
            FakeSubmenuController.MenuItemKey(2u, SubmenuType.REPLY_ANSWERS) to listOf(
               SubmenuItem(
                  "Message A",
                  ReplySubmenuPayload("Message A", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Message B",
                  ReplySubmenuPayload("Message B", intent, "ResultKey")
               ),
               SubmenuItem(
                  "Message C",
                  ReplySubmenuPayload("Message C", intent, "ResultKey")
               )
            )
         )
      )

      servicecontroller.lastTriggeredIntent shouldBe null
   }

   @Test
   fun `Toggle app pause action`() = runTest {
      insertDefaultRules()

      val notification = ParsedNotification(
         "keyNotification",
         "",
         "",
         "",
         "Hello",
         Instant.MIN,
      )
      repo.putNotification(
         2,
         ProcessedNotification(
            notification,
            actions = listOf(
               Action.PauseApp("Toggle app pause")
            )
         ),
      )

      servicecontroller.returnValue = true

      handler.handleAction(2, 0) shouldBe true

      pauseController.toggledNotifications.shouldContainExactly(notification)
   }

   private suspend fun insertDefaultRules() {
      rulesRepository.insert("Default Rule")
   }
}
