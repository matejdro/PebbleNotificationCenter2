package com.matejdro.pebblenotificationcenter.navigation.keys

import kotlinx.parcelize.Parcelize
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Parcelize
data class ActionListToggleKey(val directoryId: Int) : ScreenKey()
