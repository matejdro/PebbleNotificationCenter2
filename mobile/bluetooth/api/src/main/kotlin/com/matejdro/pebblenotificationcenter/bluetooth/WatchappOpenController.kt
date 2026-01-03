package com.matejdro.pebblenotificationcenter.bluetooth

interface WatchappOpenController {
   fun isNextWatchappOpenForAutoSync(): Boolean
   fun setNextWatchappOpenForAutoSync()
   fun resetNextWatchappOpen()
}
