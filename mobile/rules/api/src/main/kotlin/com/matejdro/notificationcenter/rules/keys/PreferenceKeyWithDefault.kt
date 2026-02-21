package com.matejdro.notificationcenter.rules.keys

import androidx.compose.runtime.Stable
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

class BooleanPreferenceKeyWithDefault(name: String, default: Boolean) : DirectKeyWithDefault<Boolean>(name, default) {
   override val key: Preferences.Key<Boolean>
      get() = booleanPreferencesKey(name)
}

class StringPreferenceKeyWithDefault(name: String, default: String) : DirectKeyWithDefault<String>(name, default) {
   override val key: Preferences.Key<String>
      get() = stringPreferencesKey(name)
}

class NullableStringPreferenceKeyWithDefault(name: String, default: String?) : DirectKeyWithDefault<String?>(name, default) {
   @Suppress("UNCHECKED_CAST")
   override val key: Preferences.Key<String?>
      get() = stringPreferencesKey(name) as Preferences.Key<String?>
}

class StringSetPreferenceKeyWithDefault(name: String, default: Set<String>) : DirectKeyWithDefault<Set<String>>(name, default) {
   override val key: Preferences.Key<Set<String>>
      get() = stringSetPreferencesKey(name)
}

@Suppress("FunctionNaming") // It's a factory function
inline fun <reified E : Enum<E>> EnumPreferenceKeyWithDefault(name: String, default: E) =
   EnumPreferenceKeyWithDefault(name, default, E::class.java)

class EnumPreferenceKeyWithDefault<T : Enum<T>>(name: String, default: T, private val cls: Class<T>) :
   ProxyPreferenceKeyWithDefault<T, String>(name, default) {
   override val key: Preferences.Key<String>
      get() = stringPreferencesKey(name)

   override fun serialize(value: T): String {
      return value.name
   }

   override fun deserialize(value: String): T {
      return java.lang.Enum.valueOf(cls, value)
   }
}

abstract class ProxyPreferenceKeyWithDefault<T, P>(protected val name: String, protected val default: T) :
   PreferenceKeyWithDefault<T> {
   abstract override val key: Preferences.Key<P>

   abstract fun serialize(value: T): P
   abstract fun deserialize(value: P): T

   override fun get(preferences: Preferences): T {
      return preferences[key]?.let(::deserialize) ?: default
   }

   override fun put(preferences: MutablePreferences, value: T) {
      preferences[key] = serialize(value)
   }
}

abstract class DirectKeyWithDefault<T>(protected val name: String, protected val default: T) : PreferenceKeyWithDefault<T> {
   abstract override val key: Preferences.Key<T>

   override fun get(preferences: Preferences): T {
      return preferences[key] ?: default
   }

   override fun put(preferences: MutablePreferences, value: T) {
      preferences[key] = value
   }
}

interface PreferenceKeyWithDefault<T> {
   val key: Preferences.Key<*>
   fun get(preferences: Preferences): T
   fun put(preferences: MutablePreferences, value: T)
}

operator fun <T> Preferences.get(key: PreferenceKeyWithDefault<T>): T {
   return key.get(this)
}

operator fun <T> MutablePreferences.set(key: PreferenceKeyWithDefault<T>, value: T) {
   key.put(this, value)
}

fun <T> MutablePreferences.remove(key: PreferenceKeyWithDefault<T>) {
   remove(key.key)
}

@Stable
interface SetPreference {
   operator fun <T> invoke(key: PreferenceKeyWithDefault<T>, value: T)
}

@Suppress("FunctionNaming") // It's a factory function
fun SetPreference(method: (key: PreferenceKeyWithDefault<*>, value: Any?) -> Unit) = object : SetPreference {
   override fun <T> invoke(key: PreferenceKeyWithDefault<T>, value: T) {
      method(key, value)
   }
}
