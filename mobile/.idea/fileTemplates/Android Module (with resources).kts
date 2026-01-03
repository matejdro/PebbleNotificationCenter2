plugins {
   androidLibraryModule
   di
}

android {
    namespace = "com.matejdro.catapult.${NAME}"

    buildFeatures {
        androidResources = true
    }
}

dependencies {
    testImplementation(projects.common.test)
}
