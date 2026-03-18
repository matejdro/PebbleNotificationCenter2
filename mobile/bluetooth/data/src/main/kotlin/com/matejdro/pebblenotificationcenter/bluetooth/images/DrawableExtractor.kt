package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

interface DrawableExtractor {
   fun convertIconDrawableToBitmapBytes(drawable: Drawable, width: Int, height: Int, colorWatch: Boolean): ByteArray
}

@Inject
@ContributesBinding(AppScope::class)
class DrawableExtractorImpl : DrawableExtractor {
   override fun convertIconDrawableToBitmapBytes(
      drawable: Drawable,
      width: Int,
      height: Int,
      colorWatch: Boolean,
   ): ByteArray {
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      drawable.setBounds(0, 0, width, height)
      drawable.draw(canvas)

      val finalImage = ImagePixels(bitmap)
         .useAlphaAsValues()
         .dither(toColorScreen = colorWatch)

      return if (colorWatch) {
         finalImage.encodeColorImageIntoBytes()
      } else {
         finalImage.encodeMonochromeImageIntoBytes()
      }
   }
}
