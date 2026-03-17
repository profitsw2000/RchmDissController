package gradleplugins

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension
) {
    commonExtension.apply {
        compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

        defaultConfig.apply {
            minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
            testInstrumentationRunner = libs.findVersion("testInstrumentationRunner").get().requiredVersion
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        buildFeatures.viewBinding = true
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}