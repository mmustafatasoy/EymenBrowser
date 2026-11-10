pluginManagement {
    val pinnedAgp = requireNotNull(System.getenv("EY_AUDIT_AGP")) {
        "Run tools/prepare_signed_update_audit.py to load versions.toml pins"
    }
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    plugins { id("com.android.application") version pinnedAgp }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "signed-update-audit"
