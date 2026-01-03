package com.matejdro.pebblenotificationcenter.showkase

import android.content.Context
import com.airbnb.android.showkase.annotation.ShowkaseRoot
import com.airbnb.android.showkase.annotation.ShowkaseRootModule
import com.airbnb.android.showkase.models.Showkase
import com.matejdro.pebblenotificationcenter.ui.showkase.ShowkaseLauncher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
class ShowkaseLauncherImpl @Inject constructor() : ShowkaseLauncher {
   override fun launch(context: Context) {
      context.startActivity(Showkase.getBrowserIntent(context))
   }
}

@ShowkaseRoot
class MyRootModule : ShowkaseRootModule
