plugins {
    id("com.android.library")
}

val versionsText = rootDir.resolve("../versions.toml").readText()
val geckoVersion = Regex("""(?m)^geckoview\s*=\s*"([^"]+)"""")
    .find(versionsText)?.groupValues?.get(1)
    ?: error("versions.toml: GeckoView surumu eksik")

android {
    namespace = "com.eymen.browser.motor"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    api("org.mozilla.geckoview:geckoview:$geckoVersion")
}
