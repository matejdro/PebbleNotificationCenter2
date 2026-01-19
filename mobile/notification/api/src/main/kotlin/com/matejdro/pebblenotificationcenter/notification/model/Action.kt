package com.matejdro.pebblenotificationcenter.notification.model

sealed class Action {
   abstract val title: String

   data class Dismiss(override val title: String) : Action()
}
