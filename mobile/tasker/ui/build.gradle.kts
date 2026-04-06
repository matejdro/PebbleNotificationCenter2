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

   implementation(projects.commonCompose)
   implementation(projects.sharedResources)
   implementation(libs.androidx.activity.compose)
}
