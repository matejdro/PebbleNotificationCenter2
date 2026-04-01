plugins {
   androidLibraryModule
   compose
   di
   navigation
   serialization
}

android {
   namespace = "com.matejdro.pebblenotificationcenter.history.ui"

   androidResources.enable = true
}

dependencies {
   api(projects.history.api)
   api(projects.common)
   api(libs.kotlin.coroutines)
   api(libs.kotlinova.core)
   api(libs.kotlinova.navigation)

   implementation(projects.commonCompose)
   implementation(libs.kotlinova.compose)

   testImplementation(testFixtures(projects.history.api))
   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.turbine)
}
