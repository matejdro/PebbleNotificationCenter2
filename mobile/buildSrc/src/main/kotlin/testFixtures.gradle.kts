import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.accessors.dm.LibrariesForLibs
import util.isAndroidProject

val libs = the<LibrariesForLibs>()

if (isAndroidProject()) {
   extensions.findByType<LibraryExtension>()?.apply {
      this.testFixtures.enable = true
   }
   extensions.findByType<ApplicationExtension>()?.apply {
      this.testFixtures.enable = true
   }
} else {
   apply(plugin = "java-test-fixtures")
}

dependencies {
   add("testFixturesImplementation", libs.kotlin.coroutines.test)
   add("testFixturesImplementation", libs.turbine)
}
