package com.matejdro.pebblenotificationcenter.navigation.keys

import com.matejdro.pebblenotificationcenter.navigation.keys.base.BaseScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.base.DetailKey
import kotlinx.serialization.Serializable

@Serializable
data class RuleDetailsScreenKey(val id: Int) : BaseScreenKey(), DetailKey
