package com.matejdro.notificationcenter.rules.di

import app.cash.sqldelight.db.SqlDriver
import com.matejdro.notificationcenter.rules.sqldelight.generated.Database
import com.matejdro.notificationcenter.rules.sqldelight.generated.DbRuleQueries
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface RulesDataProviders {
   @Provides
   @SingleIn(AppScope::class)
   fun provideDatabase(driver: SqlDriver): Database {
      return Database(driver)
   }

   @Provides
   fun provideRuleQueries(database: Database): DbRuleQueries {
      return database.dbRuleQueries
   }
}
