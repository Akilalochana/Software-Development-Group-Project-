plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.arlandmeasuretest33"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.arlandmeasuretest33"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {

    //this one same
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.core.ktx) {
        exclude(group = "com.android.support")
    }
    implementation(libs.androidx.appcompat) {
        exclude(group = "com.android.support")
    }
    implementation(libs.material) {
        exclude(group = "com.android.support")
    }

    // AR dependencies
    implementation("com.google.ar:core:1.41.0")
    implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1") {
        exclude(group = "com.android.support")
    }
    implementation("com.google.ar.sceneform:core:1.17.1") {
        exclude(group = "com.android.support")
    }

    // Force AndroidX versions
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    //Scalable Size Unit (support for different screen sizes)
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    //Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("andeoidx.navigation:navigation-ui-ktx:2.7.5")

    //koin(dependency injection)
    implementation("io.insert-koin:koin-android:3.4.2")

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    //ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    //Coil
    implementation("io.coil-kt:coil:2.5.0")

    //Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

}