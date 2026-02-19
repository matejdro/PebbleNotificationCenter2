package com.matejdro.pebblenotificationcenter.notification

import androidx.datastore.preferences.core.Preferences
import com.matejdro.notificationcenter.rules.RULE_ID_DEFAULT_SETTINGS
import com.matejdro.notificationcenter.rules.RuleOption
import com.matejdro.notificationcenter.rules.RulesRepository
import com.matejdro.notificationcenter.rules.keys.get
import com.matejdro.pebblenotificationcenter.common.preferences.plus
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import si.inova.kotlinova.core.outcome.Outcome

@Inject
class RuleResolver(private val rulesRepository: RulesRepository) {
   suspend fun resolveRules(notification: ParsedNotification): ResolvedRules {
      val rules = rulesRepository.getAll().firstSuccessOrThrow()

      val matchingRules = rules.mapNotNull { rule ->
         val preferences = rulesRepository.getRulePreferences(rule.id).first()

         val nameIfNotDefault = rule.name.takeIf { rule.id != RULE_ID_DEFAULT_SETTINGS }

         (nameIfNotDefault to preferences).takeIf { rule.id == RULE_ID_DEFAULT_SETTINGS || preferences.matches(notification) }
      }

      return ResolvedRules(
         matchingRules.mapNotNull { (name, _) -> name },
         matchingRules.map { (_, preferences) -> preferences }.reduce(Preferences::plus)
      )
   }

   private fun Preferences.matches(notification: ParsedNotification): Boolean {
      val conditionPkg = this[RuleOption.conditionAppPackage]
      if (conditionPkg != null && conditionPkg != notification.pkg) {
         return false
      }

      val conditionChannels = this[RuleOption.conditionNotificationChannels]
      if (conditionChannels.isNotEmpty() && !conditionChannels.contains(notification.channel)) {
         return false
      }

      val whitelistRegexes = this[RuleOption.conditionWhitelistRegexes].map { Regex(it) }
      if (whitelistRegexes.isNotEmpty() && !whitelistRegexes.all(notification::containsRegex)) {
         return false
      }

      val blacklistRegexes = this[RuleOption.conditionBlacklistRegexes].map { Regex(it) }
      if (blacklistRegexes.any(notification::containsRegex)) {
         return false
      }

      return true
   }
}

data class ResolvedRules(
   val involvedRules: List<String>,
   val preferences: Preferences,
)

private fun ParsedNotification.containsRegex(regex: Regex): Boolean {
   return regex.containsMatchIn(title) || regex.containsMatchIn(subtitle) || regex.containsMatchIn(body)
}

private suspend fun <T> Flow<Outcome<T>>.firstSuccessOrThrow(): T {
   val result = first {
      it is Outcome.Success || it is Outcome.Error
   }

   return when (result) {
      is Outcome.Success -> result.data
      is Outcome.Error -> throw result.exception
      is Outcome.Progress -> error("Result should never be progress")
   }
}
