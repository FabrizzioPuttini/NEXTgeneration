plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        debug {
            val apiUrl = providers.gradleProperty("API_BASE_URL").getOrElse("https://nextgeneration-seven.vercel.app/")
            require(apiUrl.matches(Regex("https?://[A-Za-z0-9.:-]+/"))) { "API_BASE_URL must be an http(s) origin ending in /" }
            buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://nextgeneration-seven.vercel.app/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.fragment:fragment:1.6.2")
    debugImplementation("androidx.fragment:fragment-testing-manifest:1.6.2")
    androidTestImplementation("androidx.fragment:fragment-testing:1.6.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}