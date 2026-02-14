package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.notificationcenter.rules.FakeRulesRepository
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.notificationcenter.rules.keys.setTo
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

class RuleResolverTest {
   private val rulesRepository = FakeRulesRepository()

   private val resolver = RuleResolver(rulesRepository)

   @Test
   fun `When no other rules are available, use default settings`() = runTest {
      rulesRepository.insert("Default Rule")
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
         channel = "test_channel"
      )

      val resolvedRules = resolver.resolveRules(notification)
      resolvedRules.involvedRules.shouldBeEmpty()
      resolvedRules.preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.MUTE
   }

   @Test
   fun `Apply override when a rule's app and channel preference is empty`() = runTest {
      rulesRepository.insert("Default Rule")
      rulesRepository.insert("Rule B")

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.SHOW
      )
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo null,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE
      )

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         channel = "test_channel"
      )

      val resolvedRules = resolver.resolveRules(notification)
      resolvedRules.involvedRules.shouldContainExactly("Rule B")
      resolvedRules.preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.HIDE
   }

   @Test
   fun `Apply override when a rule's app matches and channel preference is empty`() = runTest {
      rulesRepository.insert("Default Rule")
      rulesRepository.insert("Rule B")

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.SHOW
      )
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "com.app",
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
         channel = "test_channel"
      )

      val resolvedRules = resolver.resolveRules(notification)
      resolvedRules.involvedRules.shouldContainExactly("Rule B")
      resolvedRules.preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.MUTE
   }

   @Test
   fun `Do not apply override when a rule's app does not match`() = runTest {
      rulesRepository.insert("Default Rule")
      rulesRepository.insert("Rule B")

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE
      )
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "com.app2",
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
         channel = "test_channel"
      )

      val resolvedRules = resolver.resolveRules(notification)
      resolvedRules.involvedRules.shouldBeEmpty()
      resolvedRules.preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.HIDE
   }

   @Test
   fun `Apply override when a rule's app and channel matches`() = runTest {
      rulesRepository.insert("Default Rule")
      rulesRepository.insert("Rule B")

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.SHOW
      )
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "com.app",
         RuleOption.conditionNotificationChannels setTo setOf("channel_2", "test_channel"),
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
         channel = "test_channel"
      )

      val resolvedRules = resolver.resolveRules(notification)
      resolvedRules.involvedRules.shouldContainExactly("Rule B")
      resolvedRules.preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.MUTE
   }

   @Test
   fun `Apply override when a rule's app matches but channel does not`() = runTest {
      rulesRepository.insert("Default Rule")
      rulesRepository.insert("Rule B")

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE
      )
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.conditionAppPackage setTo "com.app",
         RuleOption.conditionNotificationChannels setTo setOf("channel_2", "channel_3"),
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
         channel = "test_channel"
      )

      val resolvedRules = resolver.resolveRules(notification)
      resolvedRules.involvedRules.shouldBeEmpty()
      resolvedRules.preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.HIDE
   }

   @Test
   fun `When multiple rules match, select last rule based on the user's order`() = runTest {
      rulesRepository.insert("Default Rule")
      rulesRepository.insert("Rule B")
      rulesRepository.insert("Rule C")

      rulesRepository.updateRulePreferences(
         RULE_ID_DEFAULT_SETTINGS,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.SHOW
      )
      rulesRepository.updateRulePreferences(
         2,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.HIDE
      )
      rulesRepository.updateRulePreferences(
         3,
         RuleOption.masterSwitch setTo RuleOption.MasterSwitch.MUTE
      )

      rulesRepository.reorder(3, 1)

      val notification = ParsedNotification(
         "key",
         "com.app",
         "Title",
         "sTitle",
         "Body",
         // 19:18:25 GMT | Sunday, January 4, 2026
         Instant.ofEpochSecond(1_767_554_305),
         channel = "test_channel"
      )

      val resolvedRules = resolver.resolveRules(notification)
      resolvedRules.involvedRules.shouldContainExactly("Rule C", "Rule B")
      resolvedRules.preferences[RuleOption.masterSwitch] shouldBe RuleOption.MasterSwitch.HIDE
   }
}
