import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import jacoco.setupJacocoMergingRoot
import nl.littlerobots.vcu.plugin.resolver.ModuleVersionCandidate
import nl.littlerobots.vcu.plugin.versionSelector
import si.inova.kotlinova.gradle.sarifmerge.SarifMergeTask

// Please do not add any subprojects {} / allprojects {} blocks or anything else that affects suborpojects to allow for
// project isolation when it comes out (https://gradle.github.io/configuration-cache/#project_isolation)

plugins {
   id("com.autonomousapps.dependency-analysis")
   alias(libs.plugins.versionCatalogUpdate)
   id("kotlinova")
   jacoco
}

setupJacocoMergingRoot()

jacoco {
   toolVersion = libs.versions.jacoco.get()
}

dependencyAnalysis {
   structure {
      ignoreKtx(true)

      bundle("coil") {
         // We only ever want coil-compose, so coil is considered as a group
         includeGroup("io.coil-kt.coil3")
      }

      bundle("compose") {
         // Compose libraries are blanket-included to for convenience. It shouldn't cause a big issue
         includeGroup("androidx.compose.animation")
         includeGroup("androidx.compose.foundation")
         includeGroup("androidx.compose.material")
         includeGroup("androidx.compose.material3")
         includeGroup("androidx.compose.runtime")
         includeGroup("androidx.compose.ui")
      }

      // Library Groups:

      bundle("androidxActivity") {
         includeGroup("androidx.activity")
      }

      bundle("androidxBenchmark") {
         includeGroup("androidx.benchmark")
      }

      bundle("androidxCore") {
         includeGroup("androidx.core")
      }

      bundle("androidxLifecycle") {
         includeGroup("androidx.lifecycle")
      }

      bundle("androidxTest") {
         includeGroup("androidx.test")
      }

      bundle("inject") {
         includeGroup("me.tatarka.inject")
         includeGroup("software.amazon.lastmile.kotlin.inject.anvil")
      }

      bundle("datastore") {
         includeGroup("androidx.datastore")
      }

      bundle("kotest") {
         includeGroup("io.kotest")
      }

      bundle("showkase") {
         includeGroup("com.airbnb.android")
      }

      bundle("sqlDelight") {
         includeGroup("app.cash.sqldelight")
      }

      bundle("dagger") {
         includeGroup("com.google.dagger")
         includeDependency("javax.inject:javax.inject")
      }

      bundle("kermit") {
         includeGroup("co.touchlab")
      }

      bundle("tinylog") {
         includeGroup("org.tinylog")
      }

      bundle("windowManagement") {
         includeGroup("androidx.window")
         includeDependency("com.google.accompanist:accompanist-adaptive")
      }

      bundle("PebbleKit2") {
         includeGroup("io.rebble.pebblekit2")
      }

      bundle("Navigation3") {
         includeGroup("androidx.navigation3")
      }

      bundle("WorkManager") {
         includeGroup("androidx.work")
         includeDependency("com.google.guava:listenablefuture")
      }
   }
}

versionCatalogUpdate {
   catalogFile.set(file("config/libs.toml"))

   fun ModuleVersionCandidate.newlyContains(keyword: String): Boolean {
      return !currentVersion.contains(keyword, ignoreCase = true) && candidate.version.contains(keyword, ignoreCase = true)
   }

   versionSelector {
      !it.newlyContains("alpha") &&
         !it.newlyContains("beta") &&
         !it.newlyContains("RC") &&
         !it.newlyContains("M") &&
         !it.newlyContains("eap") &&
         !it.newlyContains("dev") &&
         !it.newlyContains("pre")
   }
}

// Always update to the ALL distribution when updating Gradle
tasks.wrapper {
   distributionType = Wrapper.DistributionType.ALL
}

// Workaround for the https://youtrack.jetbrains.com/issue/QD-13913
// We remove the %SRCROOT% from the final merged sarif
tasks.named("reportMerge", SarifMergeTask::class.java).configure {
   @Suppress("UNCHECKED_CAST")
   doLast {
      val sarifFile = output.get().asFile
      if (sarifFile.exists()) {
         val sarifJson = JsonSlurper().parse(sarifFile) as Map<String, Any>
         val runs = sarifJson["runs"] as List<Map<String, Any>>

         val runsWithoutSrcRoot = runs.map { run -> run.filterKeys { it -> it != "originalUriBaseIds" } }
         val newSarifJson = sarifJson + mapOf("runs" to runsWithoutSrcRoot)
         sarifFile.writeText(JsonBuilder(newSarifJson).toPrettyString())
      }
   }
}
