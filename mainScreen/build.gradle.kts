plugins {
    alias(libs.plugins.rdc.main.lib.gradle.plugin)
}

android {
    namespace = "ru.profitsw2000.mainscreen"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":navigator"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}