plugins {
   pureKotlinModule
   testFixtures
}

dependencies {
   api(projects.notification.api)

   testFixturesApi(projects.notification.api)
}
