package com.matejdro.pebblenotificationcenter.tools.ui.settings

import com.matejdro.pebblenotificationcenter.rules.RuleMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingsCodecTest {
   @Test
   fun `Round trip preserves IDs and names including special characters`() {
      val rules = listOf(
         RuleMetadata(1, "Default Settings"),
         RuleMetadata(2, "Telegram, with \"quotes\""),
         RuleMetadata(3, "àèìòù émoji"),
         RuleMetadata(4, "line1\nline2"),
      )

      val encoded = SettingsCodec.encodeRules(rules)
      val decoded = SettingsCodec.decodeRules(encoded)

      decoded.map { it.id } shouldBe listOf(1, 2, 3, 4)
      decoded.map { it.name } shouldBe rules.map { it.name }
   }

   @Test
   fun `Decode rejects unsupported versions`() {
      shouldThrow<IllegalArgumentException> {
         SettingsCodec.decodeRules("""{"version": 2, "rules": []}""")
      }

      shouldThrow<IllegalArgumentException> {
         SettingsCodec.decodeRules("""{"version": 0, "rules": []}""")
      }
   }

   @Test
   fun `Decode ignores unknown top-level fields`() {
      val decoded = SettingsCodec.decodeRules("""{"version": 1, "rules": [], "futureField": "x"}""")

      decoded.shouldBeEmpty()
   }

   @Test
   fun `Classify known entries`() {
      SettingsCodec.classifyEntry("rules.json") shouldBe Entry.RulesJson
      SettingsCodec.classifyEntry("preferences.preferences_pb") shouldBe Entry.GlobalDataStore
      SettingsCodec.classifyEntry("12.preferences_pb") shouldBe Entry.RuleDataStore(12)
      SettingsCodec.classifyEntry("42.preferences_pb") shouldBe Entry.RuleDataStore(42)
      SettingsCodec.classifyEntry("not_a_number.preferences_pb") shouldBe Entry.Ignored
      SettingsCodec.classifyEntry("1.2.preferences_pb") shouldBe Entry.Ignored
      SettingsCodec.classifyEntry("some_other_file.txt") shouldBe Entry.Ignored
   }

   @Test
   fun `Classify rejects path traversal names`() {
      shouldThrow<IllegalArgumentException> {
         SettingsCodec.classifyEntry("rules.json/bk")
      }

      shouldThrow<IllegalArgumentException> {
         SettingsCodec.classifyEntry("..")
      }

      shouldThrow<IllegalArgumentException> {
         SettingsCodec.classifyEntry("a\\b.preferences_pb")
      }
   }
}
