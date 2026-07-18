plugins {
    id("com.android.application")
}

android {
    namespace = "de.sanniki.gestureking"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.sanniki.gestureking"
        minSdk = 26
        targetSdk = 36
        versionCode = 37
        versionName = "0.14"
    }
}

dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
