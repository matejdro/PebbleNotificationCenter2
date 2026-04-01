package com.matejdro.pebblenotificationcenter.history.di

import app.cash.sqldelight.db.SqlDriver
import com.matejdro.pebblenotificationcenter.history.sqldelight.generated.Database
import com.matejdro.pebblenotificationcenter.history.sqldelight.generated.DbHistoryQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface HistoryDataProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun provideHistoryDatabase(driver: SqlDriver): Database {
      return Database(driver)
   }

   @Provides
   fun provideHistoryQueries(database: Database): DbHistoryQueries {
      return database.dbHistoryQueries
   }
}
