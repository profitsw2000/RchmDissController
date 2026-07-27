import com.android.build.api.dsl.ApplicationExtension
import gradleplugins.configureKotlinAndroid
import gradleplugins.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class MainApplicationGradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(project.pluginManager) {
                apply("com.android.application")
                apply("com.google.devtools.ksp")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureDefaultConfig(project)
            }
        }
    }

    private fun ApplicationExtension.configureDefaultConfig(project: Project) {
        with(project) {
            defaultConfig {
                applicationId = libs.findVersion("applicationId").get().requiredVersion
                targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                versionCode = libs.findVersion("versionCode").get().requiredVersion.toInt()
                versionName = libs.findVersion("versionName").get().requiredVersion
            }
        }
    }
}