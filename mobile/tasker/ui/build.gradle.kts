plugins {
   androidLibraryModule
   compose
   di
   navigation
   showkase
}

android {
   namespace = "com.matejdro.notificationcenter.tasker.ui"

   androidResources.enable = true
}

dependencies {
   api(libs.kotlinova.navigation)
   api(libs.kotlin.serialization)

   implementation(projects.commonAndroid)
   implementation(projects.commonCompose)
   implementation(projects.sharedResources)
   implementation(projects.tasker.api)
   implementation(libs.androidx.core)
   implementation(libs.androidx.activity.compose)
   implementation(libs.androidx.navigation3)
   implementation(libs.composePreference)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.navigation.navigation3)
}
