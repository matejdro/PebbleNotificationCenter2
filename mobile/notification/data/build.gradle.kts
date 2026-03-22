plugins {
   androidLibraryModule
   di
   unmock
}

android {
   androidResources.enable = true
   namespace = "com.matejdro.pebblenotificationcenter.notification"
}

custom {
   enableEmulatorTests = true
}

dependencies {
   api(projects.bluetooth.api)
   api(projects.bluetoothCommon)
   api(projects.commonAndroid)
   api(projects.notification.api)
   api(projects.rules.api)
   api(libs.androidx.datastore.preferences)
   api(libs.dispatch)

   implementation(libs.androidx.core)
   implementation(libs.logcat)
   implementation(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.pebblekit.api)

   testImplementation(testFixtures(projects.common))
   testImplementation(testFixtures(projects.bluetooth.api))
   testImplementation(testFixtures(projects.notification.api))
   testImplementation(testFixtures(projects.rules.api))
   testImplementation(libs.kotlinova.core.test)

   androidTestImplementation(libs.androidx.test.runner)
   androidTestImplementation(libs.androidx.test.core)
   androidTestImplementation(libs.kotest.assertions)
   androidTestImplementation(libs.junit4)
}
