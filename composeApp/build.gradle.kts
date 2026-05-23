import java.util.Properties

plugins {
    kotlin("multiplatform") version "1.9.23"
    id("com.android.application") version "8.2.2"
    id("org.jetbrains.compose") version "1.6.1"
    id("app.cash.sqldelight") version "2.0.1"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("com.google.android.gms:play-services-location:21.2.0")
                implementation("com.google.android.gms:play-services-maps:18.2.0")
                implementation("com.google.maps.android:maps-compose:4.3.3")
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }
    }
}

android {
    namespace = "com.example.gpsspeedometer"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.gpsspeedometer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val props = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            props.load(localPropertiesFile.inputStream())
        }
        manifestPlaceholders["MAPS_API_KEY"] = props.getProperty("MAPS_API_KEY") ?: ""
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.example.gpsspeedometer.db")
        }
    }
}
