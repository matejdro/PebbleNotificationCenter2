package com.matejdro.pebblenotificationcenter.rules.di

import app.cash.sqldelight.db.SqlDriver
import com.matejdro.pebblenotificationcenter.rules.sqldelight.generated.Database
import com.matejdro.pebblenotificationcenter.rules.sqldelight.generated.DbRuleQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface RulesDataProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun provideMainDatabase(driver: SqlDriver): Database {
      return Database(driver)
   }

   @Provides
   fun provideRuleQueries(database: Database): DbRuleQueries {
      return database.dbRuleQueries
   }
}
