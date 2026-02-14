package com.matejdro.notificationcenter.rules.keys

data class PreferencePair<T>(
   val key: PreferenceKeyWithDefault<T>,
   val value: T,
)

infix fun <T> PreferenceKeyWithDefault<T>.setTo(value: T): PreferencePair<T> {
   return PreferencePair(this, value)
}
