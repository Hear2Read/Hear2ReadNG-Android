plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.hear2read.h2rng"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.hear2read.h2rng"
        minSdk = 30
        targetSdk = 35
        versionCode = 8
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    assetPacks += listOf(":hi", ":en", ":kn")

//    val enableAllVoices: String? by project
    val enableAllVoices = providers.gradleProperty("enableAllVoices")
    if (enableAllVoices.getOrNull() == "true") {
        assetPacks += listOf(":as", ":gu", ":ml", ":mr", ":ne", ":or", ":pa",
            ":si", ":ta", ":te")
    }

    // Specifies one flavor dimension.
//    flavorDimensions += "version"
//    productFlavors {
//        create("demo") {
////            dimension = "version"
//            applicationIdSuffix = ".demo"
//            versionNameSuffix = "-demo"
//            assetPacks += listOf(":hi", ":en", ":kn")
//        }
//        create("full") {
////            dimension = "version"
////            applicationIdSuffix = ".full"
////            versionNameSuffix = "-full"
//            assetPacks += listOf(":hi", ":en", ":kn", ":as", ":gu", ":ml", ":mr", ":ne", ":or", ":pa",
//                ":si", ":ta", ":te")
//        }
//    }

//    assetPacks += listOf(":hi", ":en", ":kn", ":as", ":gu", ":ml", ":mr", ":ne", ":or", ":pa",
//        ":si", ":ta", ":te")
//    assetPacks += listOf(":hin", ":pan", ":eng")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.onnxruntime.android)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation(libs.asset.delivery)
    // For Kotlin use core-ktx
    implementation(libs.asset.delivery.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation("androidx.core:core-splashscreen:1.2.0")
}