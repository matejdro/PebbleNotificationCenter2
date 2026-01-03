plugins {
   androidLibraryModule
   compose
   parcelize
}

dependencies {
   api(libs.kotlinova.navigation)
   implementation(libs.accompanist.adaptive)
   implementation(libs.androidx.activity.compose)
   implementation(libs.androidx.compose.material3.sizeClasses)
   implementation(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.compose)
}
