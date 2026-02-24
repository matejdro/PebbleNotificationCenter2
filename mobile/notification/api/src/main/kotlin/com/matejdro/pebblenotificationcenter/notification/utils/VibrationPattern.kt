package com.matejdro.pebblenotificationcenter.notification.utils

fun parseVibrationPattern(text: String): List<Short>? {
   return text.split(",", ".").mapNotNull {
      it.trim().toShortOrNull() ?: return null
   }.takeIf { it.size > 0 }
}
