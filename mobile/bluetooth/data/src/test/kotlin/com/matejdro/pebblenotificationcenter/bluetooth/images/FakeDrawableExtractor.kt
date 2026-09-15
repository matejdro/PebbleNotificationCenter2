package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon

class FakeDrawableExtractor : DrawableExtractor {
   private val outputMap = mutableMapOf<Any, ByteArray>()
   var wasFilled: Boolean? = null

   fun registerOutput(drawable: Drawable, width: Int, height: Int, output: ByteArray, colorWatch: Boolean? = null) {
      outputMap[DrawableExtractorRequest(drawable, width, height, colorWatch)] = output
   }

   fun registerOutput(icon: Any, output: ByteArray) {
      outputMap[icon] = output
   }

   override fun convertIconDrawableToBitmapBytes(
      drawable: Drawable,
      width: Int,
      height: Int,
   ): ByteArray {
      return outputMap[DrawableExtractorRequest(drawable, width, height, null)]
         ?: error(
            "Output of drawable=$drawable, width=$width, height=$height, colorWatch=null does not exist." +
               " Existing fakes: ${outputMap.keys}"
         )
   }

   override fun convertIconToBitmapBytes(icon: Icon, fill: Boolean): ByteArray {
      wasFilled = fill
      return outputMap[icon] ?: error("Icon $icon does not exist. Existing fakes: ${outputMap.keys}")
   }

   private data class DrawableExtractorRequest(
      val drawable: Drawable,
      val width: Int,
      val height: Int,
      val colorWatch: Boolean? = null,
   )
}
