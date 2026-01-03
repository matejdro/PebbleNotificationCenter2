import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
   id("app.cash.sqldelight")
}

dependencies {
   add("implementation", libs.sqldelight.async)
   add("implementation", libs.sqldelight.coroutines)

   add("testImplementation", libs.sqldelight.jvm)
}
