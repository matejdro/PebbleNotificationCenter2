package com.matejdro.pebblenotificationcenter

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matejdro.pebblenotificationcenter.navigation.keys.HomeScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.OnboardingKey
import com.matejdro.pebblenotificationcenter.navigation.keys.RuleListScreenKey
import com.matejdro.pebblenotificationcenter.notification.NotificationServiceStatus
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@AssistedInject
class MainViewModel(
   private val preferences: DataStore<Preferences>,
   private val notificationServiceStatus: NotificationServiceStatus,
) : ViewModel() {
   private val _startingScreens = MutableStateFlow<List<ScreenKey>?>(null)
   val startingScreens: StateFlow<List<ScreenKey>?> = _startingScreens

   init {
      viewModelScope.launch {
         _startingScreens.value = if (
            notificationServiceStatus.isPermissionGranted() &&
            preferences.data.first()[onboardingShownVersion] == LATEST_VERSION
         ) {
            listOf(HomeScreenKey, RuleListScreenKey)
         } else {
            listOf(OnboardingKey)
         }

         preferences.edit {
            it[onboardingShownVersion] = LATEST_VERSION
         }
      }
   }

   @AssistedFactory
   fun interface Factory {
      fun create(): MainViewModel
   }
}

private const val LATEST_VERSION = 1
private val onboardingShownVersion = intPreferencesKey("onboarding_shown_version")
