plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.apkmint.creator"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.apkmint.creator"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        noCompress += listOf("apk")
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/BCKEY.DSA",
                "META-INF/BCKEY.SF",
                "META-INF/BC1024KE.DSA",
                "META-INF/BC1024KE.SF",
                "META-INF/BC2048KE.DSA",
                "META-INF/BC2048KE.SF"
            )
        }
    }
}

val copyTemplateApk by tasks.registering(Copy::class) {
    dependsOn(":template:assembleRelease")
    from(project(":template").layout.buildDirectory.file("outputs/apk/release/template-release-unsigned.apk"))
    into(layout.projectDirectory.dir("src/main/assets"))
    rename { "template.apk" }
}

tasks.named("preBuild") {
    dependsOn(copyTemplateApk)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.android.tools.build:apksig:8.7.3")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}