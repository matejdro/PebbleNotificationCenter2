package com.matejdro.pebblenotificationcenter.navigation.keys

import com.matejdro.pebblenotificationcenter.navigation.keys.base.BaseScreenKey
import com.matejdro.pebblenotificationcenter.navigation.keys.base.DetailKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class RuleDetailsScreenKey(val id: Int) : BaseScreenKey(), DetailKey
