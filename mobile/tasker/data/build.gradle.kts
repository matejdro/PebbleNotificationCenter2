plugins {
   androidLibraryModule
   di
   unmock
}

android {
   namespace = "com.matejdro.notificationcenter.tasker"

   androidResources.enable = true
}


dependencies {
   api(projects.tasker.api)
   api(projects.notification.api)
   api(libs.dispatch)

   implementation(projects.commonAndroid)
   implementation(projects.rules.api)
   implementation(projects.sharedResources)
   implementation(libs.androidx.core)
   implementation(libs.androidx.datastore.preferences)
   implementation(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.logcat)

   testImplementation(testFixtures(projects.common))
}
