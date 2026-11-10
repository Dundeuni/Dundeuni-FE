import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localApiConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

// Keep analysis transport on this branch independent of untracked local.properties.
val analysisApiMode = "REAL"
val analysisApiBaseUrl = "https://suitable-overblown-umbrella.ngrok-free.dev/"
// TODO(BE_CONTRACT): enable HTTP only after the draft contract and deployment URL are confirmed.
val authApiMode = localApiConfig.getProperty("auth.api.mode", "UNCONNECTED").trim()
val authApiBaseUrl = localApiConfig.getProperty("auth.api.baseUrl", "").trim()
val analysisApiMockResults = false

android {
    namespace = "com.dundueni.app"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dundueni.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "ANALYSIS_API_MODE", buildConfigString(analysisApiMode))
        buildConfigField("String", "AUTH_API_MODE", buildConfigString(authApiMode))
        buildConfigField("String", "AUTH_API_BASE_URL", buildConfigString(authApiBaseUrl))
        buildConfigField("String", "ANALYSIS_API_BASE_URL", buildConfigString(analysisApiBaseUrl))
        buildConfigField("boolean", "ANALYSIS_API_MOCK_RESULTS", analysisApiMockResults.toString())

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty("user.home", rootProject.file(".gradle-codex").absolutePath)
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
