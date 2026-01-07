plugins {
   pureKotlinModule
   testHelpers
}

dependencies {
   api(projects.notification.api)
   implementation(projects.common.test)
}
