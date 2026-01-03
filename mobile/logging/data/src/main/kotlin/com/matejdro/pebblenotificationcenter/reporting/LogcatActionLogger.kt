package com.matejdro.pebblenotificationcenter.reporting

import com.matejdro.pebblenotificationcenter.common.logging.ActionLogger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import logcat.logcat

@ContributesBinding(AppScope::class)
class LogcatActionLogger @Inject constructor() : ActionLogger {
   override fun logAction(text: () -> String) {
      logcat(message = text, tag = "UserAction")
   }
}
