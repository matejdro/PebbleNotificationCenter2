package com.matejdro.pebblenotificationcenter.notification

import android.os.Build
import android.os.Process
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.matejdro.pebblenotificationcenter.common.di.NavigationInjectingApplication
import com.matejdro.pebblenotificationcenter.notification.di.NotificationInject
import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification
import com.matejdro.pebblenotificationcenter.notification.parsing.NotificationParser
import com.matejdro.pebblenotificationcenter.rules.GlobalPreferenceKeys
import com.matejdro.pebblenotificationcenter.rules.keys.get
import dev.zacsweers.metro.Inject
import dispatch.core.DefaultCoroutineScope
import io.rebble.pebblekit2.client.PebbleInfoRetriever
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.logcat
import si.inova.kotlinova.core.reporting.ErrorReporter

class NotificationService : NotificationListenerService() {
   @Inject
   private lateinit var notificationProcessor: NotificationProcessor

   @Inject
   private lateinit var notificationParser: NotificationParser

   @Inject
   private lateinit var coroutineScope: DefaultCoroutineScope

   @Inject
   private lateinit var pebbleInfoRetriever: PebbleInfoRetriever

   @Inject
   private lateinit var errorReporter: ErrorReporter

   @Inject
   private lateinit var preferenceStore: DataStore<Preferences>

   private val mutex = Mutex()

   private var bound = false

   override fun onCreate() {
      logcat { "Starting notification service" }
      (application!! as NavigationInjectingApplication)
         .applicationGraph
         .let { it as NotificationInject }
         .inject(this)

      instance = this

      controlListenerHints()

      super.onCreate()
   }

   override fun onDestroy() {
      logcat { "Stopping notification service" }
      instance = null
      bound = false
      super.onDestroy()
   }

   override fun onListenerConnected() {
      super.onListenerConnected()

      if (bound) {
         // Prevent duplicate calls
         return
      }
      bound = true

      coroutineScope.launch {
         mutex.withLock {
            notificationProcessor.onNotificationsCleared()
            for (sbn in activeNotifications) {
               val parsed = parseNotification(sbn)
               if (parsed != null) {
                  notificationProcessor.onNotificationPosted(parsed, suppressVibration = true)
               } else {
                  logcat { "Notification ${sbn.key} has no text. Skipping..." }
               }
            }
         }
      }
   }

   override fun onNotificationPosted(sbn: StatusBarNotification) {
      logcat { "Notification ${sbn.key} posted" }
      coroutineScope.launch {
         mutex.withLock {
            val parsed = parseNotification(sbn)
            if (parsed == null) {
               logcat { "Notification ${sbn.key} has no text. Skipping..." }
               return@launch
            }
            notificationProcessor.onNotificationPosted(parsed)
         }
      }
   }

   private fun parseNotification(sbn: StatusBarNotification): ParsedNotification? {
      val ranking = Ranking()
      currentRanking.getRanking(sbn.key, ranking)

      return notificationParser.parse(sbn, getNotificationChannel(sbn), ranking)
   }

   override fun onNotificationRemoved(sbn: StatusBarNotification) {
      logcat { "Notification ${sbn.key} removed" }

      coroutineScope.launch {
         mutex.withLock {
            notificationProcessor.onNotificationDismissed(sbn.key)
         }
      }
   }

   private fun getNotificationChannel(sbn: StatusBarNotification): Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      getNotificationChannels(sbn.packageName, Process.myUserHandle()).firstOrNull {
         it.id == sbn.notification.channelId
      }
   } else {
      null
   }

   private fun controlListenerHints() = coroutineScope.launch {
      val anyWatchConnected = pebbleInfoRetriever.getConnectedWatches().map { it.isNotEmpty() }.distinctUntilChanged()
         .onEach { logcat { "Watch connected: $it" } }

      val mutePhoneFlow = preferenceStore.data.map { preferences ->
         preferences[GlobalPreferenceKeys.mutePhone]
      }.distinctUntilChanged()

      mutePhoneFlow.flatMapLatest { mutePhone ->
         if (mutePhone) {
            anyWatchConnected.map { connected ->
               var listenerHints = 0
               if (connected) {
                  listenerHints = listenerHints or HINT_HOST_DISABLE_NOTIFICATION_EFFECTS
               }

               listenerHints
            }.distinctUntilChanged()
         } else {
            flowOf(0)
         }
      }
         .collect { listenerHints ->
            try {
               requestListenerHints(listenerHints)
            } catch (e: SecurityException) {
               errorReporter.report(e)
            }
         }
   }

   companion object {
      internal var instance: NotificationService? = null
   }
}
