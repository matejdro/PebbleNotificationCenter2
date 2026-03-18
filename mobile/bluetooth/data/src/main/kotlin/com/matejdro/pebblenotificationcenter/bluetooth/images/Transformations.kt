package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.Color
import kotlin.math.max

/**
 * Android's "monochrome" icons have identical RGB values everywhere, but they only differ in alpha values
 * Convert all alpha values in this image into full alpha + grayscale colors.
 */
fun ImagePixels.useAlphaAsValues(): ImagePixels {
   var maxAlpha = 1

   for (y in 0 until height) {
      for (x in 0 until width) {
         maxAlpha = max(maxAlpha, Color.alpha(this[x, y]))
      }
   }

   for (y in 0 until height) {
      for (x in 0 until width) {
         val alpha = UByte.MAX_VALUE.toInt() - Color.alpha(this[x, y]) * UByte.MAX_VALUE.toInt() / maxAlpha
         this[x, y] = Color.argb(UByte.MAX_VALUE.toInt(), alpha, alpha, alpha)
      }
   }

   return this
}

/**
 * Dither pixels into this image either into black/white (when [toColorScreen] is false)
 * or into Pebble colors (when [toColorScreen] is true).
 */
// Splitting it up would it be even worse. Numbers are part of the algorithm.
@Suppress("CognitiveComplexMethod", "MagicNumber")
fun ImagePixels.dither(toColorScreen: Boolean): ImagePixels {
   // Implementation of the Floyd Steinberg dithering
   val separatedColorArray: Array<Array<SeparatedColor>?> =
      Array(width) { x ->
         Array(width) { y ->
            SeparatedColor(this[x, y])
         }
      }

   for (y in 0..<height) {
      for (x in 0..<width) {
         val oldColor: SeparatedColor = separatedColorArray[x]!![y]
         val newColor: SeparatedColor = if (toColorScreen) {
            oldColor.getNearestPebbleTimeColor()
         } else {
            oldColor.getNearestBlackWhiteColor()
         }
         this[x, y] = newColor.toRGB()

         newColor.reverseSub(oldColor)

         if (x < width - 1) separatedColorArray[x + 1]!![y].addAndMultiplyAndDivide16(newColor, 7)

         if (y < height - 1) {
            if (x > 0) separatedColorArray[x - 1]!![y + 1].addAndMultiplyAndDivide16(newColor, 3)

            separatedColorArray[x]!![y + 1].addAndMultiplyAndDivide16(newColor, 5)

            if (x < width - 1) separatedColorArray[x + 1]!![y + 1].addAndMultiplyAndDivide16(newColor, 1)
         }
      }
   }

   return this
}
