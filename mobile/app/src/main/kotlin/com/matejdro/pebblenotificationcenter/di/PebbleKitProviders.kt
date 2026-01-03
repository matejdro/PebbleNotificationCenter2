package com.matejdro.pebblenotificationcenter.di

import android.content.Context
import com.matejdro.pebble.bluetooth.common.WatchappId
import com.matejdro.pebblenotificationcenter.bluetooth.api.WATCHAPP_UUID
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.rebble.pebblekit2.client.DefaultPebbleAndroidAppPicker
import io.rebble.pebblekit2.client.DefaultPebbleInfoRetriever
import io.rebble.pebblekit2.client.DefaultPebbleSender
import io.rebble.pebblekit2.client.PebbleAndroidAppPicker
import io.rebble.pebblekit2.client.PebbleInfoRetriever
import io.rebble.pebblekit2.client.PebbleSender
import java.util.UUID

@ContributesTo(AppScope::class)
interface PebbleKitProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun providePebbleSender(context: Context): PebbleSender = DefaultPebbleSender(context)

   @Provides
   @SingleIn(AppScope::class)
   fun providePebbleInfoRetriever(context: Context): PebbleInfoRetriever = DefaultPebbleInfoRetriever(context)

   @Provides
   fun provideAndroidAppPicker(context: Context): PebbleAndroidAppPicker = DefaultPebbleAndroidAppPicker.getInstance(context)

   @Provides
   @WatchappId
   fun provideWatchappUuid(): UUID = WATCHAPP_UUID
}
