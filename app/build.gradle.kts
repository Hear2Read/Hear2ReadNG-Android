import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    id("com.autonomousapps.dependency-analysis")
}

val h2rngDir: String = gradleLocalProperties(rootDir, providers).getProperty("h2rng.dir")
val ndkVer: String = gradleLocalProperties(rootDir, providers).getProperty("ndk.version")
val ortbuildDir: String = gradleLocalProperties(rootDir, providers).getProperty("ortbuild.dir")
val allPacks = listOf(":en", ":as", ":bn", ":gu", ":kn", ":ml", ":mr", ":ne", ":or", ":pa", ":sa",
    ":si", ":ta", ":te", ":hi")
val smallPacks = listOf(":si", ":kn", ":hi")

android {
    namespace = "org.hear2read.h2rng"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.hear2read.h2rng"
        minSdk = 30
        targetSdk = 36
        versionCode = 18
        versionName = "2.4"
        ndkVersion = ndkVer
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        resValues = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")

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

            assetPacks += allPacks
        }
        // Limited PAD delivery of select voices for faster testing
        create("padsmall") {
            dimension = "delivery"
            manifestPlaceholders["appLabel"] = "Hear2Read NG (small)"
//            applicationIdSuffix = ".small"
            versionNameSuffix = "-small"
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
                    arguments += "-DORT_BUILD_DIR=${ortbuildDir}"
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
//        java.directories.clear()
        java.directories.add("src/pad/java")
        kotlin.directories.add("src/pad/java")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugRuntimeOnly(libs.androidx.ui.test.manifest)

    implementation(libs.firebase.crashlytics)
    implementation(libs.google.firebase.crashlytics.ndk)

    "padImplementation"(libs.asset.delivery)
    "padImplementation"(libs.asset.delivery.ktx)
    "padsmallImplementation"(libs.asset.delivery)
    "padsmallImplementation"(libs.asset.delivery.ktx)

    // Transitives
    // -----------
    // Testing
    androidTestImplementation(libs.androidx.monitor)
    androidTestImplementation(libs.junit)
    androidTestRuntimeOnly(libs.androidx.runner)
    androidTestRuntimeOnly(libs.kotlinx.coroutines.test)
    // Androidx
    implementation(libs.androidx.activity)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.common)
    implementation(libs.androidx.navigation.runtime)
    implementation(libs.kotlinx.coroutines.core)
    // Compose
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.unit)
    // Google Play
    "padImplementation"("com.google.android.play:core-common:2.0.4")
    "padsmallImplementation"("com.google.android.play:core-common:2.0.4")
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
            Triple("local", "shared", "debug"),
            // Testing build for sideloading
            Triple("local", "static", "release")
        )

        val currentVariant = Triple(delivery, stl, buildType)

        if(currentVariant !in allowedVariants) {
            variantBuilder.enable = false
        }
    }
}

// Flavor specific asset packs
gradle.startParameter.taskNames.forEach { taskName ->
    val isSmall = taskName.contains("Padsmall", ignoreCase = true)
    val isLocal = taskName.contains("Local", ignoreCase = true)

    if (isLocal) {
        android.assetPacks.clear()
    } else if (isSmall) {
        android.assetPacks.clear()
        android.assetPacks += smallPacks
    }
}