/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.gradle.plugin

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

// TODO - Move constants to toml file and generate constants with build config
public class UnsealedGradlePlugin : KotlinCompilerPluginSupportPlugin {

    // TODO - Move constants to toml file and generate constants with build config
    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "dev.oscarspruit.unsealed",
        artifactId = "compiler",
        version = "0.0.1-SNAPSHOT",
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val outputDir = project.layout.buildDirectory.dir("generated/unsealed/resources")

        val processResourcesTaskName = if (kotlinCompilation.name == "main") {
            "processResources"
        } else {
            "process${kotlinCompilation.name.replaceFirstChar { it.uppercase() }}Resources"
        }
        project.tasks.named(processResourcesTaskName).configure {
            it.dependsOn(kotlinCompilation.compileKotlinTaskName)
        }

        kotlinCompilation.defaultSourceSet.resources.srcDir(outputDir)

        return project.provider {
            listOf(SubpluginOption("resourceOutputDir", outputDir.get().asFile.absolutePath))
        }
    }

    private companion object {
        const val PLUGIN_ID = "dev.oscarspruit.unsealed.compiler"
    }
}
