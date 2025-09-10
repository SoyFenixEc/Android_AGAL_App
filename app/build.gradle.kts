plugins {
    id("com.google.gms.google-services") // ← Plugin aplicado PRIMERO
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.agalplataformaeducativa.webview"
    compileSdk = 35 // ✅ Actualiza a 35

    defaultConfig {
        applicationId = "com.agalplataformaeducativa.webview"
        minSdk = 24
        targetSdk = 35 // ✅ ¡CAMBIA ESTO A 35!
        versionCode = 1
        versionName = "1.0"
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

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true // ✅ ¡HABILITA OFUSCACIÓN!
            isShrinkResources = true // ✅ Reduce tamaño
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Google AdMob
    implementation("com.google.android.gms:play-services-ads:23.5.0") // ✅ Actualizado

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // ✅ Actualizado
    implementation("com.google.firebase:firebase-analytics")

    // Lifecycle
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}