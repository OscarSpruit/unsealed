/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate

class UnsealedSymbolProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("dev.oscarspruit.unsealed.runtime.UnsealedRoot")
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .forEach { it.accept(UnsealedRootVisitor(codeGenerator), Unit) }

        return emptyList()
    }

    private class UnsealedRootVisitor(
        private val codeGenerator: CodeGenerator,
    ) : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()
            val registryName = "${className}Registry"

            val file = codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false, classDeclaration.containingFile!!),
                packageName = packageName,
                fileName = registryName,
            )

            file.bufferedWriter().use { writer ->
                writer.write("package $packageName\n\n")
                writer.write("object $registryName {\n")
                writer.write("    // TODO: populate with leaf types\n")
                writer.write("}\n")
            }
        }
    }
}
