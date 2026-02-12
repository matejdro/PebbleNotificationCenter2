package com.matejdro.pebblenotificationcenter.notification.api

fun interface AppNameProvider {
   fun getAppName(pkg: String): String
}
