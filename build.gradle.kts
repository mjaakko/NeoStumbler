// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false

    alias(libs.plugins.kotlinAndroid) version "2.2.0" apply false
    alias(libs.plugins.kotlinCompose) version "2.2.0" apply false
    alias(libs.plugins.kotlinSerialization) version "2.2.0" apply false

    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.bundletool) apply false

    alias(libs.plugins.ktfmt) apply false

    alias(libs.plugins.detekt) apply false
}
