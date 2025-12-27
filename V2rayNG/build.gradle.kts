// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Add license plugin to the buildscript classpath so modules can apply it via `apply(plugin = ...)`.
        classpath("com.jaredsburrows:gradle-license-plugin:${libs.versions.gradleLicensePlugin.get()}")
    }
}