plugins {
   pureKotlinModule
   testHelpers
}

dependencies {
   api(projects.bluetooth.api)
   api(projects.bucketsync.api)
   api(projects.notification.api)
   api(libs.pebblekit.api)
   implementation(libs.kotlin.coroutines)
}
