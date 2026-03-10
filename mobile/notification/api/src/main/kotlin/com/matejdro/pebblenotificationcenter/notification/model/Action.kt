package com.matejdro.pebblenotificationcenter.notification.model

sealed class Action {
   abstract val title: String

   data class Dismiss(override val title: String) : Action()

   // This is pure kotlin module, so we cannot reference PendingIntent directly.
   // I don't want to change it, so for now we can just use Any and upcast it
   // For that reason we also don't check equality on intent, but only check for the identity and exclude intent from toString
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

   // This is pure kotlin module, so we cannot reference PendingIntent directly.
   // I don't want to change it, so for now we can just use Any and upcast it
   // For that reason we also don't check equality on intent, but only check for the identity and exclude intent from toString
   data class Reply(
      override val title: String,
      val intent: Any,
      val remoteInputResultKey: String,
      val cannedTexts: List<String>,
      val allowFreeFormInput: Boolean,
   ) : Action() {
      override fun equals(other: Any?): Boolean {
         if (this === other) return true
         if (other !is Reply) return false

         if (allowFreeFormInput != other.allowFreeFormInput) return false
         if (title != other.title) return false
         if (intent !== other.intent) return false
         if (remoteInputResultKey != other.remoteInputResultKey) return false
         if (cannedTexts != other.cannedTexts) return false

         return true
      }

      override fun hashCode(): Int {
         var result = allowFreeFormInput.hashCode()
         result = 31 * result + title.hashCode()
         result = 31 * result + remoteInputResultKey.hashCode()
         result = 31 * result + cannedTexts.hashCode()
         return result
      }

      override fun toString(): String {
         return "Reply(title='$title'," +
            " remoteInputResultKey='$remoteInputResultKey'," +
            " allowFreeFormInput=$allowFreeFormInput," +
            " cannedTexts=$cannedTexts)"
      }
   }

   data class PauseApp(override val title: String) : Action()
   data class PauseConversation(override val title: String) : Action()
}
