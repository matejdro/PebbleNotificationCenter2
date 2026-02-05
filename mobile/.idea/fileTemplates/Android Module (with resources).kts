plugins {
   androidLibraryModule
   di
}

android {
    namespace = "com.matejdro.notificationcenter.${NAME}"

    buildFeatures {
        androidResources = true
    }
}

dependencies {
    testImplementation(projects.common.test)
}
