package com.matejdro.pebblenotificationcenter.tools.ui.settings

import com.matejdro.pebblenotificationcenter.rules.GLOBAL_PREFERENCES_DATASTORE_NAME
import com.matejdro.pebblenotificationcenter.rules.RuleMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val FORMAT_VERSION = 1

const val RULES_JSON_NAME = "rules.json"

const val GLOBAL_DATASTORE_FILE_NAME = GLOBAL_PREFERENCES_DATASTORE_NAME + ".preferences_pb"

private const val RULE_DATASTORE_FILE_SUFFIX = ".preferences_pb"

@Serializable
data class ExportedRuleJson(
   val id: Int,
   val name: String,
)

@Serializable
data class SettingsExportJson(
   val version: Int = FORMAT_VERSION,
   val rules: List<ExportedRuleJson> = emptyList(),
)

sealed interface Entry {
   object RulesJson : Entry

   object GlobalDataStore : Entry

   data class RuleDataStore(val ruleId: Int) : Entry

   object Ignored : Entry
}

object SettingsCodec {
   val json = Json {
      prettyPrint = true
      ignoreUnknownKeys = true
   }

   fun encodeRules(rules: List<RuleMetadata>): String {
      return json.encodeToString(SettingsExportJson(FORMAT_VERSION, rules.map { ExportedRuleJson(it.id, it.name) }))
   }

   fun decodeRules(text: String): List<ExportedRuleJson> {
      val export = json.decodeFromString<SettingsExportJson>(text)
      require(export.version == FORMAT_VERSION) { "Unsupported settings export version: ${export.version}" }

      return export.rules
   }

   fun classifyEntry(name: String): Entry {
      require(!name.contains('/') && !name.contains('\\') && name != "..") { "Invalid settings export entry name: $name" }

      if (name == RULES_JSON_NAME) {
         return Entry.RulesJson
      }

      if (name == GLOBAL_DATASTORE_FILE_NAME) {
         return Entry.GlobalDataStore
      }

      val ruleId = name.removeSuffix(RULE_DATASTORE_FILE_SUFFIX).toIntOrNull()
      if (ruleId != null && ruleId > 0) {
         return Entry.RuleDataStore(ruleId)
      }

      return Entry.Ignored
   }
}
