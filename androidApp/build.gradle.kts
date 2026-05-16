import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "dev.dominikstahl.emu_8051"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            storeFile = System.getenv("CI_KEYSTORE_PATH")?.takeIf { it.isNotEmpty() }?.let { file(it) }
                ?: file("release-key.jks")
            storePassword = System.getenv("CI_KEYSTORE_PASSWORD") ?: "password"
            keyAlias = System.getenv("CI_KEY_ALIAS") ?: "release_key"
            keyPassword = System.getenv("CI_KEY_PASSWORD") ?: "password"
        }
    }
    defaultConfig {
        applicationId = "dev.dominikstahl.emu_8051"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true // Set to true for production!
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}