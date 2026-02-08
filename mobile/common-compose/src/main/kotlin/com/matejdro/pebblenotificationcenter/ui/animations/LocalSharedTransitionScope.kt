package com.matejdro.pebblenotificationcenter.ui.animations

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope =
   staticCompositionLocalOf<SharedTransitionScope> { error("LocalSharedTransitionScope should be provided") }
