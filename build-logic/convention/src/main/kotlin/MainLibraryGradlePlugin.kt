import com.android.build.gradle.LibraryExtension
import gradleplugins.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class MainLibraryGradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(project.pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.kapt")
                apply("kotlin-parcelize")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig { consumerProguardFile("consumer-rules.pro")}
            }
        }
    }
}