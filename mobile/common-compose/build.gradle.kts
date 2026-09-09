plugins {
   androidLibraryModule
   compose
   showkase
}

android {
   namespace = "com.matejdro.pebblenotificationcenter.ui"

   androidResources.enable = true
}

dependencies {
   implementation(libs.androidx.navigation3)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.compose)
   implementation(libs.kotlin.coroutines)
   implementation(libs.composeDnd)
}
