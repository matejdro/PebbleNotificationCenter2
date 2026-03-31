package com.matejdro.pebble.bluetooth

class WatchMetadata(
   var watchBufferSize: Int = 0,
   var colorWatch: Boolean = false,
   var screenWidth: Int = STOCK_PEBBLE_WIDTH,
   var screenHeight: Int = STOCK_PEBBLE_HEIGHT,
)

private const val STOCK_PEBBLE_WIDTH = 144
private const val STOCK_PEBBLE_HEIGHT = 144
