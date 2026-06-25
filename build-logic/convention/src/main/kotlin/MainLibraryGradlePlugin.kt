import com.android.build.api.dsl.LibraryExtension
import gradleplugins.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class MainLibraryGradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(project.pluginManager) {
                apply("com.android.library")
                apply("com.google.devtools.ksp")
                apply("org.jetbrains.kotlin.plugin.parcelize")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                buildFeatures.buildConfig = true
                defaultConfig { consumerProguardFile("consumer-rules.pro")}
            }
        }
    }
}