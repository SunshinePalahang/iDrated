plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") // Corrected the alias usage
    id("com.google.gms.google-services") // Apply Google services plugin
}

android {
    namespace = "com.example.idrated"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.idrated"
        minSdk = 23
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Enable ViewBinding
    buildFeatures {
        viewBinding = true
    }

    // Set custom APK name
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
        implementation(libs.firebase.auth)
        implementation(libs.firebase.firestore)
        implementation(platform(libs.firebase.bom))
        implementation(libs.google.firebase.analytics)
        implementation(libs.com.google.firebase.firebase.database)
        implementation(libs.play.services.location)

        // Retrofit and Gson Converter
        implementation(libs.retrofit)
        implementation(libs.converter.gson)
        implementation(libs.firebase.storage.ktx)

        // Picasso
        implementation(libs.picasso)

        // Lottie Animation Library (Fixed Syntax)
        implementation(libs.lottie)

        implementation("com.google.code.gson:gson:2.10.1")


    // Test dependencies
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)

}
