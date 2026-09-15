package com.matejdro.pebblenotificationcenter.notification.parsing

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

fun interface AppColorProvider {
   @ColorInt
   fun getAppColor(pkg: String): Int
}

@ContributesBinding(AppScope::class)
class AppColorProviderImpl @Inject constructor(private val context: Context) : AppColorProvider {
   override fun getAppColor(pkg: String): Int {
      return try {
         val appIcon = context.getPackageManager().getApplicationIcon(pkg)
         val iconBitmap = appIcon.toBitmapOrNull(SMALL_BITMAP_SIZE, SMALL_BITMAP_SIZE) ?: return 0
         val palette = Palette.from(iconBitmap).addTarget(Target.VIBRANT).generate()
         return palette.getColorForTarget(Target.VIBRANT, 0)
      } catch (ignored: NameNotFoundException) {
         0
      }
   }
}

private const val SMALL_BITMAP_SIZE = 50
