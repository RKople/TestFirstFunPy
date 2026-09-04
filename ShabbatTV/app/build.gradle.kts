plugins {
    id("com.android.application")
}

android {
    namespace = "fr.shabbattv"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.shabbattv"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.4-beta"
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
