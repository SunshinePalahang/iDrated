plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.idrated"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.idrated"
        minSdk = 23
        targetSdk = 35 // Updated to match compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_17 // Updated to Java 17
        targetCompatibility = JavaVersion.VERSION_17 // Updated to Java 17
    }

    kotlinOptions {
        jvmTarget = "17" // Updated to Java 17
    }

    buildFeatures {
        viewBinding = true
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val variantName = name.capitalize()
            val versionCode = defaultConfig.versionCode
            val versionName = defaultConfig.versionName

            output.outputFileName = "iDrated-${versionName}(${versionCode})-${variantName}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Add Volley library if you need it for custom network operations (optional)
    implementation(libs.volley)

    // Firebase Authentication, Firestore, Realtime Database, and Analytics
    implementation(platform(libs.firebase.bom)) // Use BoM to manage versions
    implementation(libs.firebase.auth) // No version needed with BoM
    implementation(libs.firebase.firestore) // No version needed with BoM
    implementation(libs.google.firebase.analytics) // No version needed with BoM
    implementation(libs.com.google.firebase.firebase.database) // No version needed with BoM
    implementation(libs.play.services.location)

    // Retrofit and Gson Converter
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.firebase.storage.ktx)

    // Picasso
    implementation(libs.picasso)

    // Lottie Animation Library (Fixed Syntax)
    implementation(libs.lottie)

    implementation("com.google.code.gson:gson:2.13.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}