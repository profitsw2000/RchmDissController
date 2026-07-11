plugins {
    alias(libs.plugins.rdc.main.lib.gradle.plugin)
}

android {
    namespace = "ru.profitsw2000.mainscreen"

    packaging {
        resources {
            // Исключаем дубликаты лицензий из сборки
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/LICENSE-notice.md")

            // На всякий случай дублируем в pickFirsts
            pickFirsts.add("META-INF/LICENSE.md")
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":navigator"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    //Koin
    implementation(libs.koin)
    //Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    //Instrumentation test
    androidTestImplementation(libs.androidx.fragment.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.io.mockk)
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.androidx.junit)
}