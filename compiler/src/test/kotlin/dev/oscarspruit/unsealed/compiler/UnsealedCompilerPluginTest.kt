/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
class UnsealedCompilerPluginTest {

    @Nested
    inner class AnnotationValidation {

        @Test
        fun `@UnsealedRoot on interface compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    interface Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `@UnsealedRoot on abstract class compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    abstract class Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `@UnsealedRoot on open class compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    open class Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `@UnsealedRoot on final class fails`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    class Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("@UnsealedRoot"))
        }

        @Test
        fun `@UnsealedRoot on object fails`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    object Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("@UnsealedRoot"))
        }

        @Test
        fun `@UnsealedRoot on enum class fails`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    enum class Root { A, B }
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("@UnsealedRoot"))
        }

        @Test
        fun `@UnsealedRoot on annotation class fails`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    annotation class Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("@UnsealedRoot"))
        }

        @Test
        fun `@UnsealedLeaf extending @UnsealedRoot compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Root.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface Root

                    @UnsealedLeaf
                    class Leaf : Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `both annotations on class extending root compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface Root

                    @UnsealedRoot
                    @UnsealedLeaf
                    interface MiddleNode : Root
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `@UnsealedLeaf without @UnsealedRoot supertype fails`() {
            val result = compile(
                SourceFile.kotlin(
                    "Leaf.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    interface NotARoot

                    @UnsealedLeaf
                    class Leaf : NotARoot
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("@UnsealedLeaf"))
        }

        @Test
        fun `@UnsealedLeaf with no supertypes fails`() {
            val result = compile(
                SourceFile.kotlin(
                    "Leaf.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedLeaf
                    class Leaf
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("@UnsealedLeaf"))
        }
    }

    @Nested
    inner class WhenExhaustiveness {

        @Test
        fun `exhaustive when compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
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
                            is LeafB -> {}
                        }
                    }
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `non-exhaustive when fails`() {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
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
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assertTrue(result.messages.contains("LeafB"))
        }

        @Test
        fun `when with else branch compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
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
                            else -> {}
                        }
                    }
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `when on non-UnsealedRoot type is not checked`() {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    interface Root

                    class LeafA : Root

                    fun test(root: Root) {
                        when (root) {
                            is LeafA -> {}
                        }
                    }
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `when without subject is not checked`() {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface Root

                    @UnsealedLeaf
                    class LeafA : Root

                    @UnsealedLeaf
                    class LeafB : Root

                    fun test(root: Root) {
                        when {
                            root is LeafA -> {}
                        }
                    }
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        @Test
        fun `single leaf exhaustive when compiles successfully`() {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface Root

                    @UnsealedLeaf
                    class OnlyLeaf : Root

                    fun test(root: Root) {
                        when (root) {
                            is OnlyLeaf -> {}
                        }
                    }
                    """,
                ),
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }
    }

    @Nested
    inner class ResourceGeneration {

        @Test
        fun `generates META-INF unsealed resource for leaf`(@TempDir tempDir: File) {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface Root

                    @UnsealedLeaf
                    class LeafA : Root
                    """,
                ),
                resourceOutputDir = tempDir,
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val unsealedDir = File(tempDir, "META-INF/unsealed")
            assertTrue(unsealedDir.isDirectory, "META-INF/unsealed directory should exist")

            val resourceFiles = unsealedDir.listFiles()!!
            assertEquals(1, resourceFiles.size)

            val content = resourceFiles.first().readText()
            assertTrue(content.contains("LeafA"))
        }

        @Test
        fun `generates multiple leaves in resource file`(@TempDir tempDir: File) {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface Root

                    @UnsealedLeaf
                    class LeafA : Root

                    @UnsealedLeaf
                    class LeafB : Root
                    """,
                ),
                resourceOutputDir = tempDir,
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val unsealedDir = File(tempDir, "META-INF/unsealed")
            val content = unsealedDir.listFiles()!!.first().readText()
            val leaves = content.lines().filter { it.isNotBlank() }

            assertEquals(2, leaves.size)
            assertTrue(leaves.any { it.contains("LeafA") })
            assertTrue(leaves.any { it.contains("LeafB") })
        }

        @Test
        fun `does not generate resources when no leaves exist`(@TempDir tempDir: File) {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot

                    @UnsealedRoot
                    interface Root
                    """,
                ),
                resourceOutputDir = tempDir,
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val unsealedDir = File(tempDir, "META-INF/unsealed")
            val files = unsealedDir.listFiles()

            assertTrue(files == null || files.isEmpty())
        }

        @Test
        fun `generates separate resource files for multiple roots`(@TempDir tempDir: File) {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    package com.example

                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface RootA

                    @UnsealedRoot
                    interface RootB

                    @UnsealedLeaf
                    class LeafA : RootA

                    @UnsealedLeaf
                    class LeafB : RootB
                    """,
                ),
                resourceOutputDir = tempDir,
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val unsealedDir = File(tempDir, "META-INF/unsealed")
            val resourceFiles = unsealedDir.listFiles()!!

            assertEquals(2, resourceFiles.size)
        }

        @Test
        fun `resource file name matches root FQN`(@TempDir tempDir: File) {
            val result = compile(
                SourceFile.kotlin(
                    "Test.kt",
                    """
                    package com.example

                    import dev.oscarspruit.unsealed.runtime.UnsealedRoot
                    import dev.oscarspruit.unsealed.runtime.UnsealedLeaf

                    @UnsealedRoot
                    interface MyRoot

                    @UnsealedLeaf
                    class MyLeaf : MyRoot
                    """,
                ),
                resourceOutputDir = tempDir,
            )

            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

            val unsealedDir = File(tempDir, "META-INF/unsealed")
            val resourceFiles = unsealedDir.listFiles()!!

            assertEquals(1, resourceFiles.size)
            assertEquals("com.example.MyRoot", resourceFiles.first().name)

            val content = resourceFiles.first().readText()
            assertTrue(content.contains("com.example.MyLeaf"))
        }
    }

    private fun compile(
        vararg sources: SourceFile,
        resourceOutputDir: File? = null,
    ): CompilationResult {
        return KotlinCompilation().apply {
            this.sources = sources.toList()
            compilerPluginRegistrars = listOf(UnsealedCompilerPluginRegistrar())
            commandLineProcessors = listOf(UnsealedCommandLineProcessor())
            inheritClassPath = true
            messageOutputStream = System.out

            if (resourceOutputDir != null) {
                pluginOptions = listOf(
                    com.tschuchort.compiletesting.PluginOption(
                        pluginId = Constants.PLUGIN_ID,
                        optionName = Constants.RESOURCE_OUTPUT_DIR,
                        optionValue = resourceOutputDir.absolutePath,
                    ),
                )
            }
        }.compile()
    }
}
