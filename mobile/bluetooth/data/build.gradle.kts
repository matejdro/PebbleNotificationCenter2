plugins {
   androidLibraryModule
   di
}

dependencies {
   api(libs.dispatch)
   api(projects.bluetooth.api)
   api(projects.bluetoothCommon)
   api(projects.bucketsync.api)
   api(projects.notification.api)
   api(libs.pebblekit.api)

   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)
   implementation(libs.okio)
   implementation(libs.pebblekit.common)

   testImplementation(projects.bluetooth.test)
   testImplementation(projects.bucketsync.test)
   testImplementation(projects.bucketsync.data)
   testImplementation(projects.notification.test)
   testImplementation(libs.kotlinova.core.test)
}
