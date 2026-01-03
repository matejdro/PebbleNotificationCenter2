package com.matejdro.pebblenotificationcenter.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import si.inova.kotlinova.core.reporting.ErrorReporter

class ErrorReportingKermitWriter(
   private val errorReporter: ErrorReporter,
) : LogWriter() {
   override fun log(
      severity: Severity,
      message: String,
      tag: String,
      throwable: Throwable?,
   ) {
      if (throwable != null) {
         errorReporter.report(throwable)
      }
   }
}
