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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.oscarspruit.unsealed.runtime.UnsealedLeaf
import dev.oscarspruit.unsealed.runtime.UnsealedLeafProvider
import dev.oscarspruit.unsealed.runtime.UnsealedRoot
import kotlin.reflect.KClass

internal class UnsealedSymbolProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(UnsealedLeaf::class.qualifiedName!!)
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .forEach {
                it.accept(UnsealedLeafVisitor(codeGenerator), Unit)
            }

        return emptyList()
    }

    private class UnsealedLeafVisitor(
        private val codeGenerator: CodeGenerator,
    ) : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.toClassName()
            val providerName = "${className.simpleName}_UnsealedProvider"

            writeProviderFile(
                classDeclaration = classDeclaration,
                className = className,
                providerName = providerName,
                packageName = packageName,
            )

            writeResourceFile(
                classDeclaration = classDeclaration,
                providerQualifiedName = "$packageName.$providerName",
            )
        }

        private fun writeProviderFile(
            classDeclaration: KSClassDeclaration,
            className: ClassName,
            providerName: String,
            packageName: String,
        ) {
            val superType = resolveSuperType(classDeclaration)
                ?: error("No super type annotated with @UnsealedRoot found on ${className.canonicalName}")

            val root = PropertySpec.builder(
                name = "root",
                type = KClass::class.asClassName().parameterizedBy(STAR),
                KModifier.OVERRIDE,
            )
                .initializer("%T::class", superType)
                .build()

            val leaves = PropertySpec.builder(
                name = "leaves",
                type = List::class.asClassName().parameterizedBy(KClass::class.asClassName().parameterizedBy(STAR)),
                KModifier.OVERRIDE,
            )
                .initializer("listOf(%T::class)", className)
                .build()

            val provider = TypeSpec.objectBuilder(providerName)
                .addSuperinterface(UnsealedLeafProvider::class.asClassName())
                .addProperty(root)
                .addProperty(leaves)
                .build()

            val file = FileSpec.builder(packageName, providerName)
                .addType(provider)
                .build()

            file.writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(false, classDeclaration.containingFile!!),
            )
        }

        private fun resolveSuperType(classDeclaration: KSClassDeclaration): ClassName? {
            return classDeclaration.superTypes.find { typeRef ->
                typeRef.resolve().declaration.annotations.any { annotation ->
                    annotation.annotationType.resolve().declaration.qualifiedName?.asString() ==
                        UnsealedRoot::class.qualifiedName
                }
            }?.resolve()?.toClassName()
        }

        private fun writeResourceFile(
            classDeclaration: KSClassDeclaration,
            providerQualifiedName: String,
        ) {
            val resourceFile = codeGenerator.createNewFile(
                Dependencies(false, classDeclaration.containingFile!!),
                packageName = "",
                fileName = "META-INF/services/${UnsealedLeafProvider::class.qualifiedName}",
                extensionName = "",
            )

            resourceFile.bufferedWriter().use { writer ->
                writer.write(providerQualifiedName)
                writer.newLine()
            }
        }
    }
}
