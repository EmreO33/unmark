plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing comes from env vars so the keystore itself never touches git.
// CI decodes the UNMARK_RELEASE_KEYSTORE_BASE64 secret to a file and sets these;
// locally, assembleRelease just produces an unsigned APK when they're unset.
val releaseKeystorePath: String? = System.getenv("UNMARK_RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword: String? = System.getenv("UNMARK_RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = System.getenv("UNMARK_RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = System.getenv("UNMARK_RELEASE_KEY_PASSWORD")

android {
    namespace = "com.unmark.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.unmark.app"
        minSdk = 29
        targetSdk = 34
        versionCode = 6
        versionName = "0.3.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // AGP embeds a "Dependency metadata" block in signed APKs by default (a Play Store
    // dependency-transparency feature). F-Droid's reproducible-build check rejects any
    // signing block it didn't produce itself, so this needs to stay off.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
