import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

val h2rngDir: String = gradleLocalProperties(rootDir, providers).getProperty("h2rng.dir")
val ndkVer: String = gradleLocalProperties(rootDir, providers).getProperty("ndk.version")

android {
    namespace = "org.hear2read.h2rng"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.hear2read.h2rng"
        minSdk = 30
        targetSdk = 35
        versionCode = 14
        versionName = "2.0"
        ndkVersion = ndkVer
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }

        }
    }

    externalNativeBuild {
        cmake {
            path = file("${h2rngDir}/CMakeLists.txt")
            version = "3.21.0+"
        }
    }

    flavorDimensions += listOf("delivery","stl")

    productFlavors {
        // Full PAD delivery of all voices
        create("pad") {
            dimension = "delivery"
            manifestPlaceholders["appLabel"] = "Hear2Read NG"

            assetPacks += listOf(":en")
            assetPacks += listOf(":bn", ":kn")
            assetPacks += listOf(":as", ":gu", ":ml", ":mr", ":ne", ":or", ":pa", ":si", ":ta", ":te",
                ":hi"
            )
        }
        // Limited PAD delivery of select voices for faster testing
        create("padsmall") {
            dimension = "delivery"
            manifestPlaceholders["appLabel"] = "Hear2Read NG"

            assetPacks += listOf(":en")
            assetPacks += listOf(":bn", ":kn")
        }
        // Local delivery without PAD for immediate running
        create("local") {
            dimension = "delivery"
            manifestPlaceholders["appLabel"] = "Hear2Read NG (Local)"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            // No asset pack dependencies
        }

        // Static .so used in release app
        create("static") {
            dimension = "stl"
            ndk {
                abiFilters -= listOf("x86", "armeabi-v7a")
                abiFilters += listOf("arm64-v8a")
            }
            externalNativeBuild {
                cmake {
                    arguments += "-DANDROID_STL=c++_static"
                }
            }
        }

        // Shared .so used for emulator testing
        create("shared") {
            dimension = "stl"
            ndk {
                abiFilters -= listOf("arm64-v8a")
                abiFilters += listOf("x86", "armeabi-v7a")
            }
            externalNativeBuild {
                cmake {
                    arguments += "-DANDROID_STL=c++_shared"
                }
            }
        }
    }

    // padsmall uses the same code as pad
    sourceSets.getByName("padsmall") {
        java.setSrcDirs(listOf("src/pad/java"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

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
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.material.icons.extended)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    "padImplementation"(libs.asset.delivery)
    "padImplementation"(libs.asset.delivery.ktx)
    "padsmallImplementation"(libs.asset.delivery)
    "padsmallImplementation"(libs.asset.delivery.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.google.firebase.crashlytics.ndk)
}

androidComponents {
    beforeVariants { variantBuilder ->
        val flavours = variantBuilder.productFlavors.toMap()
        val delivery = flavours["delivery"]
        val stl = flavours["stl"]
        val buildType = variantBuilder.buildType

        val allowedVariants = setOf(
            // Final production release
            Triple("pad", "static", "release"),
            // Testing production release with limited voices
            Triple("padsmall", "static", "release"),
            // Testing build for phone
            Triple("local", "static", "debug"),
            // Testing build for emulator
            Triple("local", "shared", "debug")
        )

        val currentVariant = Triple(delivery, stl, buildType)

        if(currentVariant !in allowedVariants) {
            variantBuilder.enable = false
        }
    }
}