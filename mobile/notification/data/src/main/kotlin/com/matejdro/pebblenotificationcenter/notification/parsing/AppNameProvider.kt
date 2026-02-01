package com.matejdro.pebblenotificationcenter.notification.parsing

import android.content.Context
import android.content.pm.PackageManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

fun interface AppNameProvider {
   fun getAppName(pkg: String): String
}

@Inject
@ContributesBinding(AppScope::class)
class AppNameProviderImpl(private val context: Context) : AppNameProvider {
   override fun getAppName(pkg: String): String {
      return try {
         context.packageManager.getApplicationInfo(pkg, 0).loadLabel(context.packageManager).toString()
      } catch (ignored: PackageManager.NameNotFoundException) {
         pkg
      }
   }
}
