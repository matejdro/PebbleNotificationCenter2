package com.matejdro.pebblenotificationcenter.notification.parsing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.FileNotFoundException

fun interface BitmapLoader {
   fun getBitmap(icon: IconCompat): Bitmap?
}

@ContributesBinding(AppScope::class)
class BitmapLoaderImpl @Inject constructor(private val context: Context) : BitmapLoader {
   // URI icons are streamed manually so the stream is always closed; other types use loadDrawable.
   override fun getBitmap(icon: IconCompat): Bitmap? = getBitmapFromIcon(icon)

   @Suppress("UseKtx", "SwallowedException")
   private fun getBitmapFromIcon(icon: IconCompat): Bitmap? {
      val type = icon.type
      return if (type == IconCompat.TYPE_URI || type == IconCompat.TYPE_URI_ADAPTIVE_BITMAP) {
         val uri = icon.uri
         try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
               val bitmap = BitmapFactory.decodeStream(stream) ?: return@use null
               val scaledHeight = LARGE_BITMAP_WIDTH_SIZE * bitmap.height / bitmap.width
               if (bitmap.width <= LARGE_BITMAP_WIDTH_SIZE) return@use bitmap
               Bitmap.createScaledBitmap(bitmap, LARGE_BITMAP_WIDTH_SIZE, scaledHeight, true)
            }
         } catch (e: SecurityException) {
            null // FileProvider not exported or permission denied
         } catch (e: IllegalArgumentException) {
            null // Invalid URI
         } catch (e: FileNotFoundException) {
            null // URI no longer available
         }
      } else {
         val drawable = icon.loadDrawable(context) ?: return null
         drawable.toBitmapOrNull(
            width = LARGE_BITMAP_WIDTH_SIZE,
            height = LARGE_BITMAP_WIDTH_SIZE * drawable.intrinsicHeight / drawable.intrinsicWidth
         )
      }
   }
}

// Pick something that is large enough to display nicely on all watches, but small enough to not overfill the ram
private const val LARGE_BITMAP_WIDTH_SIZE = 512
