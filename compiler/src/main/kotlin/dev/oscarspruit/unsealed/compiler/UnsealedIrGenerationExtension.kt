/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File

internal class UnsealedIrGenerationExtension(
    private val resourceOutputDir: String,
) : IrGenerationExtension {

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val trees = findTrees(moduleFragment.files)
        writeResources(trees)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun findTrees(files: List<IrFile>): Map<String, List<String>> {
        val unsealedLeafId = ClassId(FqName("dev.oscarspruit.unsealed.runtime"), Name.identifier("UnsealedLeaf"))
        val unsealedRootId = ClassId(FqName("dev.oscarspruit.unsealed.runtime"), Name.identifier("UnsealedRoot"))

        val rootToLeaves = mutableMapOf<String, MutableList<String>>()

        for (file in files) {
            for (declaration in file.declarations) {
                if (declaration !is IrClass) continue
                if (!declaration.hasAnnotation(unsealedLeafId)) continue

                val rootFqn = declaration.superTypes
                    .mapNotNull { it.classOrNull?.owner }
                    .firstOrNull { it.hasAnnotation(unsealedRootId) }
                    ?.classId
                    ?.asFqNameString() ?: continue

                val leafFqn = declaration.classId?.asFqNameString() ?: continue

                rootToLeaves.getOrPut(rootFqn) { mutableListOf() }.add(leafFqn)
            }
        }

        return rootToLeaves
    }

    private fun writeResources(trees: Map<String, List<String>>) {
        val outputDir = File(resourceOutputDir)
        for ((rootFqn, leaves) in trees) {
            val file = File(outputDir, "META-INF/unsealed/$rootFqn")
            file.parentFile.mkdirs()
            file.writeText(
                leaves.joinToString("\n"),
            )
        }
    }
}
