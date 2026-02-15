plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   api(libs.androidx.datastore.preferences.core)
   api(libs.kotlinova.core)
   api(libs.kotlin.coroutines)

   compileOnly(libs.androidx.compose.runtime.annotation)
}
