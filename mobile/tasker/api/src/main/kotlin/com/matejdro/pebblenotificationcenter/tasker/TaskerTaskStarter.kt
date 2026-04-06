package com.matejdro.pebblenotificationcenter.tasker

import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification

interface TaskerTaskStarter {
   fun startTask(task: String, notification: ParsedNotification): Boolean
}
