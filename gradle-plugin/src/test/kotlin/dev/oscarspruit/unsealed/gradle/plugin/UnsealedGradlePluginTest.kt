/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarFile

class UnsealedGradlePluginTest {

    private val rootProjectDir = System.getProperty("rootProjectDir")!!

    @Nested
    inner class PluginApplication {

        @Test
        fun `plugin applies without errors`(@TempDir tempDir: File) {
            writeSettingsFile(tempDir)
            writeBuildFile(tempDir)

            val result = createRunner(tempDir)
                .withArguments("tasks", "--all")
                .build()

            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        }

        @Test
        fun `processResources depends on compileKotlin`(@TempDir tempDir: File) {
            writeSettingsFile(tempDir)
            writeBuildFile(tempDir, withRuntimeDependency = true)
            writeSourceFile(
                tempDir,
                "Root.kt",
                """
                package com.example
                import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                @UnsealedRoot
                interface Root
                """,
            )

            val result = createRunner(tempDir)
                .withArguments("processResources", "--dry-run")
                .build()

            val taskOrder = result.output.lines()
                .filter { it.startsWith(":") }
                .map { it.substringBefore(" ") }
            val compileIndex = taskOrder.indexOf(":compileKotlin")
            val processIndex = taskOrder.indexOf(":processResources")

            assertTrue(compileIndex >= 0, "compileKotlin should be in task list")
            assertTrue(processIndex >= 0, "processResources should be in task list")
            assertTrue(compileIndex < processIndex, "compileKotlin should run before processResources")
        }
    }

    @Nested
    inner class Compilation {

        @Test
        fun `compiles source with annotations`(@TempDir tempDir: File) {
            writeSettingsFile(tempDir)
            writeBuildFile(tempDir, withRuntimeDependency = true)
            writeSourceFile(
                tempDir,
                "Root.kt",
                """
                package com.example
                import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                @UnsealedRoot
                interface Root
                """,
            )
            writeSourceFile(
                tempDir,
                "Leaf.kt",
                """
                package com.example
                import dev.oscarspruit.unsealed.runtime.UnsealedLeaf
                @UnsealedLeaf
                class Leaf : Root
                """,
            )

            val result = createRunner(tempDir)
                .withArguments("build")
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
        }

        @Test
        fun `compilation fails for non-exhaustive when`(@TempDir tempDir: File) {
            writeSettingsFile(tempDir)
            writeBuildFile(tempDir, withRuntimeDependency = true)
            writeSourceFile(
                tempDir,
                "Root.kt",
                """
                package com.example
                import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                @UnsealedRoot
                interface Root

                @UnsealedLeaf
                class LeafA : Root

                @UnsealedLeaf
                class LeafB : Root

                fun test(root: Root) {
                    when (root) {
                        is LeafA -> {}
                    }
                }
                """,
            )

            val result = createRunner(tempDir)
                .withArguments("build")
                .buildAndFail()

            assertTrue(result.output.contains("LeafB"))
        }
    }

    @Nested
    inner class ResourceGeneration {

        @Test
        fun `generates unsealed resource files`(@TempDir tempDir: File) {
            writeSettingsFile(tempDir)
            writeBuildFile(tempDir, withRuntimeDependency = true)
            writeSourceFile(
                tempDir,
                "Root.kt",
                """
                package com.example
                import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                @UnsealedRoot
                interface Root

                @UnsealedLeaf
                class LeafA : Root

                @UnsealedLeaf
                class LeafB : Root
                """,
            )

            createRunner(tempDir)
                .withArguments("build")
                .build()

            val resourceDir = File(tempDir, "build/generated/unsealed/resources/META-INF/unsealed")

            assertTrue(resourceDir.isDirectory, "META-INF/unsealed directory should exist")

            val files = resourceDir.listFiles()!!
            assertEquals(1, files.size)
            assertEquals("com.example.Root", files.first().name)

            val leaves = files.first().readText().lines().filter { it.isNotBlank() }
            assertEquals(2, leaves.size)
            assertTrue(leaves.any { it.contains("LeafA") })
            assertTrue(leaves.any { it.contains("LeafB") })
        }

        @Test
        fun `resources are included in JAR`(@TempDir tempDir: File) {
            writeSettingsFile(tempDir)
            writeBuildFile(tempDir, withRuntimeDependency = true)
            writeSourceFile(
                tempDir,
                "Root.kt",
                """
                package com.example
                import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                @UnsealedRoot
                interface Root

                @UnsealedLeaf
                class Leaf : Root
                """,
            )

            createRunner(tempDir)
                .withArguments("jar")
                .build()

            val jarFile = File(tempDir, "build/libs").listFiles()!!
                .first { it.extension == "jar" }

            JarFile(jarFile).use { jar ->
                val entry = jar.getEntry("META-INF/unsealed/com.example.Root")

                assertTrue(entry != null, "JAR should contain META-INF/unsealed/com.example.Root")

                val content = jar.getInputStream(entry).bufferedReader().readText()
                assertTrue(content.contains("com.example.Leaf"))
            }
        }
    }

    private fun createRunner(projectDir: File): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()

    private fun writeSettingsFile(projectDir: File) {
        val escapedPath = rootProjectDir.replace("\\", "/")
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                includeBuild("$escapedPath/gradle-plugin")
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            includeBuild("$escapedPath")
            """.trimIndent(),
        )
    }

    private fun writeBuildFile(projectDir: File, withRuntimeDependency: Boolean = false) {
        val deps = if (withRuntimeDependency) {
            """

            dependencies {
                implementation("dev.oscarspruit.unsealed:runtime:0.0.1-SNAPSHOT")
            }
            """.trimIndent()
        } else {
            ""
        }

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.4.0"
                id("dev.oscarspruit.unsealed")
            }
            $deps
            """.trimIndent(),
        )
    }

    private fun writeSourceFile(projectDir: File, name: String, content: String) {
        val srcDir = File(projectDir, "src/main/kotlin")
        srcDir.mkdirs()
        File(srcDir, name).writeText(content.trimIndent())
    }
}
