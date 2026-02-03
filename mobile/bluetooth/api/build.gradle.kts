plugins {
   pureKotlinModule
}

dependencies {
   api(projects.notification.api)

   implementation(libs.pebblekit.common.api)
}
