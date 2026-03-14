plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   compileOnly(libs.androidx.compose.runtime.annotation)

   implementation(libs.kotlin.coroutines)

   testFixturesApi(projects.bluetooth.api)
   testFixturesApi(projects.notification.api)
}
