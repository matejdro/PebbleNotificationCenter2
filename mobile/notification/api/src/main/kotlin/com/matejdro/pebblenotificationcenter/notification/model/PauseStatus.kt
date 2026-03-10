package com.matejdro.pebblenotificationcenter.notification.model

data class PauseStatus(val app: Boolean = false, val conversation: Boolean = false)

val PauseStatus.any get() = app || conversation
