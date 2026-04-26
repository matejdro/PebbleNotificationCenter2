package com.matejdro.pebblenotificationcenter.notification

internal fun replaceRegexes(input: String, replacements: Collection<Pair<String, String>>): String {
   if (replacements.isEmpty()) {
      return input
   }

   return replacements.fold(input) { text, (from, to) ->
      Regex(from).replace(text, to)
   }
      .replace("\\n", "\n")
      .replace("\\t", "\t")
      .replace("\\r", "\r")
}
