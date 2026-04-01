plugins {
   pureKotlinModule
   di
   sqldelight
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.pebblenotificationcenter.history.sqldelight.generated")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
      }
   }
}


dependencies {
   api(projects.history.api)
   api(libs.kotlin.coroutines)
   api(libs.kotlinova.core)

   implementation(libs.dispatch)

   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.turbine)
}
