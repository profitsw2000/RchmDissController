plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("mainApplicationGradlePlugin") {
            id = libs.plugins.rdc.main.app.gradle.plugin.get().pluginId
            implementationClass = "MainApplicationGradlePlugin"
        }
        register("mainLibraryGradlePlugin") {
            id = libs.plugins.rdc.main.lib.gradle.plugin.get().pluginId
            implementationClass = "MainLibraryGradlePlugin"
        }
    }
}