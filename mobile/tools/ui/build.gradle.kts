plugins {
   androidLibraryModule
   compose
   di
   navigation
   serialization
   showkase
}

android {
   namespace = "com.matejdro.pebblenotificationcenter.tools.ui"

   androidResources.enable = true
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
