pluginManagement {
   repositories {
      google()
      mavenCentral()
      gradlePluginPortal()
   }
}

dependencyResolutionManagement {
   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

   repositories {
      mavenLocal()
      google()
      mavenCentral()
      maven("https://jitpack.io")
   }

   versionCatalogs {
      create("libs") {
         from(files("config/libs.toml"))
      }
   }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "PebbleNotificationCenter"

include(":app")
include(":app-screenshot-tests")
include(":common")
include(":common:test")
include(":common-android")
include(":common-android:test")
include(":common-compose")
include(":common-navigation")
include(":detekt")
include(":logging:api")
include(":logging:data")
include(":logging:crashreport")
include(":home:ui")
include(":shared-resources")
include(":bluetooth:api")
include(":bluetooth:data")
include(":bluetooth:test")
include(":navigation-impl")
include(":tools:ui")

// Projects from PebbleCommons

include(":bluetooth-common")
project(":bluetooth-common").projectDir = file("../PebbleCommons/mobile/bluetooth-common")

include(":bucketsync:api")
project(":bucketsync:api").projectDir = file("../PebbleCommons/mobile/bucketsync/api")
include(":bucketsync:data")
project(":bucketsync:data").projectDir = file("../PebbleCommons/mobile/bucketsync/data")
include(":bucketsync:test")
project(":bucketsync:test").projectDir = file("../PebbleCommons/mobile/bucketsync/test")
project(":bucketsync").projectDir = file("../PebbleCommons/mobile/bucketsync")
