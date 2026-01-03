plugins {
   androidLibraryModule
   compose
   di
   navigation
}

dependencies {
   api(libs.kotlinova.navigation)
   implementation(libs.androidx.compose.material3.sizeClasses)
   implementation(libs.androidx.navigation3)
   implementation(libs.accompanist.adaptive)
   implementation(libs.androidx.datastore.preferences)
   implementation(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.compose)
   implementation(libs.kotlinova.navigation.navigation3)
}
