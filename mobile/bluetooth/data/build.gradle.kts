plugins {
   androidLibraryModule
   di
   unmock
}

dependencies {
   api(libs.dispatch)
   api(projects.bluetooth.api)
   api(projects.bluetoothCommon)
   api(projects.bucketsync.api)
   api(projects.notification.api)
   api(libs.pebblekit.api)

   implementation(projects.rules.api)
   implementation(libs.androidx.datastore.preferences)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.logcat)
   implementation(libs.okio)
   implementation(libs.pebblekit.common)
   implementation(libs.pngj)

   testImplementation(testFixtures(projects.common))
   testImplementation(testFixtures(projects.bluetooth.api))
   testImplementation(projects.bucketsync.test)
   testImplementation(projects.bucketsync.data)
   testImplementation(testFixtures(projects.notification.api))
   testImplementation(libs.kotlinova.core.test)
}
