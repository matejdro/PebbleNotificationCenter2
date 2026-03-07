plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   api(libs.kotlin.coroutines)

   implementation(libs.kotlinova.core)

   testFixturesImplementation(libs.androidx.datastore)
   testFixturesImplementation(libs.kotlin.coroutines.test)
   testFixturesImplementation(libs.kotlin.coroutines)
   testFixturesImplementation(libs.kotest.assertions)
   testFixturesImplementation(libs.turbine)
}
