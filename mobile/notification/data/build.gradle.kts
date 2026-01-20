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
   api(projects.notification.api)
   api(libs.dispatch)

   implementation(projects.commonAndroid)
   implementation(libs.androidx.core)
   implementation(libs.logcat)
   implementation(libs.kotlin.coroutines)

   testImplementation(projects.bluetooth.test)
   testImplementation(projects.notification.test)
   testImplementation(libs.kotlinova.core.test)

   androidTestImplementation(libs.androidx.test.runner)
   androidTestImplementation(libs.androidx.test.core)
   androidTestImplementation(libs.kotest.assertions)
   androidTestImplementation(libs.junit4)
}
