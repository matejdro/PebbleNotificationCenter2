plugins {
   androidLibraryModule
   di
}

dependencies {
   api(projects.bluetooth.api)
   api(projects.bluetoothCommon)
   api(projects.bucketsync.api)
   api(libs.pebblekit.api)

   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)

   testImplementation(projects.bluetooth.test)
   testImplementation(projects.bucketsync.test)
   testImplementation(projects.bucketsync.data)
   testImplementation(libs.kotlinova.core.test)
}
