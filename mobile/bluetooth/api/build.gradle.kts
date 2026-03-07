plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   api(projects.notification.api)

   implementation(libs.pebblekit.common.api)

   testFixturesApi(projects.bluetooth.api)
   testFixturesApi(projects.bucketsync.api)
   testFixturesApi(projects.notification.api)
   testFixturesApi(libs.pebblekit.api)
   testFixturesImplementation(libs.kotlin.coroutines)
}
