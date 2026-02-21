plugins {
   androidLibraryModule
   compose
   di
   navigation
   parcelize
   showkase
   unmock
}

android {
   namespace = "com.matejdro.notificationcenter.rules.ui"

   androidResources.enable = true
}

dependencies {
   api(projects.notification.api)
   api(projects.rules.api)
   api(projects.common)
   api(libs.kotlin.coroutines)
   api(libs.kotlinova.navigation)

   implementation(projects.commonAndroid)
   implementation(projects.commonCompose)
   implementation(projects.sharedResources)
   implementation(libs.accompanist.drawablepainter)
   implementation(libs.androidx.compose.material3.sizeClasses)
   implementation(libs.androidx.datastore.preferences)
   implementation(libs.androidx.navigation3)
   implementation(libs.composePreference)
   implementation(libs.dispatch)
   implementation(libs.kotlinova.core)

   testImplementation(projects.notification.test)
   testImplementation(testFixtures(projects.rules.api))
   testImplementation(libs.kotlin.coroutines)
   testImplementation(libs.kotlinova.core.test)
   testImplementation(libs.kotlinova.navigation.test)
}
