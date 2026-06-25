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
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

public class UnsealedSymbolProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val leaves = resolver.getSymbolsWithAnnotation("dev.oscarspruit.unsealed.runtime.UnsealedLeaf")
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        resolver.getSymbolsWithAnnotation("dev.oscarspruit.unsealed.runtime.UnsealedRoot")
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .forEach { root ->
                val rootLeaves = leaves.filter { leaf ->
                    leaf.superTypes.any { superType ->
                        superType.resolve().declaration.qualifiedName?.asString() == root.qualifiedName?.asString()
                    }
                }
                root.accept(UnsealedRootVisitor(codeGenerator, rootLeaves), Unit)
            }

        return emptyList()
    }

    private class UnsealedRootVisitor(
        private val codeGenerator: CodeGenerator,
        private val leaves: List<KSClassDeclaration>,
    ) : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.toClassName()
            val registryName = "${className.simpleName}Registry"

            val listType = List::class
                .asClassName()
                .parameterizedBy(KClass::class.asClassName().parameterizedBy(STAR))

            val implementations = PropertySpec.builder(
                name = "implementations",
                type = listType,
            )
                .initializer("listOf(\n⇥%L⇤)", leaves.joinToString(",\n") { "${it.toClassName()}::class" })
                .build()

            val registry = TypeSpec.objectBuilder(registryName)
                .addProperty(implementations)
                .build()

            val file = FileSpec.builder(packageName, registryName)
                .addType(registry)
                .build()

            file.writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(false, classDeclaration.containingFile!!),
            )
        }
    }
}
