plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.arlandmeasuretest33"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.arlandmeasuretest33"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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

repositories {
    mavenCentral() // Add the repository here
}

dependencies {





    // ... existing code ...


    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // For dropdown menus (MaterialSpinner)
    implementation("com.google.android.material:material:1.11.0")
    // For image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.androidx.gridlayout)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Add your dependency for LottieFiles
    implementation("com.airbnb.android:lottie:6.1.0") // Latest version as of now


// ... existing code ...

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

    //login dependencides
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    //Scalable Size Unit (support for different screen sizes)
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    //Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

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
    // For Android-compatible logging
    // SLF4J for logging (Fix SLF4J warnings)
    implementation (libs.slf4j.api)
    implementation (libs.slf4j.android)
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")

    //weather
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")



    implementation("org.apache.poi:poi-ooxml:5.2.3") // For Word processing
    implementation("com.itextpdf:itext7-core:7.1.16") // For PDF conversion


    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.2.0"))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-librarie

    // Firebase Authentication
    implementation (libs.firebase.auth.ktx)

// Firebase Firestore (for database)
    implementation (libs.firebase.firestore.ktx)

    implementation ("com.google.firebase:firebase-auth-ktx:21.0.1")

    implementation ("com.google.android.gms:play-services-auth:20.2.0")
    implementation ("com.google.firebase:firebase-auth:21.0.1")

    implementation ("com.github.bumptech.glide:glide:4.15.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.0")



    // Gemini AI API - updated version
    implementation ("com.google.ai.client.generativeai:generativeai:0.3.0")

    // Coroutines for asynchronous operations
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation ("com.google.android.material:material:1.10.0")
}