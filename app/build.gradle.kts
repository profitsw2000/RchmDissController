plugins {
    alias(libs.plugins.rdc.main.app.gradle.plugin)
}

android {
    namespace = "ru.profitsw2000.rchmdisscontroller"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":navigator"))
    implementation(project(":mainScreen"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    //Koin
    implementation(libs.koin)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}