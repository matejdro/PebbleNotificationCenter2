package com.matejdro.pebblenotificationcenter.tasker

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.os.bundleOf
import com.matejdro.notificationcenter.tasker.R
import com.matejdro.pebblenotificationcenter.common.NotificationsKeys
import com.matejdro.pebblenotificationcenter.common.di.NavigationInjectingApplication
import dev.zacsweers.metro.Inject
import dispatch.core.MainImmediateCoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import logcat.logcat
import net.dinglisch.android.tasker.TaskerPlugin
import si.inova.kotlinova.core.reporting.ErrorReporter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class TaskerActionService : Service() {
   @Inject
   lateinit var taskerRunner: TaskerActionRunner

   @Inject
   lateinit var errorReporter: ErrorReporter

   @Inject
   lateinit var coroutineScope: MainImmediateCoroutineScope

   private val runningTasks = AtomicInteger(0)

   private val binder by lazy { Binder() }

   override fun onCreate() {
      applicationContext!!
         .let { it as NavigationInjectingApplication }
         .applicationGraph
         .let { it as TaskerServiceInjector }
         .inject(this)

      super.onCreate()
   }

   @Suppress("SuspendFunSwallowedCancellation") // CancellationException is re-thrown after signalFinish
   override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
      val canBind = intent.getBooleanExtra(TaskerPluginConstants.EXTRA_CAN_BIND_FIRE_SETTING, false)
      logcat { "Starting TaskerActionService. Bound: $canBind" }
      if (!canBind) {
         startForeground()
      }

      runningTasks.incrementAndGet()

      coroutineScope.launch {
         try {
            taskerRunner.run(intent.extras ?: Bundle())
            logcat { "Run finished" }

            TaskerPlugin.Setting.signalFinish(
               this@TaskerActionService,
               intent,
               TaskerPluginConstants.RESULT_CODE_OK,
               Bundle()
            )
         } catch (e: CancellationException) {
            TaskerPlugin.Setting.signalFinish(
               this@TaskerActionService,
               intent,
               TaskerPluginConstants.RESULT_CODE_FAILED,
               bundleOf("%err" to "1", "%errmsg" to "Cancelled")
            )
            throw e
         } catch (e: Exception) {
            errorReporter.report(e)
            TaskerPlugin.Setting.signalFinish(
               this@TaskerActionService,
               intent,
               TaskerPluginConstants.RESULT_CODE_FAILED,
               bundleOf("%err" to "1", "%errmsg" to e.message)
            )
         } finally {
            val leftTasks = runningTasks.decrementAndGet()
            if (leftTasks == 0) {
               logcat { "Stopping service" }
               stopSelf()
            }
         }
      }

      return super.onStartCommand(intent, flags, startId)
   }

   override fun onDestroy() {
      coroutineScope.cancel()
   }

   override fun onBind(intent: Intent?): IBinder? {
      return binder
   }

   private fun startForeground() {
      val notification = NotificationCompat.Builder(this, NotificationsKeys.CHANNEL_ID_TASKER_SERVICE)
         .setContentTitle(
            getString(com.matejdro.pebblenotificationcenter.sharedresources.R.string.app_name)
         )
         .setContentText(getString(R.string.running_tasker_action))
         .setSmallIcon(com.matejdro.pebblenotificationcenter.sharedresources.R.drawable.ic_launcher)
         .build()

      ServiceCompat.startForeground(
         this,
         NotificationsKeys.NOTIFICATION_ID_TASKER_SERVICE,
         notification,
         FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      )
   }
}

/**
 * copy of the [ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE] for compat reasons
 */
private const val FOREGROUND_SERVICE_TYPE_SPECIAL_USE = 1 shl 30
