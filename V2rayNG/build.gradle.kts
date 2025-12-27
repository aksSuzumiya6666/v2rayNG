// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

// Intentionally do not add the license plugin to the root buildscript classpath here.
// The license plugin can be applied selectively in modules if (and only if) compatible with the AGP version.