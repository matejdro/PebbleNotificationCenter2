package com.matejdro.pebblenotificationcenter.navigation.keys

import com.matejdro.pebblenotificationcenter.navigation.keys.base.BaseScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.base.ListKey
import kotlinx.serialization.Serializable

@Serializable
data object RuleListScreenKey : BaseScreenKey(), ListKey
