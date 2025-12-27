plugins {
    alias(libs.plugins.android.application)
    // Kotlin is provided by AGP built-in Kotlin (AGP 9+); do not apply kotlin-android plugin here.
    // id("org.jetbrains.kotlin.android")
    // Temporarily disabled: license plugin triggers "Extension of type 'AppExtension' does not exist" with current AGP.
    // If you need license reports, re-enable via a compatible root buildscript classpath or use a compatible plugin version.
    // id("com.jaredsburrows.license")
}

import com.android.build.api.variant.FilterConfiguration
import com.android.build.api.variant.ApplicationVariant

// read ndk version from a Gradle property `-PndkVersion=...` or from env `NDK_VERSION`
val ndkVersionFromPropOrEnv: String? = (project.findProperty("ndkVersion") as? String)
    ?: System.getenv("NDK_VERSION")

android {
    namespace = "com.v2ray.ang"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.v2ray.ang"
        minSdk = 21
        targetSdk = 36
        versionCode = 684
        versionName = "1.10.32"
        multiDexEnabled = true

        val abiFilterList = (properties["ABI_FILTERS"] as? String)?.split(';')
        splits {
            abi {
                isEnable = true
                reset()
                if (abiFilterList != null && abiFilterList.isNotEmpty()) {
                    include(*abiFilterList.toTypedArray())
                } else {
                    include(
                        "arm64-v8a",
                        "armeabi-v7a",
                        "x86_64",
                        "x86"
                    )
                }
                isUniversalApk = abiFilterList.isNullOrEmpty()
            }
        }

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

    flavorDimensions.add("distribution")
    productFlavors {
        create("fdroid") {
            dimension = "distribution"
            applicationIdSuffix = ".fdroid"
            buildConfigField("String", "DISTRIBUTION", "\"F-Droid\"")
        }
        create("playstore") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"Play Store\"")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.add("libs")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    androidComponents {
        onVariants(selector().all()) { variant: ApplicationVariant ->
            val versionCode = android.defaultConfig.versionCode as Int
            val versionName = android.defaultConfig.versionName as String

            val isFdroid = variant.name.contains("fdroid", ignoreCase = true)

            variant.outputs.forEach { output ->
                val abi = output.filters.firstOrNull {
                    it.filterType == FilterConfiguration.FilterType.ABI
                }?.identifier ?: "universal"

                    if (isFdroid) {
                        val versionCodes = mapOf(
                            "armeabi-v7a" to 2, "arm64-v8a" to 1, "x86" to 4, "x86_64" to 3, "universal" to 0
                        )
                        // don't set output filename here (AGP public API may differ); only set version code
                        versionCodes[abi]?.let { code ->
                            output.versionCode.set((100 * versionCode + code) + 5000000)
                        }
                    } else {
                        val versionCodes = mapOf(
                            "armeabi-v7a" to 4, "arm64-v8a" to 4, "x86" to 4, "x86_64" to 4, "universal" to 4
                        )
                        // don't set output filename here; only set version code
                        versionCodes[abi]?.let { code ->
                            output.versionCode.set((1000000 * code) + versionCode)
                        }
                    }
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

}

dependencies {
    // Core Libraries
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    // AndroidX Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.preference.ktx)
    implementation(libs.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)

    // UI Libraries
    implementation(libs.material)
    implementation(libs.toasty)
    implementation(libs.editorkit)
    implementation(libs.flexbox)

    // Data and Storage Libraries
    implementation(libs.mmkv.static)
    implementation(libs.gson)

    // Reactive and Utility Libraries
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Language and Processing Libraries
    implementation(libs.language.base)
    implementation(libs.language.json)

    // Intent and Utility Libraries
    implementation(libs.quickie.foss)
    implementation(libs.core)

    // AndroidX Lifecycle and Architecture Components
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Background Task Libraries
    implementation(libs.work.runtime.ktx)
    implementation(libs.work.multiprocess)

    // Multidex Support
    // implementation(libs.multidex)

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.org.mockito.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

// Apply license plugin from root buildscript classpath (added in root build.gradle.kts)
// Only apply if legacy AppExtension class is present; the plugin expects the older AGP extension.
try {
    val hasLegacyAppExtension = runCatching {
        Class.forName("com.android.build.gradle.AppExtension")
        true
    }.getOrDefault(false)

    if (hasLegacyAppExtension) {
        apply(plugin = "com.jaredsburrows.license")
    } else {
        logger.warn("Skipping license plugin: legacy AppExtension class not found; plugin may be incompatible with current AGP.")
    }
} catch (e: Exception) {
    logger.warn("License plugin check failed: ${'$'}{e.message}")
}