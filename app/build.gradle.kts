plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.iot_p"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.iot_p"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        resValue("string", "google_maps_key", mapsApiKey)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
a"proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImp
    // local.properties에서 MAPS_API_KEY 불러오기
val localProperties = java.util.Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(localFile.inputStream())
    }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: ""lementation(libs.espresso.core)
}