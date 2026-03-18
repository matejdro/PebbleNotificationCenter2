package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.Bitmap

/**
 * Lighter bitmap container that allows much faster access to getPixel and setPixel methods than Android's [Bitmap].
 */
class ImagePixels(bitmap: Bitmap) {
   val width: Int
   val height: Int
   private val pixels: IntArray

   init {
      width = bitmap.width
      height = bitmap.height
      pixels = IntArray(width * height)
      bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
   }

   operator fun get(x: Int, y: Int) = pixels[y * width + x]
   operator fun set(x: Int, y: Int, value: Int) {
      pixels[y * width + x] = value
   }
}
