package com.matejdro.pebblenotificationcenter.notification.parsing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.toBitmapOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

fun interface BitmapLoader {
   fun getBitmap(icon: Icon): Bitmap?
}

@ContributesBinding(AppScope::class)
class BitmapLoaderImpl @Inject constructor(private val context: Context) : BitmapLoader {
   override fun getBitmap(icon: Icon): Bitmap? {
      val drawable = icon.loadDrawable(context) ?: return null
      return drawable.toBitmapOrNull(
         width = LARGE_BITMAP_WIDTH_SIZE,
         height = LARGE_BITMAP_WIDTH_SIZE * drawable.intrinsicHeight / drawable.intrinsicWidth
      )
   }
}

// Pick something that is large enough to display nicely on all watches, but small enough to not overfill the ram
private const val LARGE_BITMAP_WIDTH_SIZE = 512
