package com.matejdro.pebblenotificationcenter.di

import android.content.Context
import com.matejdro.pebblenotificationcenter.BuildConfig
import com.matejdro.pebblenotificationcenter.common.exceptions.CrashOnDebugException
import com.matejdro.pebblenotificationcenter.crashreport.CrashReportService
import com.matejdro.pebblenotificationcenter.logging.TinyLogLoggingThread
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import org.tinylog.Level
import si.inova.kotlinova.core.exceptions.UnknownCauseException
import si.inova.kotlinova.core.outcome.CauseException
import si.inova.kotlinova.core.reporting.ErrorReporter

@Suppress("unused")
@ContributesTo(AppScope::class)
interface ErrorReportingProviders {
   @Provides
   fun provideErrorReporter(tinyLogLoggingThread: TinyLogLoggingThread, context: Context): ErrorReporter {
      return object : ErrorReporter {
         override fun report(throwable: Throwable) {
            if (throwable !is CauseException) {
               report(UnknownCauseException("Got reported non-cause exception", throwable))
               return
            }

            if (throwable.shouldReport) {
               throwable.printStackTrace()
               tinyLogLoggingThread.log(1, "ErrorReporter", Level.ERROR, null, throwable)
               CrashReportService.showCrashNotification(throwable.stackTraceToString(), context)
            } else if (BuildConfig.DEBUG) {
               if (throwable is CrashOnDebugException) {
                  throw throwable
               }
               throwable.printStackTrace()
            }
         }
      }
   }
}
