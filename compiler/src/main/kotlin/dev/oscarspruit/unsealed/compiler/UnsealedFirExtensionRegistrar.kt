/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.compiler

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import java.io.File

internal class UnsealedFirExtensionRegistrar(
    private val classpathEntries: List<File>,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +::UnsealedFirAdditionalCheckersExtension
        +::UnsealedPredicateRegistrar
        +UnsealedTreeRegistry.factory(classpathEntries)
    }
}
