plugins { id("com.android.application") }

fun auditPin(name: String) = requireNotNull(System.getenv("EY_AUDIT_$name")).toInt()

// Independent UI driver only. The production browser is installed unmodified
// from Releases; no production key, test hook or debuggable browser is needed.
android {
    namespace = "com.eymen.browser.updateaudit"
    compileSdk = auditPin("COMPILE_SDK")
    defaultConfig {
        applicationId = "com.eymen.browser.updateaudit"
        minSdk = auditPin("MIN_SDK")
        targetSdk = auditPin("TARGET_SDK")
        versionCode = 1
        versionName = "0.0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(auditPin("JAVA"))
        targetCompatibility = JavaVersion.toVersion(auditPin("JAVA"))
    }
}
