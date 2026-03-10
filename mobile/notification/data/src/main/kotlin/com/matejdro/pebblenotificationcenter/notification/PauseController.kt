package com.matejdro.pebblenotificationcenter.notification

import androidx.datastore.preferences.core.Preferences
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.SingleIn
import logcat.logcat
import java.util.concurrent.ConcurrentHashMap

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class PauseControllerImpl(
   private val repo: Provider<NotificationRepository>,
) : PauseController {
   // Use Map because Java has no ConcurrentHashSet
   private val mutedApps = ConcurrentHashMap<String, Unit>()

   override fun onNewNotification(
      notification: ParsedNotification,
      preferences: Preferences,
   ) {
   }

   override suspend fun onNotificationDismissed(notification: ParsedNotification) {
      val existingNotificationsFromThisPkg = repo().getAllActiveNotifications().filter { it.systemData.pkg == notification.pkg }
      if (existingNotificationsFromThisPkg.isEmpty()) {
         mutedApps.remove(notification.pkg)
         repo().notifyPackagePauseStatusChanged(notification.pkg)
      }
   }

   override fun isNotificationPaused(notification: ParsedNotification): Boolean {
      return mutedApps.containsKey(notification.pkg)
   }

   override suspend fun toggleAppPause(notification: ParsedNotification) {
      if (mutedApps.containsKey(notification.pkg)) {
         logcat { "Package ${notification.pkg} is not paused anymore" }
         mutedApps.remove(notification.pkg)
      } else {
         logcat { "Package ${notification.pkg} is now paused" }
         mutedApps[notification.pkg] = Unit
      }

      repo().notifyPackagePauseStatusChanged(notification.pkg)
   }
}

interface PauseController {
   fun onNewNotification(notification: ParsedNotification, preferences: Preferences)

   suspend fun onNotificationDismissed(notification: ParsedNotification)

   fun isNotificationPaused(notification: ParsedNotification): Boolean

   suspend fun toggleAppPause(notification: ParsedNotification)
}
