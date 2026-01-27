package com.matejdro.pebblenotificationcenter.notification.model

sealed class Action {
   abstract val title: String

   data class Dismiss(override val title: String) : Action()

   // This is pure kotlin module, so we cannot reference PendingIntent directly.
   // I don't want to change it, so for now we can just use Any and upcast it
   data class Native(override val title: String, val intent: Any) : Action() {
      override fun toString(): String {
         return "Native(title='$title')"
      }

      override fun equals(other: Any?): Boolean {
         if (this === other) return true
         if (other !is Native) return false

         if (title != other.title) return false
         if (intent !== other.intent) return false

         return true
      }

      override fun hashCode(): Int {
         var result = title.hashCode()
         result = 31 * result + intent.hashCode()
         return result
      }
   }
}
