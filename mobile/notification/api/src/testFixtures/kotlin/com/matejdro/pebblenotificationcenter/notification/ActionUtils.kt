package com.matejdro.pebblenotificationcenter.notification

import com.matejdro.pebblenotificationcenter.notification.model.Action

fun Action.zeroId(): Action {
   return when (this) {
      is Action.Dismiss -> copy(id = 0u)
      is Action.Native -> copy(id = 0u)
      is Action.PauseApp -> copy(id = 0u)
      is Action.PauseConversation -> copy(id = 0u)
      is Action.Reply -> copy(id = 0u)
      is Action.Snooze -> copy(id = 0u)
      is Action.ShowImage -> copy(id = 0u)
      is Action.TaskerTask -> copy(id = 0u)
   }
}

fun List<Action>.zeroIds() = map { it.zeroId() }
