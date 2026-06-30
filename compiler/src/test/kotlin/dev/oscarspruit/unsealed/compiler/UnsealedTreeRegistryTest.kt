/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class UnsealedTreeRegistryTest {

    private val rootClassId = classId("com.example.core", "SampleRoot")
    private val leafA = classId("com.example.feature1", "LeafA")
    private val leafB = classId("com.example.feature2", "LeafB")

    @Nested
    inner class LoadFromDirectory {

        @Test
        fun `loads leaves from META-INF unsealed directory`(@TempDir tempDir: File) {
            val unsealedDir = File(tempDir, "META-INF/unsealed")
            unsealedDir.mkdirs()
            File(unsealedDir, "com.example.core.SampleRoot").writeText(
                "com.example.feature1.LeafA\ncom.example.feature2.LeafB",
            )
            val registry = UnsealedTreeRegistry(listOf(tempDir))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertEquals(setOf(leafA, leafB), leaves)
        }

        @Test
        fun `returns empty set for unknown root`(@TempDir tempDir: File) {
            val unsealedDir = File(tempDir, "META-INF/unsealed")
            unsealedDir.mkdirs()
            File(unsealedDir, "com.example.core.SampleRoot").writeText(
                "com.example.feature1.LeafA",
            )
            val registry = UnsealedTreeRegistry(listOf(tempDir))
            val unknownRoot = classId("com.example", "Unknown")

            val leaves = registry.getLeavesForRoot(unknownRoot)

            assertTrue(leaves.isEmpty())
        }

        @Test
        fun `skips blank lines in resource file`(@TempDir tempDir: File) {
            val unsealedDir = File(tempDir, "META-INF/unsealed")
            unsealedDir.mkdirs()
            File(unsealedDir, "com.example.core.SampleRoot").writeText(
                "com.example.feature1.LeafA\n\n\ncom.example.feature2.LeafB\n",
            )
            val registry = UnsealedTreeRegistry(listOf(tempDir))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertEquals(setOf(leafA, leafB), leaves)
        }

        @Test
        fun `returns empty set when no META-INF unsealed directory exists`(@TempDir tempDir: File) {
            val registry = UnsealedTreeRegistry(listOf(tempDir))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertTrue(leaves.isEmpty())
        }
    }

    @Nested
    inner class LoadFromJar {

        @Test
        fun `loads leaves from JAR file`(@TempDir tempDir: File) {
            val jarFile = createJar(
                tempDir,
                "test.jar",
                mapOf(
                    "META-INF/unsealed/com.example.core.SampleRoot" to
                        "com.example.feature1.LeafA\ncom.example.feature2.LeafB"
                ),
            )
            val registry = UnsealedTreeRegistry(listOf(jarFile))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertEquals(setOf(leafA, leafB), leaves)
        }

        @Test
        fun `skips non-unsealed entries in JAR`(@TempDir tempDir: File) {
            val jarFile = createJar(
                tempDir,
                "test.jar",
                mapOf(
                    "META-INF/unsealed/com.example.core.SampleRoot" to "com.example.feature1.LeafA",
                    "META-INF/services/some.Service" to "com.example.ServiceImpl",
                ),
            )
            val registry = UnsealedTreeRegistry(listOf(jarFile))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertEquals(setOf(leafA), leaves)
        }

        @Test
        fun `handles corrupt JAR gracefully`(@TempDir tempDir: File) {
            val corruptJar = File(tempDir, "corrupt.jar")
            corruptJar.writeText("not a jar")
            val registry = UnsealedTreeRegistry(listOf(corruptJar))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertTrue(leaves.isEmpty())
        }
    }

    @Nested
    inner class MultipleSources {

        @Test
        fun `merges leaves from directory and JAR`(@TempDir tempDir: File) {
            val dir = File(tempDir, "dir")
            val unsealedDir = File(dir, "META-INF/unsealed")
            unsealedDir.mkdirs()
            File(unsealedDir, "com.example.core.SampleRoot").writeText(
                "com.example.feature1.LeafA",
            )

            val jarFile = createJar(
                tempDir,
                "test.jar",
                mapOf("META-INF/unsealed/com.example.core.SampleRoot" to "com.example.feature2.LeafB"),
            )
            val registry = UnsealedTreeRegistry(listOf(dir, jarFile))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertEquals(setOf(leafA, leafB), leaves)
        }

        @Test
        fun `merges leaves from multiple JARs`(@TempDir tempDir: File) {
            val jar1 = createJar(
                tempDir,
                "jar1.jar",
                mapOf("META-INF/unsealed/com.example.core.SampleRoot" to "com.example.feature1.LeafA"),
            )
            val jar2 = createJar(
                tempDir,
                "jar2.jar",
                mapOf("META-INF/unsealed/com.example.core.SampleRoot" to "com.example.feature2.LeafB"),
            )
            val registry = UnsealedTreeRegistry(listOf(jar1, jar2))

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertEquals(setOf(leafA, leafB), leaves)
        }

        @Test
        fun `returns empty set for empty classpath`() {
            val registry = UnsealedTreeRegistry(emptyList())

            val leaves = registry.getLeavesForRoot(rootClassId)

            assertTrue(leaves.isEmpty())
        }
    }

    private fun createJar(dir: File, name: String, entries: Map<String, String>): File {
        val jarFile = File(dir, name)
        JarOutputStream(jarFile.outputStream()).use { jos ->
            for ((entryName, content) in entries) {
                jos.putNextEntry(JarEntry(entryName))
                jos.write(content.toByteArray())
                jos.closeEntry()
            }
        }
        return jarFile
    }

    private fun classId(pkg: String, name: String): ClassId =
        ClassId(FqName(pkg), Name.identifier(name))
}
