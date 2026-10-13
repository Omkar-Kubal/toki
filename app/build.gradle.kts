plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace  = "com.focusfirst"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.focusfirst"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 2
        versionName   = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // V1: minify off for easier debugging; enable + configure R8 before Play release
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }
}

dependencies {
    // ── Core ──────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.localbroadcastmanager)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ── Lifecycle ─────────────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ── Activity ──────────────────────────────────────────────────────────
    implementation(libs.androidx.activity.compose)

    // ── Compose (versions managed by BOM) ─────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // ── Navigation ────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── Hilt ──────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // ── Room ──────────────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── WorkManager + Hilt integration ────────────────────────────────────
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // ── DataStore ─────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Preference (for PreferenceManager cross-process SharedPrefs) ──────
    implementation("androidx.preference:preference-ktx:1.2.1")

    // ── Coroutines ────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Play Billing ──────────────────────────────────────────────────────
    implementation(libs.play.billing.ktx)

    // ── Test ──────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase BOM — controls all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    
    // Analytics
    implementation("com.google.firebase:firebase-analytics")
    
    // Crashlytics
    implementation("com.google.firebase:firebase-crashlytics")
    
    // Firestore (for cloud sync)
    implementation("com.google.firebase:firebase-firestore")
    
    // Auth (anonymous auth for sync)
    implementation("com.google.firebase:firebase-auth")
}
