plugins {
   androidLibraryModule
   compose
   di
   navigation
   showkase
}

android {
   namespace = "com.matejdro.pebblenotificationcenter.tools.ui"

   buildFeatures {
      androidResources = true
   }
}

dependencies {
   api(projects.common)
   api(projects.logging.api)
   api(libs.kotlin.coroutines)
   api(libs.kotlinova.core)
   api(libs.kotlinova.navigation)

   implementation(projects.commonCompose)
   implementation(libs.androidx.core)
   implementation(libs.dispatch)
}
