package com.matejdro.pebblenotificationcenter.navigation.keys

import kotlinx.serialization.Serializable
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Serializable
data class ActionListToggleKey(val directoryId: Int) : ScreenKey()
