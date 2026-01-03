package com.matejdro.pebblenotificationcenter

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.os.strictmode.Violation
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.matejdro.pebblenotificationcenter.common.di.NavigationInjectingApplication
import com.matejdro.pebblenotificationcenter.di.ApplicationGraph
import com.matejdro.pebblenotificationcenter.di.MainApplicationGraph
import com.matejdro.pebblenotificationcenter.logging.ErrorReportingKermitWriter
import com.matejdro.pebblenotificationcenter.logging.TinyLogKermitWriter
import com.matejdro.pebblenotificationcenter.logging.TinyLogLogcatLogger
import com.matejdro.pebblenotificationcenter.notifications.NotificationChannelManager
import dev.zacsweers.metro.createGraphFactory
import dispatch.core.DefaultDispatcherProvider
import dispatch.core.defaultDispatcher
import kotlinx.coroutines.launch
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import si.inova.kotlinova.core.dispatchers.AccessCallbackDispatcherProvider
import java.io.File
import co.touchlab.kermit.Logger as KermitLogger

open class NotificationCenterApplication : Application(), NavigationInjectingApplication {
   override val applicationGraph: ApplicationGraph by lazy {
      createGraphFactory<MainApplicationGraph.Factory>().create(this)
   }

   init {
      if (BuildConfig.DEBUG) {
         // Enable better coroutine stack traces on debug builds
         // this slows down coroutines, so it should not be enabled on release
         // using init instead of onCreate ensures that this is started before any content providers
         System.setProperty("kotlinx.coroutines.debug", "on")
      }
   }

   override fun onCreate() {
      super.onCreate()

      if (!isMainProcess()) {
         // Do not perform any initialisation in other processes, they are usually library-specific
         return
      }

      DefaultDispatcherProvider.set(
         AccessCallbackDispatcherProvider(DefaultDispatcherProvider.get()) {
            if (BuildConfig.DEBUG) {
               error("Dispatchers not provided via coroutine scope.")
            }
         }
      )

      SingletonImageLoader.setSafe {
         ImageLoader.Builder(this)
            // Load Coil cache on the background thread
            // See https://github.com/coil-kt/coil/issues/1878
            .interceptorCoroutineContext(applicationGraph.getDefaultCoroutineScope().defaultDispatcher)
            .build()
      }

      setupLogging()
      enableStrictMode()

      NotificationChannelManager(this).createChannels()

      WorkManager.initialize(
         this,
         Configuration.Builder()
            .setWorkerFactory(applicationGraph.getWorkerFactory())
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerCoroutineContext(applicationGraph.getDefaultCoroutineScope().coroutineContext)
            .build()
      )

      applicationGraph.getDefaultCoroutineScope().launch {
         applicationGraph.getWatchSyncer().init()
         applicationGraph.getSyncNotifier().notifyAppStarted()
      }
   }

   private fun enableStrictMode() {
      // Also check on staging release build, if applicable
      // penaltyListener only supports P and newer, so we are forced to only enable StrictMode on those devices
      if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
         return
      }

      StrictMode.setVmPolicy(
         VmPolicy.Builder()
            .detectActivityLeaks()
            .detectContentUriWithoutPermission()
            .detectFileUriExposure()
            .detectLeakedClosableObjects()
            .detectLeakedRegistrationObjects()
            .detectLeakedSqlLiteObjects()
            .run {
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  detectCredentialProtectedWhileLocked()
                     .detectImplicitDirectBoot()
               } else {
                  this
               }
            }
            .run {
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  detectUnsafeIntentLaunch()
               } else {
                  this
               }
            }
            .run {
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                  detectBlockedBackgroundActivityLaunch()
               } else {
                  this
               }
            }

            .penaltyListener(ContextCompat.getMainExecutor(this@NotificationCenterApplication)) { e ->
               reportStrictModePenalty(e)
            }
            .build()
      )

      StrictMode.setThreadPolicy(
         StrictMode.ThreadPolicy.Builder()
            .detectCustomSlowCalls()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .detectResourceMismatches()
            .detectUnbufferedIo()
            .penaltyListener(ContextCompat.getMainExecutor(this)) { e ->
               if (BuildConfig.DEBUG) {
                  throw e
               } else {
                  applicationGraph.getErrorReporter().report(e)
               }
            }
            .build()
      )
   }

   private fun setupLogging() {
      // Logging situation with this app is a bit complicated:
      // logcat (the library) - used in the app part to log
      // Kermit - used in the PebbleKit2 to log
      // Tinylog - used to create a persistent rolling file log
      // Both logcat and Kermit log into the Android's Logcat log, here we just need to wire them to also log
      // into Tinylog

      val directoryForLogs: File = applicationGraph.getFileLoggingController().getLogFolder()
         .also { it.mkdirs() }
      System.setProperty("tinylog.directory", directoryForLogs.getAbsolutePath())

      val loggingThread = applicationGraph.getTinyLogLoggingThread()

      if (BuildConfig.DEBUG) {
         LogcatLogger.loggers += AndroidLogcatLogger(minPriority = LogPriority.VERBOSE)
      }
      LogcatLogger.loggers += TinyLogLogcatLogger(loggingThread)
      LogcatLogger.install()

      KermitLogger.addLogWriter(TinyLogKermitWriter(loggingThread))
      KermitLogger.addLogWriter(ErrorReportingKermitWriter(applicationGraph.getErrorReporter()))
   }

   private fun reportStrictModePenalty(violation: Violation) {
      val e = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
         violation
      } else {
         IllegalStateException("Strict mode violation: $violation")
      }

      if (
         e.cause == null &&
         (
            STRICT_MODE_EXCLUSIONS.any {
               e.toString().contains(it)
            } ||
               e.stackTrace.any { stackTraceElement ->
                  STRICT_MODE_EXCLUSIONS.any {
                     stackTraceElement.toString().contains(it)
                  }
               }
            )
      ) {
         // Exclude some classes from strict mode, see STRICT_MODE_EXCLUSIONS below.
         return
      }

      if (BuildConfig.DEBUG) {
         throw e
      } else {
         applicationGraph.getErrorReporter().report(e)
      }
   }

   private fun isMainProcess(): Boolean {
      val activityManager = getSystemService<ActivityManager>()!!
      val myPid = android.os.Process.myPid()

      return activityManager.runningAppProcesses?.any {
         it.pid == myPid && packageName == it.processName
      } == true
   }
}

private val STRICT_MODE_EXCLUSIONS = listOf(
   "UnixSecureDirectoryStream", // https://issuetracker.google.com/issues/270704908
   "UnixDirectoryStream", // https://issuetracker.google.com/issues/270704908,
   "SurfaceControl.finalize", // https://issuetracker.google.com/issues/167533582
   "InsetsSourceControl", // https://issuetracker.google.com/issues/307473789
)
