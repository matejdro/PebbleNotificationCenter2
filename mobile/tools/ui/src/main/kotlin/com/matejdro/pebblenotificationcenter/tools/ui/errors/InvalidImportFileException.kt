package com.matejdro.pebblenotificationcenter.tools.ui.errors

import si.inova.kotlinova.core.outcome.CauseException

class InvalidImportFileException(
   message: String = "Not a valid settings export",
   cause: Throwable? = null,
) : CauseException(message, cause, isProgrammersFault = false)
