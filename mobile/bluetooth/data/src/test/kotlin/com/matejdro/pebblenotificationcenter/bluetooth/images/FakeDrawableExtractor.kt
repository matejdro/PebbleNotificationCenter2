package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.drawable.Drawable

class FakeDrawableExtractor : DrawableExtractor {
   private val outputMap = mutableMapOf<DrawableExtractorRequest, ByteArray>()

   fun registerOutput(drawable: Drawable, width: Int, height: Int, colorWatch: Boolean, output: ByteArray) {
      outputMap[DrawableExtractorRequest(drawable, width, height, colorWatch)] = output
   }

   override fun convertIconDrawableToBitmapBytes(
      drawable: Drawable,
      width: Int,
      height: Int,
      colorWatch: Boolean,
   ): ByteArray {
      return outputMap[DrawableExtractorRequest(drawable, width, height, colorWatch)]
         ?: error(
            "Output of drawable=$drawable, width=$width, height=$height, colorWatch=$colorWatch does not exist." +
               " Existing fakes: ${outputMap.keys}"
         )
   }

   private data class DrawableExtractorRequest(
      val drawable: Drawable,
      val width: Int,
      val height: Int,
      val colorWatch: Boolean,
   )
}
