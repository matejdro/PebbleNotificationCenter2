plugins {
   androidAppModule
   compose
   navigation
   parcelize
   showkase
   sqldelight
}

android {
   namespace = "com.matejdro.pebblenotificationcenter"

   buildFeatures {
      buildConfig = true
   }

   defaultConfig {
      applicationId = "com.matejdro.pebblenotificationcenter2"
      targetSdk = 33
      versionCode = 1
      versionName = "1.0.0"
   }

   signingConfigs {
      getByName("debug") {
         // SHA1: C1:20:6F:FD:EB:EA:B1:2F:72:DE:21:BA:72:6E:FB:DB:5F:ED:9F:0C
         // SHA256: 99:4F:33:1F:2D:2F:91:22:1C:9A:A7:A5:35:50:0B:FC:E7:84:70:37:6A:3B:81:3E:84:3D:FE:3E:B5:74:A2:1A

         storeFile = File(rootDir, "keys/debug.jks")
         storePassword = "android"
         keyAlias = "androiddebugkey"
         keyPassword = "android"
      }

      create("release") {
         // SHA1: D5:85:7B:28:10:D2:80:C6:47:34:66:64:90:33:04:14:41:A0:87:D6
         // SHA256: 96:18:EB:20:0B:58:2A:B0:F9:0A:9C:48:19:24:36:A5:98:59:92:62:2D:5C:53:06:24:DA:0B:9E:D6:16:65:65

         storeFile = File(rootDir, "keys/release.jks")
         storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
         keyAlias = "app"
         keyPassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
      }
   }

   buildTypes {
      getByName("debug") {
         signingConfig = signingConfigs.getByName("debug")
      }

      getByName("release") {
         isMinifyEnabled = true
         isShrinkResources = true

         proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
         )

         signingConfig = signingConfigs.getByName("release")
      }
   }

   applicationVariants.all {
      outputs.all {
         val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
         output.outputFileName = "notificationcenter-mobile.apk"
      }
   }
}

afterEvaluate {
   tasks.named("verifyDebugDatabaseMigration") {
      // Workaround for the https://github.com/cashapp/sqldelight/issues/5115
      mustRunAfter("generateDebugDatabaseSchema")
   }
}

custom {
   enableEmulatorTests.set(true)
}

sqldelight {
   databases {
      create("Database") {
         packageName.set("com.matejdro.pebblenotificationcenter")
         schemaOutputDirectory.set(file("src/main/sqldelight/databases"))

         // Use project() wrapper as a workaround for the https://github.com/sqldelight/sqldelight/pull/5801
         dependency(project(projects.bucketsync.data.path))
      }
   }
}

dependencyAnalysis {
   issues {
      onUnusedDependencies {
         // False positive
         exclude(":common")
      }
   }
}

dependencies {
   implementation(projects.bluetoothCommon)
   implementation(projects.bluetooth.api)
   implementation(projects.bluetooth.data)
   implementation(projects.common)
   implementation(projects.commonAndroid)
   implementation(projects.commonNavigation)
   implementation(projects.commonCompose)
   implementation(projects.home.ui)
   implementation(projects.logging.api)
   implementation(projects.logging.crashreport)
   implementation(projects.logging.data)
   implementation(projects.navigationImpl)
   implementation(projects.notification.api)
   implementation(projects.notification.data)
   implementation(projects.bucketsync.api)
   implementation(projects.bucketsync.data)
   implementation(projects.tools.ui)
   implementation(projects.sharedResources)

   implementation(libs.androidx.activity.compose)
   implementation(libs.androidx.core)
   implementation(libs.androidx.core.splashscreen)
   implementation(libs.androidx.datastore)
   implementation(libs.androidx.datastore.preferences)
   implementation(libs.androidx.lifecycle.runtime)
   implementation(libs.androidx.lifecycle.viewModel)
   implementation(libs.androidx.lifecycle.viewModel.compose)
   implementation(libs.androidx.navigation3)
   implementation(libs.androidx.workManager)
   implementation(libs.coil)
   implementation(libs.dispatch)
   implementation(libs.kermit)
   implementation(libs.kotlin.immutableCollections)
   implementation(libs.moshi)
   implementation(libs.kotlin.coroutines)
   implementation(libs.kotlinova.core)
   implementation(libs.kotlinova.navigation)
   implementation(libs.kotlinova.navigation.navigation3)
   implementation(libs.logcat)
   implementation(libs.pebblekit)
   implementation(libs.simpleStack)
   implementation(libs.sqldelight.android)
   implementation(libs.tinylog.api)
}
