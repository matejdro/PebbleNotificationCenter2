plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   compileOnly(libs.androidx.compose.runtime.annotation)

   testFixturesApi(projects.bluetooth.api)
   testFixturesApi(projects.notification.api)
}
