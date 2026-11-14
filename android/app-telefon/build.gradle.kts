// Production version and signing identity are supplied by the protected
// GitHub Actions release workflow. No signing key is committed to git.
val releaseVersion = providers.gradleProperty("eymenReleaseVersion").orNull
val releaseCode = providers.gradleProperty("eymenReleaseCode").orNull?.toIntOrNull()
// Only an explicitly requested Google Play bundle enables the new Play API and
// version channel. Normal GitHub signed APKs keep their original settings.
val googlePlayBuild = providers.gradleProperty("eymenGooglePlay").orNull == "true"
if (googlePlayBuild) {
    require(releaseVersion == "1.0.0" && releaseCode == 1000000) {
        "Google Play first release requires versionName 1.0.0 and versionCode 1000000"
    }
}
val signingPath = System.getenv("EY_SIGNING_KEYSTORE_FILE")
val signingPassword = System.getenv("EY_SIGNING_STORE_PASSWORD")
val signingAlias = System.getenv("EY_SIGNING_KEY_ALIAS")
val signingKeyPassword = System.getenv("EY_SIGNING_KEY_PASSWORD")
val canSign = listOf(signingPath, signingPassword, signingAlias, signingKeyPassword)
    .all { !it.isNullOrBlank() }

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.eymen.browser.telefon"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.eymen.browser.telefon"
        minSdk = 26
        targetSdk = if (googlePlayBuild) 36 else 35
        versionCode = 1
        versionName = "0.0.1"
        buildConfigField("boolean", "EYMEN_GOOGLE_PLAY", googlePlayBuild.toString())
        testInstrumentationRunner = providers.gradleProperty("eymenTestRunner")
            .getOrElse("com.eymen.browser.telefon.DownloadServiceInstrumentation")
        if (releaseVersion != null) versionName = releaseVersion
        if (releaseCode != null) versionCode = releaseCode
    }
    signingConfigs {
        if (canSign) {
            create("eymenProduction") {
                storeFile = file(signingPath!!)
                storePassword = signingPassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            if (canSign) signingConfig = signingConfigs.getByName("eymenProduction")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}
dependencies {
    implementation(project(":core-tema"))
    implementation(project(":core-motor"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core:1.16.0")
    // Offline QR decode; Apache-2.0, pinned in versions.toml, no hosted scanner SDK.
    implementation("com.google.zxing:core:3.5.4")
    // Official WireGuard userspace GoBackend; Android's VpnService is merged
    // from this library's manifest. No VPN server or keys are embedded.
    implementation("com.wireguard.android:tunnel:1.0.20260102")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    testImplementation("junit:junit:4.13.2")
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
