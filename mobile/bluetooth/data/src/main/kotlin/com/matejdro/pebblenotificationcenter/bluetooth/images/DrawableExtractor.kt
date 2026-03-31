package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import com.matejdro.pebble.bluetooth.WatchMetadata
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

interface DrawableExtractor {
   fun convertIconDrawableToBitmapBytes(drawable: Drawable, width: Int, height: Int, colorWatch: Boolean): ByteArray
   fun convertIconToBitmapBytes(icon: Icon): ByteArray
}

@Inject
@ContributesBinding(WatchappConnectionScope::class)
class DrawableExtractorImpl(
   private val context: Context,
   private val watchMetadata: WatchMetadata,
) : DrawableExtractor {
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

      return if (colorWatch) {
         finalImage
            .dither(toColorScreen = true)
            .encodeColorImageIntoBytes()
      } else {
         finalImage.encodeMonochromeImageIntoBytes()
      }
   }

   override fun convertIconToBitmapBytes(icon: Icon): ByteArray {
      val drawable = icon.loadDrawable(context) ?: error("Drawable cannot be loaded. Icon: $icon")

      val screenWidth = watchMetadata.screenWidth
      val screenHeight = watchMetadata.screenHeight
      val targetWidth: Int
      val targetHeight: Int

      val originalWidth: Int = drawable.intrinsicWidth
      val originalHeight: Int = drawable.intrinsicHeight

      if (screenWidth / originalWidth.toFloat() < screenHeight / originalHeight.toFloat()) {
         targetWidth = screenWidth
         targetHeight = originalHeight * screenWidth / originalWidth
      } else {
         targetWidth = originalWidth * screenHeight / originalHeight
         targetHeight = screenHeight
      }

      val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      drawable.setBounds(0, 0, targetWidth, targetHeight)
      drawable.draw(canvas)

      val finalImage = ImagePixels(bitmap)
         .dither(toColorScreen = watchMetadata.colorWatch)

      return if (watchMetadata.colorWatch) {
         finalImage.encodeColorImageIntoBytes()
      } else {
         finalImage.encodeMonochromeImageIntoBytes()
      }
   }
}
