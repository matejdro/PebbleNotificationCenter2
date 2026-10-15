package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.matejdro.pebble.bluetooth.WatchMetadata
import com.matejdro.pebble.bluetooth.common.di.WatchappConnectionScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt

interface DrawableExtractor {
   fun convertIconDrawableToBitmapBytes(drawable: Drawable, width: Int, height: Int): ByteArray
   fun convertIconToBitmapBytes(bitmap: Any?, zoomLevel: Int): ByteArray
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
   ): ByteArray {
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      drawable.setBounds(0, 0, width, height)
      drawable.draw(canvas)

      val finalImage = ImagePixels(bitmap)
         .useAlphaAsValues()

      return finalImage.encodeMonochromeImageIntoBytes()
   }

   override fun convertIconToBitmapBytes(bitmap: Any?, zoomLevel: Int): ByteArray {
      val sourceBitmap = bitmap as? Bitmap ?: error("Image could not be loaded. bitmap: ${bitmap ?: "null"}")
      val drawable = BitmapDrawable(context.resources, sourceBitmap)

      val screenWidth = watchMetadata.screenWidth
      val screenHeight = watchMetadata.screenHeight
      val originalWidth: Int = drawable.intrinsicWidth
      val originalHeight: Int = drawable.intrinsicHeight

      val fitWidth: Int
      val fitHeight: Int
      if (screenWidth / originalWidth.toFloat() < screenHeight / originalHeight.toFloat()) {
         fitWidth = screenWidth
         fitHeight = originalHeight * screenWidth / originalWidth
      } else {
         fitWidth = originalWidth * screenHeight / originalHeight
         fitHeight = screenHeight
      }

      val targetWidth = (fitWidth * ZOOM_LEVEL_MULTIPLIERS[zoomLevel]).roundToInt().coerceAtLeast(1)
      val targetHeight = (fitHeight * ZOOM_LEVEL_MULTIPLIERS[zoomLevel]).roundToInt().coerceAtLeast(1)

      drawable.setBounds(0, 0, targetWidth, targetHeight)

      val scaledBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(scaledBitmap)

      drawable.draw(canvas)

      val finalImage = ImagePixels(scaledBitmap)
         .dither(toColorScreen = watchMetadata.colorWatch)

      return if (watchMetadata.colorWatch) {
         finalImage.encodeColorImageIntoBytes()
      } else {
         finalImage.encodeMonochromeImageIntoBytes()
      }
   }
}

private val ZOOM_LEVEL_MULTIPLIERS = floatArrayOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
