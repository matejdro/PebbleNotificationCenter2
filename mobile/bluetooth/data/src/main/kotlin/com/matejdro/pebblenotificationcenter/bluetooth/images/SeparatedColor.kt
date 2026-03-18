package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.Color
import kotlin.math.roundToInt

class SeparatedColor(var r: Int, var g: Int, var b: Int) {
   constructor(rgb: Int) : this(
      Color.red(rgb),
      Color.green(rgb),
      Color.blue(rgb)
   )

   fun add(other: SeparatedColor) {
      r += other.r
      g += other.g
      b += other.b
   }

   fun sub(other: SeparatedColor) {
      r -= other.r
      g -= other.g
      b -= other.b
   }

   fun reverseSub(other: SeparatedColor) {
      r = other.r - r
      g = other.g - g
      b = other.b - b
   }

   fun multiply(scalar: Double) {
      r = (r * scalar).toInt()
      g = (g * scalar).toInt()
      b = (b * scalar).toInt()
   }

   @Suppress("MagicNumber") // Self evident
   fun addAndMultiplyAndDivide16(quantError: SeparatedColor, scalar: Int) {
      r += quantError.r * scalar / 16
      g += quantError.g * scalar / 16
      b += quantError.b * scalar / 16
   }

   @Suppress("MagicNumber") // Self evident
   fun getNearestPebbleTimeColor(): SeparatedColor {
      return SeparatedColor(
         (r / RGB_TO_PEBBLE_DIVIDER.toFloat()).roundToInt() * RGB_TO_PEBBLE_DIVIDER,
         (g / RGB_TO_PEBBLE_DIVIDER.toFloat()).roundToInt() * RGB_TO_PEBBLE_DIVIDER,
         (b / RGB_TO_PEBBLE_DIVIDER.toFloat()).roundToInt() * RGB_TO_PEBBLE_DIVIDER
      )
   }

   @Suppress("MagicNumber") // Math formulas
   fun getNearestBlackWhiteColor(): SeparatedColor {
      val luma = (r + r + b + g + g + g) / 6
      val color = if (luma > 255 / 2) 255 else 0
      return SeparatedColor(color, color, color)
   }

   fun copy(): SeparatedColor = SeparatedColor(r, g, b)

   fun toRGB(): Int = Color.rgb(r, g, b)
}

/**
 * Divider of the 8-bit color value into the Pebble's 2-bit color.
 */
private const val RGB_TO_PEBBLE_DIVIDER = 85
