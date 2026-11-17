plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.apkmint.template"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.apkmint.template"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
