package com.matejdro.pebblenotificationcenter.bluetooth.images

import android.graphics.Color
import ar.com.hjg.pngj.ImageInfo
import ar.com.hjg.pngj.ImageLineByte
import ar.com.hjg.pngj.PngWriter
import java.io.ByteArrayOutputStream

/**
 * Encode a monochrome image into a grayscale PNG.
 */
fun ImagePixels.encodeMonochromeImageIntoBytes(): ByteArray {
   val imageInfo: ImageInfo = ImageInfo(
      /* cols = */ width,
      /* rows = */ height,
      /* bitdepth = */ 1,
      /* alpha = */ false,
      /* grayscale = */ true,
      /* indexed = */ false
   )

   @Suppress("MissingUseCall") // ByteArrayOutputStream does not need to be closed
   val byteStream = ByteArrayOutputStream()
   val pngWriter = PngWriter(byteStream, imageInfo)

   for (y in 0..<height) {
      val imageLine = ImageLineByte(imageInfo)
      for (x in 0..<width) {
         val pixel: Int = this[x, y]
         val color = Color.red(pixel)

         imageLine.getScanline()[x] = (if (color > 0) 1 else 0).toByte()
      }

      pngWriter.writeRow(imageLine, y)
   }

   pngWriter.end()
   return byteStream.toByteArray()
}

/**
 * Encode an image in Pebble colors into color indexed PNG
 */
@Suppress("MagicNumber") // PNG constants
fun ImagePixels.encodeColorImageIntoBytes(): ByteArray {
   @Suppress("MissingUseCall") // ByteArrayOutputStream does not need to be closed
   val byteStream = ByteArrayOutputStream()
   val imageInfo = ImageInfo(
      /* cols = */ width,
      /* rows = */ height,
      /* bitdepth = */ 8,
      /* alpha = */ false,
      /* grayscale = */ false,
      /* indexed = */ true
   )
   val pngWriter = PngWriter(byteStream, imageInfo)

   val paletteChunk = pngWriter.getMetadata().createPLTEChunk()
   paletteChunk.setNentries(64)
   for (i in 0..63) {
      val color: Int = PEBBLE_TIME_PALETTE[i]
      paletteChunk.setEntry(i, Color.red(color), Color.green(color), Color.blue(color))
   }

   for (y in 0..<height) {
      val imageLine = ImageLineByte(imageInfo)
      for (x in 0..<width) {
         val pixel: Int = this[x, y] and 0x00FFFFFF
         val index = PEBBLE_TIME_PALETTE_MAP[pixel]

         requireNotNull(index) { "Color is not supported by Pebble Time: " + Integer.toHexString(pixel) }

         imageLine.getScanline()[x] = index
      }

      pngWriter.writeRow(imageLine, y)
   }

   pngWriter.end()
   return byteStream.toByteArray()
}

private val PEBBLE_TIME_PALETTE = IntArray(64)
private val PEBBLE_TIME_PALETTE_MAP = HashMap<Int, Byte>().apply {
   var counter = 0
   for (r in 0x000000..0xFF0000 step 0x550000) {
      for (g in 0x000000..0x00FF00 step 0x005500) {
         for (b in 0x000000..0x0000FF step 0x000055) {
            val color = r or g or b
            PEBBLE_TIME_PALETTE[counter] = color
            this[color] = counter.toByte()
            counter++
         }
      }
   }
}
