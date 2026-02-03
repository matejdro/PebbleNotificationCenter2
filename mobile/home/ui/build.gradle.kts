plugins {
   androidLibraryModule
   compose
   di
   navigation
   showkase
}

android {
   namespace = "com.matejdro.pebblenotificationcenter.home.ui"

   buildFeatures {
      androidResources = true
   }
}

dependencies {
   api(projects.notification.api)
   api(libs.kotlinova.navigation)

   implementation(projects.commonCompose)
   implementation(libs.accompanist.permissions)
   implementation(libs.androidx.core)
   implementation(libs.androidx.compose.material3.sizeClasses)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlin.coroutines)
   implementation(libs.pebblekit.ui)
}
