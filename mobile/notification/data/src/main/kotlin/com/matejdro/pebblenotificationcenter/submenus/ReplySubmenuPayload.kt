package com.matejdro.pebblenotificationcenter.submenus

import android.app.PendingIntent

// Intents are a pain in the butt to unit test.
// For that reason we also don't check equality on intent, but only check for the identity and exclude intent from toString
data class ReplySubmenuPayload(
   val text: String,
   val intent: PendingIntent,
   val remoteInputResultKey: String,
) {
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is ReplySubmenuPayload) return false

      if (text != other.text) return false
      if (intent !== other.intent) return false
      if (remoteInputResultKey != other.remoteInputResultKey) return false

      return true
   }

   override fun hashCode(): Int {
      var result = text.hashCode()
      result = 31 * result + remoteInputResultKey.hashCode()
      return result
   }

   override fun toString(): String {
      return "ReplySubmenuPayload(text='$text', remoteInputResultKey='$remoteInputResultKey')"
   }
}
