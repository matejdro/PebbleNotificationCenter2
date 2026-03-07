plugins {
   pureKotlinModule
}

dependencies {
   implementation(libs.androidx.datastore)
   implementation(libs.kotlin.coroutines.test)
   implementation(libs.kotlin.coroutines)
   implementation(libs.dispatch.test)
   implementation(libs.kotest.assertions)
   implementation(libs.turbine)
}
