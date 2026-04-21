package com.matejdro.pebblenotificationcenter.tasker

interface TaskerServiceInjector {
   fun inject(taskerActionService: TaskerActionService)
   fun inject(legacyTaskerReceiver: LegacyTaskerReceiver)
}
