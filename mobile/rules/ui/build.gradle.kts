plugins {
   androidLibraryModule
   compose
   di
   navigation
   parcelize
   showkase
}

android {
   namespace = "com.matejdro.notificationcenter.rules.ui"

   androidResources.enable = true
}

dependencies {
   api(projects.rules.api)
   api(projects.common)
   api(libs.kotlin.coroutines)
   api(libs.kotlinova.navigation)

   implementation(projects.commonCompose)
   implementation(libs.kotlinova.core)
   implementation(libs.androidx.compose.material3.sizeClasses)
   implementation(libs.androidx.navigation3)

   testImplementation(testFixtures(projects.rules.api))
   testImplementation(libs.kotlin.coroutines)
   testImplementation(libs.kotlinova.core.test)
}
