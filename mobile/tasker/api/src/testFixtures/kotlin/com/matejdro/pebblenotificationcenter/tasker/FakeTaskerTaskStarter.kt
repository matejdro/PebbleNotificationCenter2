package com.matejdro.pebblenotificationcenter.tasker

import com.matejdro.pebblenotificationcenter.notification.model.ParsedNotification

class FakeTaskerTaskStarter : TaskerTaskStarter {
   val startedTasks = mutableListOf<String>()
   var reportStartSuccessful = true

   override fun startTask(task: String, notification: ParsedNotification): Boolean {
      startedTasks.add(task)

      return reportStartSuccessful
   }
}
