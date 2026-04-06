package com.matejdro.pebblenotificationcenter.navigation.keys

import kotlinx.serialization.Serializable
import si.inova.kotlinova.compose.result.ResultKey
import si.inova.kotlinova.navigation.screenkeys.DialogKey
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

@Serializable
data class TaskerTaskSetScreenKey(
   val title: String,
   val initialList: Set<String>,
   val result: ResultKey<Set<String>>,
) : ScreenKey(), DialogKey
