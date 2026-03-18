package com.matejdro.pebblenotificationcenter.rules.ui.errors

import si.inova.kotlinova.core.outcome.CauseException

class RuleMissingException : CauseException(isProgrammersFault = false)
