// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}
buildscript {
    dependencies {
        classpath ("com.google.ar.sceneform:plugin:1.15.0")
    }
}