plugins {
   androidLibraryModule
   compose
   di
   navigation
}

android {
   namespace = "com.matejdro.notificationcenter.rules.ui"

   androidResources.enable = true
}

dependencies {
   api(projects.rules.api)
   api(projects.common)

   implementation(projects.commonCompose)
   implementation(libs.kotlinova.core)

   testImplementation(projects.common.test)
   testImplementation(testFixtures(projects.rules.api))
   testImplementation(libs.kotlinova.core.test)
}
