package com.matejdro.pebblenotificationcenter.navigation.keys

import com.matejdro.pebblenotificationcenter.navigation.keys.base.BaseScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.base.TabContainerKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeScreenKey : BaseScreenKey(), TabContainerKey
