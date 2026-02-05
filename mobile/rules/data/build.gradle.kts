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

   testImplementation(projects.common.test)
   testImplementation(libs.kotlinova.core.test)
}
