import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.VariantOutputConfiguration
import org.gradle.accessors.dm.LibrariesForLibs
import tasks.setupTooManyKotlinFilesTaskForApp
import java.io.ByteArrayOutputStream

val libs = the<LibrariesForLibs>()

plugins {
   id("com.android.application")
   id("android-module-commons")
   id("kotlinova")
   id("com.jraska.module.graph.assertion")
   id("di")
}

moduleGraphAssert {
   maxHeight = 6
   restricted = arrayOf(
      ":common-navigation -X> .*",

      // Prevent all modules but this app module from depending on :data and :ui modules
      ":(?!$name|logging:crashreport).* -X> .*:data",
      ":(?!$name).* -X> .*:ui",

      // Only allow common modules to depend on other common modules and shared resources
      ":common-.* -X> :(?!common).*",
   )
}

android {
   lint {
      checkDependencies = true
   }

   androidComponents {
      onVariants { variant ->
         val mainOutput =
            variant.outputs.single { it.outputType == VariantOutputConfiguration.OutputType.SINGLE }

         // TODO
         val gitHashProvider = providers.of(GitCommandValueSource::class.java) {}
         val baseVersionName = defaultConfig.versionName
         val buildNumberProvider = providers.environmentVariable("BUILD_NUMBER")
         val overrideVersionName = providers.environmentVariable("VERSION")
            .orElse(
               // A bit weird syntax as a workaround for the https://github.com/gradle/gradle/issues/30792
               buildNumberProvider.map { "$baseVersionName-$it" }
                  .orElse(gitHashProvider.map { gitHash -> "$baseVersionName-local-$gitHash" })
            )

         val overrideVersionCode = providers.environmentVariable("VERSION")
            .orElse(
               // A bit weird syntax as a workaround for the https://github.com/gradle/gradle/issues/30792
               buildNumberProvider.map { "$baseVersionName-$it" }
                  .orElse(gitHashProvider.map { gitHash -> "$baseVersionName-local-$gitHash" })
            )

         variant.buildConfigFields?.put(
            "VERSION_NAME",
            overrideVersionName.map {
               BuildConfigField(
                  "String",
                  "\"$it\"",
                  "App version"
               )
            }
         )

         mainOutput.versionName.set(overrideVersionName)
         mainOutput.versionCode.set(buildNumberProvider.orElse("1").map { it.toInt() })
      }
   }
}

setupTooManyKotlinFilesTaskForApp()

abstract class GitCommandValueSource : ValueSource<String, ValueSourceParameters.None> {

   @get:Inject
   abstract val execOperations: ExecOperations

   override fun obtain(): String {
      val outputStream = ByteArrayOutputStream()
      val errStream = ByteArrayOutputStream()

      val result = execOperations.exec {
         commandLine = listOf("git", "rev-parse", "--short", "HEAD")
         standardOutput = outputStream
         errorOutput = errStream
         isIgnoreExitValue = true
      }

      if (result.exitValue != 0) {
         val errorText = errStream.toByteArray().toString(Charsets.UTF_8)
         throw ProcessExecutionException("Git failed: $errorText")
      }

      return outputStream.toByteArray().toString(Charsets.UTF_8)
   }
}
