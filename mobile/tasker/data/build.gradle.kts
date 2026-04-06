plugins {
   androidLibraryModule
   di
}

android {
   namespace = "com.matejdro.notificationcenter.tasker.data"

   androidResources.enable = true
}


dependencies {
   api(projects.tasker.api)
   api(projects.notification.api)

   implementation(projects.commonAndroid)
   implementation(projects.sharedResources)
   implementation(libs.androidx.core)
   implementation(libs.logcat)
}
