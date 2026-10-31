package com.matejdro.pebblenotificationcenter.tools.ui.settings

import android.os.Process
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

fun interface ProcessKiller {
   fun kill()
}

@Inject
@ContributesBinding(AppScope::class)
class AndroidProcessKiller : ProcessKiller {
   override fun kill() {
      Process.killProcess(Process.myPid())
   }
}
