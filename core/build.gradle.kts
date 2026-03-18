plugins {
    alias(libs.plugins.rdc.main.lib.gradle.plugin)
}

android {
    namespace = "ru.profitsw2000.core"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}