plugins {
   pureKotlinModule
   di
   sqldelight
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.notificationcenter.rules.sqldelight.generated")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
      }
   }
}


dependencies {
   api(projects.rules.api)

   implementation(libs.dispatch)
   implementation(libs.kotlinova.core)

   testImplementation(libs.kotlin.coroutines)
   testImplementation(libs.kotlinova.core)
   testImplementation(libs.kotlinova.core.test)
}
