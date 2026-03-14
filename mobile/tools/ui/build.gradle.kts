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
   api(projects.notification.api)
   api(projects.rules.api)
   api(libs.kotlin.coroutines)
   api(libs.kotlinova.core)
   api(libs.kotlinova.navigation)

   implementation(projects.commonCompose)
   implementation(projects.sharedResources)
   implementation(libs.androidx.core)
   implementation(libs.androidx.datastore.preferences)
   implementation(libs.composePreference)
   implementation(libs.dispatch)
}
