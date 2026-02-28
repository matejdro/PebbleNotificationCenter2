plugins {
   pureKotlinModule
   testHelpers
}

dependencies {
   api(projects.bluetooth.api)
   api(projects.notification.api)
}
