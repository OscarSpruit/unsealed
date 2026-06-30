/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.name.ClassId
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

public class UnsealedTreeRegistry(
    private val classpathEntries: List<File>,
) {

    private val trees: Map<ClassId, Set<ClassId>> by lazy { loadFromClasspath() }

    public fun getLeavesForRoot(rootClassId: ClassId): Set<ClassId> {
        return trees[rootClassId] ?: emptySet()
    }

    private fun loadFromClasspath(): Map<ClassId, Set<ClassId>> {
        val result = mutableMapOf<ClassId, MutableSet<ClassId>>()
        for (entry in classpathEntries) {
            if (entry.isFile && entry.extension == "jar") {
                loadFromJar(entry, result)
            } else if (entry.isDirectory) {
                loadFromDirectory(entry, result)
            }
        }
        return result
    }

    private fun loadFromJar(jar: File, result: MutableMap<ClassId, MutableSet<ClassId>>) {
        try {
            JarFile(jar).use { jarFile ->
                jarFile.entries().asSequence()
                    .filter { it.name.startsWith("META-INF/unsealed/") && !it.isDirectory }
                    .forEach { entry ->
                        val rootFqn = entry.name.removePrefix("META-INF/unsealed/")
                        val rootClassId = UnsealedClassIds.classIdFromFqn(rootFqn) ?: return@forEach
                        val leaves = jarFile.getInputStream(entry).bufferedReader()
                            .readLines()
                            .filter { it.isNotBlank() }
                            .mapNotNull { UnsealedClassIds.classIdFromFqn(it) }
                        result.getOrPut(rootClassId) { mutableSetOf() }.addAll(leaves)
                    }
            }
        } catch (_: IOException) {
            // Skip JARs that can't be read (e.g. corrupted, inaccessible)
        }
    }

    private fun loadFromDirectory(dir: File, result: MutableMap<ClassId, MutableSet<ClassId>>) {
        val unsealedDir = File(dir, "META-INF/unsealed")
        if (!unsealedDir.isDirectory) return
        unsealedDir.listFiles()?.forEach { file ->
            val rootClassId = UnsealedClassIds.classIdFromFqn(file.name) ?: return@forEach
            val leaves = file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { UnsealedClassIds.classIdFromFqn(it) }
            result.getOrPut(rootClassId) { mutableSetOf() }.addAll(leaves)
        }
    }
}
